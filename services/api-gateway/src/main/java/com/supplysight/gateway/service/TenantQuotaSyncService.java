package com.supplysight.gateway.service;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Service for syncing tenant quotas from identity service to Redis. This enables fast quota lookups
 * at the gateway level.
 */
@Service
public class TenantQuotaSyncService {

    private static final Logger log = LoggerFactory.getLogger(TenantQuotaSyncService.class);

    private static final String QUOTA_KEY_PREFIX = "tenant:quota:";

    private final ReactiveRedisTemplate<String, String> redisTemplate;

    public TenantQuotaSyncService(ReactiveRedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /** Set tenant quota limits in Redis for fast gateway access. */
    public Mono<Void> setQuotaLimits(UUID tenantId, int maxEventsPerSecond, int maxSseConnections) {
        String tenantIdStr = tenantId.toString();

        return Mono.when(
                        redisTemplate
                                .opsForValue()
                                .set(
                                        QUOTA_KEY_PREFIX + tenantIdStr + ":max_events_per_second",
                                        String.valueOf(maxEventsPerSecond)),
                        redisTemplate
                                .opsForValue()
                                .set(
                                        QUOTA_KEY_PREFIX + tenantIdStr + ":max_sse_connections",
                                        String.valueOf(maxSseConnections)))
                .doOnSuccess(
                        v -> log.debug("Updated quota limits for tenant {} in Redis", tenantId));
    }

    /** Get tenant quota limit from Redis. */
    public Mono<Integer> getQuotaLimit(UUID tenantId, String limitType) {
        String key = QUOTA_KEY_PREFIX + tenantId + ":" + limitType;
        return redisTemplate
                .opsForValue()
                .get(key)
                .map(Integer::parseInt)
                .defaultIfEmpty(getDefaultLimit(limitType));
    }

    /** Remove tenant quota limits from Redis. */
    public Mono<Void> removeQuotaLimits(UUID tenantId) {
        String tenantIdStr = tenantId.toString();

        return Mono.when(
                        redisTemplate.delete(
                                QUOTA_KEY_PREFIX + tenantIdStr + ":max_events_per_second"),
                        redisTemplate.delete(
                                QUOTA_KEY_PREFIX + tenantIdStr + ":max_sse_connections"))
                .then();
    }

    private int getDefaultLimit(String limitType) {
        return switch (limitType) {
            case "max_events_per_second" -> 100;
            case "max_sse_connections" -> 50;
            case "max_active_shipments" -> 1000;
            default -> 100;
        };
    }
}
