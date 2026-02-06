package com.supplysight.identity.metrics;

import com.supplysight.identity.repository.TenantQuotaRepository;
import com.supplysight.identity.entity.TenantQuota;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Collects and exposes tenant quota usage metrics to Prometheus.
 */
@Component
public class TenantQuotaMetricsCollector {

    private static final Logger log = LoggerFactory.getLogger(TenantQuotaMetricsCollector.class);

    private static final String USAGE_KEY_PREFIX = "tenant:usage:";

    private final TenantQuotaRepository quotaRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final MeterRegistry meterRegistry;

    // Cache for gauge values
    private final Map<String, AtomicLong> usageGauges = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> limitGauges = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> percentageGauges = new ConcurrentHashMap<>();

    public TenantQuotaMetricsCollector(
            TenantQuotaRepository quotaRepository,
            RedisTemplate<String, String> redisTemplate,
            MeterRegistry meterRegistry) {
        this.quotaRepository = quotaRepository;
        this.redisTemplate = redisTemplate;
        this.meterRegistry = meterRegistry;
    }

    /**
     * Periodically collect and update quota metrics for all tenants.
     */
    @Scheduled(fixedRate = 30000) // Every 30 seconds
    public void collectMetrics() {
        try {
            List<TenantQuota> allQuotas = quotaRepository.findAll();

            for (TenantQuota quota : allQuotas) {
                UUID tenantId = quota.getTenantId();
                String tenantIdStr = tenantId.toString();

                // Collect shipment metrics
                collectShipmentMetrics(tenantIdStr, quota.getMaxActiveShipments());

                // Collect events per second metrics
                collectEventsPerSecondMetrics(tenantIdStr, quota.getMaxEventsPerSecond());

                // Collect SSE connection metrics
                collectSseConnectionMetrics(tenantIdStr, quota.getMaxSseConnections());
            }
        } catch (Exception e) {
            log.error("Error collecting quota metrics: {}", e.getMessage());
        }
    }

    private void collectShipmentMetrics(String tenantId, int maxShipments) {
        String usageKey = USAGE_KEY_PREFIX + tenantId + ":active_shipments";
        long currentUsage = getRedisValue(usageKey);

        registerOrUpdateGauge("tenant_quota_shipments_usage", tenantId, currentUsage, usageGauges);
        registerOrUpdateGauge("tenant_quota_shipments_limit", tenantId, maxShipments, limitGauges);

        double percentage = maxShipments > 0 ? (double) currentUsage / maxShipments * 100 : 0;
        registerOrUpdatePercentageGauge("tenant_quota_shipments_percent", tenantId, percentage, percentageGauges);
    }

    private void collectEventsPerSecondMetrics(String tenantId, int maxEventsPerSecond) {
        // Get average EPS over last 5 seconds
        long currentSecond = System.currentTimeMillis() / 1000;
        long totalEvents = 0;
        for (int i = 0; i < 5; i++) {
            String key = USAGE_KEY_PREFIX + tenantId + ":eps:" + (currentSecond - i);
            totalEvents += getRedisValue(key);
        }
        long avgEps = totalEvents / 5;

        registerOrUpdateGauge("tenant_quota_eps_usage", tenantId, avgEps, usageGauges);
        registerOrUpdateGauge("tenant_quota_eps_limit", tenantId, maxEventsPerSecond, limitGauges);

        double percentage = maxEventsPerSecond > 0 ? (double) avgEps / maxEventsPerSecond * 100 : 0;
        registerOrUpdatePercentageGauge("tenant_quota_eps_percent", tenantId, percentage, percentageGauges);
    }

    private void collectSseConnectionMetrics(String tenantId, int maxSseConnections) {
        String usageKey = USAGE_KEY_PREFIX + tenantId + ":sse_connections";
        long currentUsage = getRedisValue(usageKey);

        registerOrUpdateGauge("tenant_quota_sse_usage", tenantId, currentUsage, usageGauges);
        registerOrUpdateGauge("tenant_quota_sse_limit", tenantId, maxSseConnections, limitGauges);

        double percentage = maxSseConnections > 0 ? (double) currentUsage / maxSseConnections * 100 : 0;
        registerOrUpdatePercentageGauge("tenant_quota_sse_percent", tenantId, percentage, percentageGauges);
    }

    private void registerOrUpdateGauge(String name, String tenantId, long value, Map<String, AtomicLong> cache) {
        String cacheKey = name + ":" + tenantId;
        AtomicLong gauge = cache.computeIfAbsent(cacheKey, k -> {
            AtomicLong newGauge = new AtomicLong(value);
            Gauge.builder(name, newGauge, AtomicLong::get)
                    .tag("tenant_id", tenantId)
                    .description("Tenant quota metric: " + name)
                    .register(meterRegistry);
            return newGauge;
        });
        gauge.set(value);
    }

    private void registerOrUpdatePercentageGauge(String name, String tenantId, double value,
            Map<String, AtomicLong> cache) {
        registerOrUpdateGauge(name, tenantId, (long) value, cache);
    }

    private long getRedisValue(String key) {
        try {
            String value = redisTemplate.opsForValue().get(key);
            return value != null ? Long.parseLong(value) : 0;
        } catch (Exception e) {
            log.debug("Failed to get Redis value for key {}: {}", key, e.getMessage());
            return 0;
        }
    }
}
