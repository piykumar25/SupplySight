package com.supplysight.ingestion.interceptor;

import com.supplysight.common.security.TenantContext;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Interceptor for enforcing tenant quota limits on event ingestion.
 */
@Component
public class QuotaEnforcementInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(QuotaEnforcementInterceptor.class);

    private static final String QUOTA_KEY_PREFIX = "tenant:quota:";
    private static final String USAGE_KEY_PREFIX = "tenant:usage:";
    private static final int DEFAULT_MAX_EVENTS_PER_SECOND = 100;
    private static final int DEFAULT_RETRY_AFTER_SECONDS = 5;

    private final RedisTemplate<String, String> redisTemplate;
    private final MeterRegistry meterRegistry;
    private final ConcurrentHashMap<String, Counter> throttledCounters = new ConcurrentHashMap<>();

    public QuotaEnforcementInterceptor(
            RedisTemplate<String, String> redisTemplate,
            MeterRegistry meterRegistry) {
        this.redisTemplate = redisTemplate;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        UUID tenantId = TenantContext.getTenantId();

        if (tenantId == null) {
            // No tenant context, allow request (likely unauthenticated)
            return true;
        }

        String tenantIdStr = tenantId.toString();

        // Check rate limit
        if (!checkRateLimit(tenantIdStr)) {
            log.warn("Rate limit exceeded for tenant: {}", tenantId);

            // Increment throttled counter
            getOrCreateThrottledCounter(tenantIdStr).increment();

            // Return 429 Too Many Requests
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", String.valueOf(DEFAULT_RETRY_AFTER_SECONDS));
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);

            String errorBody = String.format("""
                    {
                        "success": false,
                        "error": {
                            "code": "QUOTA_EXCEEDED",
                            "message": "Event ingestion rate limit exceeded. Please retry after %d seconds."
                        },
                        "timestamp": "%s"
                    }
                    """, DEFAULT_RETRY_AFTER_SECONDS, Instant.now().toString());

            response.getWriter().write(errorBody);
            return false;
        }

        // Record event for rate tracking
        recordEventIngested(tenantIdStr);

        return true;
    }

    private boolean checkRateLimit(String tenantId) {
        try {
            int maxEps = getQuotaLimit(tenantId, "max_events_per_second", DEFAULT_MAX_EVENTS_PER_SECOND);
            int currentEps = getCurrentEventsPerSecond(tenantId);

            return currentEps < maxEps;
        } catch (Exception e) {
            log.warn("Error checking rate limit for tenant {}: {}", tenantId, e.getMessage());
            // Fail-open on errors
            return true;
        }
    }

    private int getQuotaLimit(String tenantId, String limitType, int defaultValue) {
        String key = QUOTA_KEY_PREFIX + tenantId + ":" + limitType;
        String value = redisTemplate.opsForValue().get(key);
        return value != null ? Integer.parseInt(value) : defaultValue;
    }

    private int getCurrentEventsPerSecond(String tenantId) {
        long currentSecond = System.currentTimeMillis() / 1000;
        int total = 0;

        // Sum last 5 seconds for average EPS
        for (int i = 0; i < 5; i++) {
            String key = USAGE_KEY_PREFIX + tenantId + ":eps:" + (currentSecond - i);
            String value = redisTemplate.opsForValue().get(key);
            if (value != null) {
                total += Integer.parseInt(value);
            }
        }

        return total / 5;
    }

    private void recordEventIngested(String tenantId) {
        try {
            long currentSecond = System.currentTimeMillis() / 1000;
            String epsKey = USAGE_KEY_PREFIX + tenantId + ":eps:" + currentSecond;
            String dailyKey = USAGE_KEY_PREFIX + tenantId + ":events_today:" + LocalDate.now();

            redisTemplate.opsForValue().increment(epsKey);
            redisTemplate.expire(epsKey, Duration.ofSeconds(10));

            redisTemplate.opsForValue().increment(dailyKey);
            redisTemplate.expire(dailyKey, Duration.ofDays(2));
        } catch (Exception e) {
            log.warn("Error recording event for tenant {}: {}", tenantId, e.getMessage());
        }
    }

    private Counter getOrCreateThrottledCounter(String tenantId) {
        return throttledCounters.computeIfAbsent(tenantId, id -> Counter.builder("event_ingestion_throttled_total")
                .tag("tenant_id", tenantId)
                .description("Total number of event ingestion requests throttled")
                .register(meterRegistry));
    }
}
