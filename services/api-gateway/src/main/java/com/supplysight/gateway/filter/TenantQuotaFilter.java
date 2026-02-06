package com.supplysight.gateway.filter;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Global filter that enforces tenant-level quotas.
 * Checks against Redis-stored quota limits and current usage.
 */
@Component
public class TenantQuotaFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(TenantQuotaFilter.class);

    private static final String QUOTA_KEY_PREFIX = "tenant:quota:";
    private static final String USAGE_KEY_PREFIX = "tenant:usage:";
    private static final int DEFAULT_MAX_EVENTS_PER_SECOND = 100;
    private static final int DEFAULT_RETRY_AFTER_SECONDS = 5;

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final MeterRegistry meterRegistry;
    private final ConcurrentHashMap<String, Counter> throttledCounters = new ConcurrentHashMap<>();

    public TenantQuotaFilter(
            ReactiveRedisTemplate<String, String> redisTemplate,
            MeterRegistry meterRegistry) {
        this.redisTemplate = redisTemplate;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String tenantId = exchange.getAttribute("tenantId");

        // Skip quota check if no tenant ID (public endpoints)
        if (tenantId == null) {
            return chain.filter(exchange);
        }

        // Skip quota check for actuator endpoints
        String path = exchange.getRequest().getURI().getPath();
        if (path.contains("/actuator/") || path.contains("/health")) {
            return chain.filter(exchange);
        }

        return checkQuota(tenantId)
                .flatMap(allowed -> {
                    if (allowed) {
                        return incrementUsage(tenantId)
                                .then(chain.filter(exchange));
                    } else {
                        return rejectRequest(exchange, tenantId);
                    }
                })
                .onErrorResume(e -> {
                    log.warn("Error checking quota for tenant {}: {}", tenantId, e.getMessage());
                    // On error, allow request through (fail-open)
                    return chain.filter(exchange);
                });
    }

    private Mono<Boolean> checkQuota(String tenantId) {
        String maxEpsKey = QUOTA_KEY_PREFIX + tenantId + ":max_events_per_second";
        String currentEpsKey = USAGE_KEY_PREFIX + tenantId + ":eps:" + getCurrentSecond();

        return Mono.zip(
                getQuotaValue(maxEpsKey, DEFAULT_MAX_EVENTS_PER_SECOND),
                getCurrentUsage(currentEpsKey)).map(tuple -> {
                    int maxEps = tuple.getT1();
                    int currentEps = tuple.getT2();

                    boolean allowed = currentEps < maxEps;
                    if (!allowed) {
                        log.warn("Tenant {} quota exceeded: {}/{} events/sec", tenantId, currentEps, maxEps);
                    }
                    return allowed;
                });
    }

    private Mono<Integer> getQuotaValue(String key, int defaultValue) {
        return redisTemplate.opsForValue().get(key)
                .map(Integer::parseInt)
                .defaultIfEmpty(defaultValue);
    }

    private Mono<Integer> getCurrentUsage(String key) {
        return redisTemplate.opsForValue().get(key)
                .map(Integer::parseInt)
                .defaultIfEmpty(0);
    }

    private Mono<Void> incrementUsage(String tenantId) {
        String epsKey = USAGE_KEY_PREFIX + tenantId + ":eps:" + getCurrentSecond();
        String dailyKey = USAGE_KEY_PREFIX + tenantId + ":events_today:" + java.time.LocalDate.now();

        return redisTemplate.opsForValue().increment(epsKey)
                .flatMap(v -> redisTemplate.expire(epsKey, Duration.ofSeconds(10)))
                .then(redisTemplate.opsForValue().increment(dailyKey))
                .flatMap(v -> redisTemplate.expire(dailyKey, Duration.ofDays(2)))
                .then();
    }

    private Mono<Void> rejectRequest(ServerWebExchange exchange, String tenantId) {
        // Increment throttled counter
        getOrCreateThrottledCounter(tenantId).increment();

        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().add(HttpHeaders.RETRY_AFTER, String.valueOf(DEFAULT_RETRY_AFTER_SECONDS));
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String body = String.format("""
                {
                    "success": false,
                    "error": {
                        "code": "QUOTA_EXCEEDED",
                        "message": "Request rate limit exceeded. Please retry after %d seconds."
                    },
                    "timestamp": "%s"
                }
                """, DEFAULT_RETRY_AFTER_SECONDS, Instant.now().toString());

        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
    }

    private Counter getOrCreateThrottledCounter(String tenantId) {
        return throttledCounters.computeIfAbsent(tenantId, id -> Counter.builder("tenant_throttled_requests_total")
                .tag("tenant_id", tenantId)
                .description("Total number of requests throttled due to quota exceeded")
                .register(meterRegistry));
    }

    private long getCurrentSecond() {
        return System.currentTimeMillis() / 1000;
    }

    @Override
    public int getOrder() {
        // Run after JWT authentication filter but before routing
        return 10;
    }
}
