package com.supplysight.ingestion.service;

import com.supplysight.common.event.TrackingEvent;
import com.supplysight.common.exception.ValidationException;
import com.supplysight.ingestion.dto.EventIngestionDto.IngestRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Service for validating tracking events before ingestion.
 */
@Service
public class EventValidationService {

    private static final Logger log = LoggerFactory.getLogger(EventValidationService.class);

    private static final Set<String> VALID_EVENT_TYPES = Set.of(
            TrackingEvent.EventTypes.CREATED,
            TrackingEvent.EventTypes.PICKED_UP,
            TrackingEvent.EventTypes.IN_TRANSIT,
            TrackingEvent.EventTypes.AT_HUB,
            TrackingEvent.EventTypes.OUT_FOR_DELIVERY,
            TrackingEvent.EventTypes.DELIVERED,
            TrackingEvent.EventTypes.DELAYED,
            TrackingEvent.EventTypes.EXCEPTION,
            TrackingEvent.EventTypes.RETURNED,
            TrackingEvent.EventTypes.CANCELLED
    );

    private static final Set<String> VALID_SOURCES = Set.of(
            TrackingEvent.EventSources.GPS_DEVICE,
            TrackingEvent.EventSources.SCANNER,
            TrackingEvent.EventSources.PARTNER_WEBHOOK,
            TrackingEvent.EventSources.MANUAL_ENTRY,
            TrackingEvent.EventSources.IOT_SENSOR,
            TrackingEvent.EventSources.SYSTEM
    );

    @Value("${ingestion.late-arrival-tolerance-hours:72}")
    private int lateArrivalToleranceHours;

    @Value("${ingestion.max-payload-size:65536}")
    private int maxPayloadSize;

    /**
     * Validate an incoming event.
     * @throws ValidationException if validation fails
     */
    public void validateEvent(IngestRequest event) {
        Set<String> errors = new HashSet<>();

        // Required field validation
        if (event.eventId() == null) {
            errors.add("eventId is required");
        }
        if (event.tenantId() == null) {
            errors.add("tenantId is required");
        }
        if (event.shipmentId() == null) {
            errors.add("shipmentId is required");
        }
        if (event.eventType() == null || event.eventType().isBlank()) {
            errors.add("eventType is required");
        }
        if (event.eventTime() == null) {
            errors.add("eventTime is required");
        }
        if (event.source() == null || event.source().isBlank()) {
            errors.add("source is required");
        }

        // Return early if required fields are missing
        if (!errors.isEmpty()) {
            throw new ValidationException("Event validation failed", Map.of("errors", String.join(", ", errors)));
        }

        // Event type validation
        if (!VALID_EVENT_TYPES.contains(event.eventType())) {
            errors.add("Invalid eventType: " + event.eventType() + ". Valid types: " + VALID_EVENT_TYPES);
        }

        // Source validation
        if (!VALID_SOURCES.contains(event.source())) {
            errors.add("Invalid source: " + event.source() + ". Valid sources: " + VALID_SOURCES);
        }

        // Event time validation (not in future, not too old)
        validateEventTime(event.eventTime(), errors);

        // Location validation
        if (event.location() != null) {
            validateLocation(event.location(), errors);
        }

        // Payload size validation
        if (event.payload() != null) {
            validatePayloadSize(event.payload(), errors);
        }

        if (!errors.isEmpty()) {
            log.warn("Event validation failed for {}: {}", event.eventId(), errors);
            throw new ValidationException("Event validation failed", Map.of("errors", String.join(", ", errors)));
        }
    }

    private void validateEventTime(Instant eventTime, Set<String> errors) {
        Instant now = Instant.now();

        // Event time cannot be in the future (with small tolerance for clock skew)
        Instant maxFuture = now.plus(Duration.ofMinutes(5));
        if (eventTime.isAfter(maxFuture)) {
            errors.add("eventTime cannot be in the future");
        }

        // Event time cannot be too old
        Instant minPast = now.minus(Duration.ofHours(lateArrivalToleranceHours));
        if (eventTime.isBefore(minPast)) {
            errors.add("eventTime is too old (older than " + lateArrivalToleranceHours + " hours)");
        }
    }

    private void validateLocation(com.supplysight.ingestion.dto.EventIngestionDto.Location location, Set<String> errors) {
        if (location.lat() != null) {
            if (location.lat() < -90 || location.lat() > 90) {
                errors.add("Invalid latitude: must be between -90 and 90");
            }
        }
        if (location.lon() != null) {
            if (location.lon() < -180 || location.lon() > 180) {
                errors.add("Invalid longitude: must be between -180 and 180");
            }
        }
        // If one coordinate is provided, the other should be too
        if ((location.lat() != null && location.lon() == null) || 
            (location.lat() == null && location.lon() != null)) {
            errors.add("Both latitude and longitude must be provided together");
        }
    }

    private void validatePayloadSize(Map<String, Object> payload, Set<String> errors) {
        // Estimate payload size (rough estimation)
        int estimatedSize = estimateJsonSize(payload);
        if (estimatedSize > maxPayloadSize) {
            errors.add("Payload size exceeds maximum allowed (" + maxPayloadSize + " bytes)");
        }
    }

    private int estimateJsonSize(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return 2; // "{}"
        }
        int size = 2; // "{}"
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            size += entry.getKey().length() + 3; // "key":
            size += estimateValueSize(entry.getValue());
            size += 1; // comma
        }
        return size;
    }

    private int estimateValueSize(Object value) {
        if (value == null) {
            return 4; // null
        }
        if (value instanceof String) {
            return ((String) value).length() + 2;
        }
        if (value instanceof Number) {
            return value.toString().length();
        }
        if (value instanceof Boolean) {
            return ((Boolean) value) ? 4 : 5;
        }
        if (value instanceof Map) {
            return estimateJsonSize((Map<String, Object>) value);
        }
        if (value instanceof java.util.List) {
            int size = 2;
            for (Object item : (java.util.List<?>) value) {
                size += estimateValueSize(item) + 1;
            }
            return size;
        }
        return value.toString().length();
    }
}
