package com.supplysight.prediction.dto;

import com.supplysight.prediction.entity.Alert.AlertType;
import com.supplysight.prediction.entity.Alert.Severity;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * DTOs for alert operations.
 */
public final class AlertDto {

    private AlertDto() {}

    /**
     * Alert response.
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
            Instant createdAt
    ) {}

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
            Instant createdAt
    ) {}

    /**
     * Alert message for Kafka.
     */
    public record AlertMessage(
            UUID alertId,
            UUID tenantId,
            UUID shipmentId,
            String alertType,
            String severity,
            String message,
            Instant createdAt
    ) {}

    /**
     * Acknowledge request.
     */
    public record AcknowledgeRequest(
            UUID alertId
    ) {}

    /**
     * Alert count response.
     */
    public record AlertCount(
            long total,
            long unacknowledged
    ) {}
}
