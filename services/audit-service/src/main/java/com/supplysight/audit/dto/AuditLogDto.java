package com.supplysight.audit.dto;

import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/**
 * DTOs for audit log operations.
 */
public class AuditLogDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private UUID eventId;
        private UUID tenantId;
        private UUID userId;
        private String username;
        private String action;
        private String resourceType;
        private String resourceId;
        private Instant eventTime;
        private String sourceIp;
        private String userAgent;
        private Map<String, Object> details;
        private String correlationId;
        private Instant createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchRequest {
        private UUID userId;
        private String action;
        private String resourceType;
        private Instant startTime;
        private Instant endTime;
        private int page = 0;
        private int size = 20;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActionCount {
        private String action;
        private long count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyStats {
        private LocalDate date;
        private long count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuditSummary {
        private UUID tenantId;
        private long totalEvents;
        private long last24Hours;
        private Map<String, Long> actionCounts;
        private Instant generatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComplianceReport {
        private UUID tenantId;
        private LocalDate startDate;
        private LocalDate endDate;
        private long totalAuditEvents;
        private Map<String, Long> eventsByAction;
        private Map<String, Long> eventsByUser;
        private Map<String, Long> eventsByResourceType;
        private Instant generatedAt;
    }
}
