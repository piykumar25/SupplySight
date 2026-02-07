package com.supplysight.identity.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Instant;
import java.util.UUID;

/** DTOs for tenant quota and usage operations. */
public class TenantQuotaDto {

    private TenantQuotaDto() {}

    /** Response DTO for tenant limits. */
    public record LimitsResponse(
            UUID tenantId,
            Integer maxActiveShipments,
            Integer maxEventsPerSecond,
            Integer maxSseConnections,
            Integer eventRetentionDays,
            Integer alertRetentionDays,
            Integer predictionRetentionDays,
            Instant updatedAt) {}

    /** Request DTO for updating tenant limits. */
    public record UpdateLimitsRequest(
            @Min(1) @Max(100000) Integer maxActiveShipments,
            @Min(1) @Max(10000) Integer maxEventsPerSecond,
            @Min(1) @Max(1000) Integer maxSseConnections,
            @Min(1) @Max(365) Integer eventRetentionDays,
            @Min(1) @Max(365) Integer alertRetentionDays,
            @Min(1) @Max(365) Integer predictionRetentionDays) {}

    /** Response DTO for tenant usage. */
    public record UsageResponse(
            UUID tenantId,
            Integer activeShipments,
            Integer currentEventsPerSecond,
            Integer activeSseConnections,
            Long eventsIngestedToday,
            Long storageUsedBytes,
            UsagePercentages percentages,
            Instant timestamp) {}

    /** Usage as percentage of quota. */
    public record UsagePercentages(
            Double shipmentsPercent, Double eventsPerSecPercent, Double sseConnectionsPercent) {
        public static UsagePercentages calculate(
                int activeShipments,
                int maxShipments,
                int currentEps,
                int maxEps,
                int activeSse,
                int maxSse) {
            return new UsagePercentages(
                    maxShipments > 0 ? (activeShipments * 100.0 / maxShipments) : 0,
                    maxEps > 0 ? (currentEps * 100.0 / maxEps) : 0,
                    maxSse > 0 ? (activeSse * 100.0 / maxSse) : 0);
        }
    }

    /** Combined limits and usage for dashboard. */
    public record QuotaSummary(LimitsResponse limits, UsageResponse usage) {}
}
