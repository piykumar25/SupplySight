package com.supplysight.identity.service;

import com.supplysight.common.exception.ResourceNotFoundException;
import com.supplysight.identity.dto.TenantQuotaDto;
import com.supplysight.identity.entity.TenantQuota;
import com.supplysight.identity.repository.TenantQuotaRepository;
import com.supplysight.identity.repository.TenantRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Service for managing tenant quotas and tracking usage. */
@Service
@Transactional(readOnly = true)
public class TenantQuotaService {

    private static final Logger log = LoggerFactory.getLogger(TenantQuotaService.class);

    private static final String USAGE_KEY_PREFIX = "tenant:usage:";
    private static final String SSE_CONNECTIONS_KEY = ":sse_connections";
    private static final String EVENTS_TODAY_KEY = ":events_today:";
    private static final String ACTIVE_SHIPMENTS_KEY = ":active_shipments";

    private final TenantQuotaRepository quotaRepository;
    private final TenantRepository tenantRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final MeterRegistry meterRegistry;
    private final ConcurrentHashMap<UUID, Double> quotaUsageGauges = new ConcurrentHashMap<>();

    public TenantQuotaService(
            TenantQuotaRepository quotaRepository,
            TenantRepository tenantRepository,
            RedisTemplate<String, String> redisTemplate,
            MeterRegistry meterRegistry) {
        this.quotaRepository = quotaRepository;
        this.tenantRepository = tenantRepository;
        this.redisTemplate = redisTemplate;
        this.meterRegistry = meterRegistry;
    }

    /** Get tenant limits. */
    public TenantQuotaDto.LimitsResponse getLimits(UUID tenantId) {
        TenantQuota quota = getOrCreateQuota(tenantId);
        return toLimitsResponse(quota);
    }

    /** Update tenant limits. */
    @Transactional
    public TenantQuotaDto.LimitsResponse updateLimits(
            UUID tenantId, TenantQuotaDto.UpdateLimitsRequest request) {
        log.info("Updating limits for tenant: {}", tenantId);

        TenantQuota quota = getOrCreateQuota(tenantId);

        if (request.maxActiveShipments() != null) {
            quota.setMaxActiveShipments(request.maxActiveShipments());
        }
        if (request.maxEventsPerSecond() != null) {
            quota.setMaxEventsPerSecond(request.maxEventsPerSecond());
        }
        if (request.maxSseConnections() != null) {
            quota.setMaxSseConnections(request.maxSseConnections());
        }
        if (request.eventRetentionDays() != null) {
            quota.setEventRetentionDays(request.eventRetentionDays());
        }
        if (request.alertRetentionDays() != null) {
            quota.setAlertRetentionDays(request.alertRetentionDays());
        }
        if (request.predictionRetentionDays() != null) {
            quota.setPredictionRetentionDays(request.predictionRetentionDays());
        }

        quota = quotaRepository.save(quota);
        log.info("Updated limits for tenant: {}", tenantId);

        return toLimitsResponse(quota);
    }

    /** Get current usage for a tenant. */
    public TenantQuotaDto.UsageResponse getUsage(UUID tenantId) {
        TenantQuota quota = getOrCreateQuota(tenantId);

        int activeShipments = getActiveShipments(tenantId);
        int currentEps = getCurrentEventsPerSecond(tenantId);
        int activeSse = getActiveSseConnections(tenantId);
        long eventsToday = getEventsIngestedToday(tenantId);

        TenantQuotaDto.UsagePercentages percentages =
                TenantQuotaDto.UsagePercentages.calculate(
                        activeShipments, quota.getMaxActiveShipments(),
                        currentEps, quota.getMaxEventsPerSecond(),
                        activeSse, quota.getMaxSseConnections());

        // Update metrics gauge
        updateQuotaUsageMetric(tenantId, "shipments", percentages.shipmentsPercent());
        updateQuotaUsageMetric(tenantId, "events_per_sec", percentages.eventsPerSecPercent());
        updateQuotaUsageMetric(tenantId, "sse_connections", percentages.sseConnectionsPercent());

        return new TenantQuotaDto.UsageResponse(
                tenantId,
                activeShipments,
                currentEps,
                activeSse,
                eventsToday,
                0L, // Storage calculation would require additional queries
                percentages,
                Instant.now());
    }

    /** Check if tenant can accept more events (rate limit check). */
    public boolean canAcceptEvent(UUID tenantId) {
        TenantQuota quota = getOrCreateQuota(tenantId);
        int currentEps = getCurrentEventsPerSecond(tenantId);
        return currentEps < quota.getMaxEventsPerSecond();
    }

