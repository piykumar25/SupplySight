package com.supplysight.common.event;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Core tracking event schema for supply chain visibility.
 * Used across all services for event-driven communication.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TrackingEvent(
    @NotNull(message = "eventId is required")
    UUID eventId,

    @NotNull(message = "tenantId is required")
    UUID tenantId,

    @NotNull(message = "shipmentId is required")
    UUID shipmentId,

    @NotBlank(message = "eventType is required")
    @Size(max = 50)
    String eventType,

    @NotNull(message = "eventTime is required")
    Instant eventTime,

    @NotBlank(message = "source is required")
    @Size(max = 100)
    String source,

    @Valid
    Location location,

    Map<String, Object> payload,

    @Valid
    EventMetadata metadata
) {
    /**
     * Location data for tracking events.
     */
    public record Location(
        Double lat,
        Double lon,
        @Size(max = 50)
        String hubCode
    ) {}

    /**
     * Metadata for event traceability and auditing.
     */
    public record EventMetadata(
        Instant ingestedAt,
        UUID correlationId,
        String sourceIp,
        String userAgent
    ) {}

    /**
     * Event types for supply chain tracking.
     */
    public static final class EventTypes {
        public static final String CREATED = "CREATED";
        public static final String PICKED_UP = "PICKED_UP";
        public static final String IN_TRANSIT = "IN_TRANSIT";
        public static final String AT_HUB = "AT_HUB";
        public static final String OUT_FOR_DELIVERY = "OUT_FOR_DELIVERY";
        public static final String DELIVERED = "DELIVERED";
        public static final String DELAYED = "DELAYED";
        public static final String EXCEPTION = "EXCEPTION";
        public static final String RETURNED = "RETURNED";
        public static final String CANCELLED = "CANCELLED";

        private EventTypes() {}
    }

    /**
     * Event sources for tracking.
     */
    public static final class EventSources {
        public static final String GPS_DEVICE = "GPS_DEVICE";
        public static final String SCANNER = "SCANNER";
        public static final String PARTNER_WEBHOOK = "PARTNER_WEBHOOK";
        public static final String MANUAL_ENTRY = "MANUAL_ENTRY";
        public static final String IOT_SENSOR = "IOT_SENSOR";
        public static final String SYSTEM = "SYSTEM";

        private EventSources() {}
    }
}
