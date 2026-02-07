package com.supplysight.audit.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.*;

/** DTOs for alert history operations. */
public class AlertHistoryDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private UUID alertId;
        private UUID tenantId;
        private UUID shipmentId;
        private String alertType;
        private String severity;
        private String message;
        private String status;
        private UUID acknowledgedBy;
        private Instant acknowledgedAt;
        private Instant resolvedAt;
        private Instant createdAt;
        private Map<String, Object> details;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AlertSummary {
        private UUID tenantId;
        private long totalAlerts;
        private long openAlerts;
        private long acknowledgedAlerts;
        private long resolvedAlerts;
        private Map<String, Long> alertsByType;
        private Instant generatedAt;
    }
}