    /** Check if tenant can open new SSE connection. */
    public boolean canOpenSseConnection(UUID tenantId) {
        TenantQuota quota = getOrCreateQuota(tenantId);
        int activeSse = getActiveSseConnections(tenantId);
        return activeSse < quota.getMaxSseConnections();
    }

    /** Increment SSE connection count. */
    public void incrementSseConnections(UUID tenantId) {
        String key = USAGE_KEY_PREFIX + tenantId + SSE_CONNECTIONS_KEY;
        redisTemplate.opsForValue().increment(key);
    }

    /** Decrement SSE connection count. */
    public void decrementSseConnections(UUID tenantId) {
        String key = USAGE_KEY_PREFIX + tenantId + SSE_CONNECTIONS_KEY;
        Long current = redisTemplate.opsForValue().decrement(key);
        if (current != null && current < 0) {
            redisTemplate.opsForValue().set(key, "0");
        }
    }

    /** Record an event ingestion. */
    public void recordEventIngested(UUID tenantId) {
        String todayKey = USAGE_KEY_PREFIX + tenantId + EVENTS_TODAY_KEY + LocalDate.now();
        redisTemplate.opsForValue().increment(todayKey);
        redisTemplate.expire(todayKey, Duration.ofDays(2));

        // Also increment sliding window counter for EPS calculation
        String epsKey = USAGE_KEY_PREFIX + tenantId + ":eps:" + System.currentTimeMillis() / 1000;
        redisTemplate.opsForValue().increment(epsKey);
        redisTemplate.expire(epsKey, Duration.ofSeconds(10));
    }

    /** Update active shipments count. */
    public void updateActiveShipments(UUID tenantId, int count) {
        String key = USAGE_KEY_PREFIX + tenantId + ACTIVE_SHIPMENTS_KEY;
        redisTemplate.opsForValue().set(key, String.valueOf(count));
    }

    /** Create default quota for a new tenant. */
    @Transactional
    public TenantQuota createDefaultQuota(UUID tenantId) {
        if (!tenantRepository.existsById(tenantId)) {
            throw new ResourceNotFoundException("Tenant", tenantId);
        }
        if (quotaRepository.existsByTenantId(tenantId)) {
            return quotaRepository.findByTenantId(tenantId).orElseThrow();
        }
        TenantQuota quota = TenantQuota.createDefault(tenantId);
        return quotaRepository.save(quota);
    }

    // Private helper methods

    private TenantQuota getOrCreateQuota(UUID tenantId) {
        return quotaRepository
                .findByTenantId(tenantId)
                .orElseGet(() -> createDefaultQuota(tenantId));
    }

    private int getActiveShipments(UUID tenantId) {
        String key = USAGE_KEY_PREFIX + tenantId + ACTIVE_SHIPMENTS_KEY;
        String value = redisTemplate.opsForValue().get(key);
        return value != null ? Integer.parseInt(value) : 0;
    }

    private int getCurrentEventsPerSecond(UUID tenantId) {
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

    private int getActiveSseConnections(UUID tenantId) {
        String key = USAGE_KEY_PREFIX + tenantId + SSE_CONNECTIONS_KEY;
        String value = redisTemplate.opsForValue().get(key);
        return value != null ? Math.max(0, Integer.parseInt(value)) : 0;
    }

    private long getEventsIngestedToday(UUID tenantId) {
        String key = USAGE_KEY_PREFIX + tenantId + EVENTS_TODAY_KEY + LocalDate.now();
        String value = redisTemplate.opsForValue().get(key);
        return value != null ? Long.parseLong(value) : 0L;
    }

    private TenantQuotaDto.LimitsResponse toLimitsResponse(TenantQuota quota) {
        return new TenantQuotaDto.LimitsResponse(
                quota.getTenantId(),
                quota.getMaxActiveShipments(),
                quota.getMaxEventsPerSecond(),
                quota.getMaxSseConnections(),
                quota.getEventRetentionDays(),
                quota.getAlertRetentionDays(),
                quota.getPredictionRetentionDays(),
                quota.getUpdatedAt());
    }

    private void updateQuotaUsageMetric(UUID tenantId, String resource, Double percentage) {
        String metricKey = tenantId + ":" + resource;
        quotaUsageGauges.computeIfAbsent(
                UUID.nameUUIDFromBytes(metricKey.getBytes()),
                k -> {
                    Gauge.builder("tenant_quota_usage_percent", () -> percentage)
                            .tag("tenant_id", tenantId.toString())
                            .tag("resource", resource)
                            .description("Percentage of quota used by tenant")
                            .register(meterRegistry);
                    return percentage;
                });
    }
}
