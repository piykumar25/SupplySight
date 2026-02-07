package com.supplysight.prediction.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/** Service for managing SSE connection limits per tenant. */
@Service
public class SseConnectionLimitService {

    private static final Logger log = LoggerFactory.getLogger(SseConnectionLimitService.class);

    private static final String QUOTA_KEY_PREFIX = "tenant:quota:";
    private static final String USAGE_KEY_PREFIX = "tenant:usage:";
    private static final int DEFAULT_MAX_SSE_CONNECTIONS = 50;

    private final RedisTemplate<String, String> redisTemplate;
    private final MeterRegistry meterRegistry;

    // Local connection counters for fast access
    private final ConcurrentHashMap<UUID, AtomicInteger> localConnectionCounts =
            new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Counter> rejectedCounters = new ConcurrentHashMap<>();

    public SseConnectionLimitService(
            RedisTemplate<String, String> redisTemplate, MeterRegistry meterRegistry) {
        this.redisTemplate = redisTemplate;
        this.meterRegistry = meterRegistry;
    }

    /**
     * Check if tenant can open a new SSE connection.
     *
     * @return true if connection is allowed, false if limit exceeded
     */
    public boolean canOpenConnection(UUID tenantId) {
        int maxConnections = getMaxConnections(tenantId);
        int currentConnections = getCurrentConnections(tenantId);

        if (currentConnections >= maxConnections) {
            log.warn(
                    "SSE connection limit exceeded for tenant {}: {}/{}",
                    tenantId,
                    currentConnections,
                    maxConnections);
            getOrCreateRejectedCounter(tenantId).increment();
            return false;
        }

        return true;
    }

    /** Track a new SSE connection. */
    public void trackConnectionOpened(UUID tenantId) {
        // Increment local counter
        localConnectionCounts
                .computeIfAbsent(
                        tenantId,
                        k -> {
                            AtomicInteger counter = new AtomicInteger(0);
                            // Register gauge for this tenant
                            Gauge.builder("sse_active_connections", counter, AtomicInteger::get)
                                    .tag("tenant_id", tenantId.toString())
                                    .description("Active SSE connections per tenant")
                                    .register(meterRegistry);
                            return counter;
                        })
                .incrementAndGet();

        // Also update Redis for distributed tracking
        try {
            String key = USAGE_KEY_PREFIX + tenantId + ":sse_connections";
            redisTemplate.opsForValue().increment(key);
        } catch (Exception e) {
            log.warn(
                    "Failed to update Redis SSE counter for tenant {}: {}",
                    tenantId,
                    e.getMessage());
        }

        log.debug(
                "SSE connection opened for tenant {}, current count: {}",
                tenantId,
                localConnectionCounts.get(tenantId).get());
    }

    /** Track an SSE connection closed. */
    public void trackConnectionClosed(UUID tenantId) {
        // Decrement local counter
        AtomicInteger localCounter = localConnectionCounts.get(tenantId);
        if (localCounter != null) {
            int newCount = localCounter.decrementAndGet();
            if (newCount < 0) {
                localCounter.set(0);
            }
        }

        // Also update Redis
        try {
            String key = USAGE_KEY_PREFIX + tenantId + ":sse_connections";
            Long current = redisTemplate.opsForValue().decrement(key);
            if (current != null && current < 0) {
                redisTemplate.opsForValue().set(key, "0");
            }
        } catch (Exception e) {
            log.warn(
                    "Failed to update Redis SSE counter for tenant {}: {}",
                    tenantId,
                    e.getMessage());
        }

        log.debug("SSE connection closed for tenant {}", tenantId);
    }

    /** Get current connection count for a tenant. */
    public int getCurrentConnections(UUID tenantId) {
        AtomicInteger localCounter = localConnectionCounts.get(tenantId);
        return localCounter != null ? Math.max(0, localCounter.get()) : 0;
    }

    /** Get max SSE connections limit for a tenant. */
    public int getMaxConnections(UUID tenantId) {
        try {
            String key = QUOTA_KEY_PREFIX + tenantId + ":max_sse_connections";
            String value = redisTemplate.opsForValue().get(key);
            return value != null ? Integer.parseInt(value) : DEFAULT_MAX_SSE_CONNECTIONS;
        } catch (Exception e) {
            log.warn(
                    "Failed to get max SSE connections for tenant {}: {}",
                    tenantId,
                    e.getMessage());
            return DEFAULT_MAX_SSE_CONNECTIONS;
        }
    }

    private Counter getOrCreateRejectedCounter(UUID tenantId) {
        return rejectedCounters.computeIfAbsent(
                tenantId,
                id ->
                        Counter.builder("sse_connection_rejected_total")
                                .tag("tenant_id", tenantId.toString())
                                .description("Total SSE connections rejected due to limit")
                                .register(meterRegistry));
    }
}
