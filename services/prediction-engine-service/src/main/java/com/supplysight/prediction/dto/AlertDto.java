package com.supplysight.prediction.dto;

import com.supplysight.prediction.entity.Alert.AlertType;
import com.supplysight.prediction.entity.Alert.Severity;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * DTOs for alert operations.
 */
public final class AlertDto {

        private AlertDto() {
        }

        /**
         * Alert response (full details).
         */
        public record AlertResponse(
                        UUID id,
                        UUID shipmentId,
                        UUID tenantId,
                        UUID predictionId,
                        AlertType alertType,
                        Severity severity,
                        String message,
                        Map<String, Object> details,
                        boolean acknowledged,
                        Instant acknowledgedAt,
                        UUID acknowledgedBy,
                        boolean resolved,
                        Instant resolvedAt,
                        UUID resolvedBy,
                        String resolutionComment,
                        Instant createdAt) {
        }

        /**
         * Alert summary for listing.
         */
        public record AlertSummary(
                        UUID id,
                        UUID shipmentId,
                        AlertType alertType,
                        Severity severity,
                        String message,
                        boolean acknowledged,
                        boolean resolved,
                        Instant createdAt) {
        }

        /**
         * Alert message for Kafka/SSE.
         */
        public record AlertMessage(
                        UUID alertId,
                        UUID tenantId,
                        UUID shipmentId,
                        String alertType,
                        String severity,
                        String message,
                        Instant createdAt) {
        }

        /**
         * Acknowledge request with optional comment.
         */
        public record AcknowledgeRequest(
                        String comment) {
        }

        /**
         * Resolve request with optional comment.
         */
        public record ResolveRequest(
                        String comment) {
        }

        /**
         * Bulk action request for multiple alerts.
         */
        public record BulkActionRequest(
                        List<UUID> alertIds,
                        String comment) {
        }

        /**
         * Bulk action response.
         */
        public record BulkActionResponse(
                        int successCount,
                        int failureCount,
                        List<UUID> failedIds) {
        }

        /**
         * Alert count response.
         */
        public record AlertCount(
                        long total,
                        long unacknowledged,
                        long unresolved) {
        }

        /**
         * SSE event for real-time updates.
         */
        public record AlertEvent(
                        String type,
                        AlertSummary data,
                        Instant timestamp) {
        }
}
