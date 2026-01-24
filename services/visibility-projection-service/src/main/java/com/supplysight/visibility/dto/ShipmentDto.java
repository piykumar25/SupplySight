package com.supplysight.visibility.dto;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * DTOs for shipment visibility operations.
 */
public final class ShipmentDto {

    private ShipmentDto() {}

    /**
     * Current state response.
     */
    public record CurrentStateResponse(
            UUID shipmentId,
            UUID tenantId,
            String status,
            UUID lastEventId,
            Instant lastEventTime,
            String lastEventType,
            Location lastLocation,
            Location origin,
            Location destination,
            Instant eta,
            Double delayProbability,
            Integer eventCount,
            Instant createdAt,
            Instant updatedAt
    ) implements Serializable {}

    /**
     * Location data.
     */
    public record Location(
            Double lat,
            Double lon,
            String hubCode
    ) implements Serializable {}

    /**
     * Timeline response.
     */
    public record TimelineResponse(
            UUID shipmentId,
            UUID tenantId,
            int eventCount,
            List<TimelineEvent> events
    ) {}

    /**
     * Single timeline event.
     */
    public record TimelineEvent(
            UUID eventId,
            String eventType,
            Instant eventTime,
            String source,
            Location location,
            Map<String, Object> payload
    ) {}

    /**
     * Shipment summary for listing.
     */
    public record ShipmentSummary(
            UUID shipmentId,
            String status,
            String lastEventType,
            Instant lastEventTime,
            Location lastLocation,
            Instant eta
    ) implements Serializable {}

    /**
     * Status statistics.
     */
    public record StatusStats(
            String status,
            long count
    ) {}

    /**
     * Dashboard statistics.
     */
    public record DashboardStats(
            long totalShipments,
            long inTransit,
            long delivered,
            long delayed,
            List<StatusStats> statusBreakdown
    ) {}

    /**
     * Query parameters for listing shipments.
     */
    public record ShipmentQuery(
            String status,
            Instant fromDate,
            Instant toDate,
            int page,
            int size,
            String sortBy,
            String sortDir
    ) {}
}
