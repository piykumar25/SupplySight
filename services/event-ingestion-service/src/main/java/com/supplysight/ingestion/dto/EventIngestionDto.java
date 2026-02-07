package com.supplysight.ingestion.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** DTOs for event ingestion operations. */
public final class EventIngestionDto {

    private EventIngestionDto() {}

    /** Request to ingest a tracking event. */
    public record IngestRequest(
            @NotNull(message = "eventId is required") UUID eventId,
            @NotNull(message = "tenantId is required") UUID tenantId,
            @NotNull(message = "shipmentId is required") UUID shipmentId,
            @NotBlank(message = "eventType is required")
                    @Size(max = 50, message = "eventType must not exceed 50 characters")
                    String eventType,
            @NotNull(message = "eventTime is required") Instant eventTime,
            @NotBlank(message = "source is required")
                    @Size(max = 100, message = "source must not exceed 100 characters")
                    String source,
            @Valid Location location,
            Map<String, Object> payload,
            @Valid Metadata metadata) {}

    /** Location data in event. */
    public record Location(
            Double lat,
            Double lon,
            @Size(max = 50, message = "hubCode must not exceed 50 characters") String hubCode) {}

    /** Metadata in event. */
    public record Metadata(UUID correlationId, Instant ingestedAt) {}

    /** Response after successful ingestion. */
    public record IngestResponse(UUID eventId, String status, String message, Instant processedAt) {
        public static IngestResponse accepted(UUID eventId) {
            return new IngestResponse(
                    eventId, "ACCEPTED", "Event accepted for processing", Instant.now());
        }

        public static IngestResponse duplicate(UUID eventId) {
            return new IngestResponse(
                    eventId, "DUPLICATE", "Event already processed", Instant.now());
        }

        public static IngestResponse rejected(UUID eventId, String reason) {
            return new IngestResponse(eventId, "REJECTED", reason, Instant.now());
        }
    }

    /** Batch ingestion request. */
    public record BatchIngestRequest(
            @NotNull(message = "events list is required")
                    @Size(min = 1, max = 100, message = "Batch must contain 1-100 events")
                    java.util.List<@Valid IngestRequest> events) {}

    /** Batch ingestion response. */
    public record BatchIngestResponse(
            int total,
            int accepted,
            int duplicates,
            int rejected,
            java.util.List<IngestResponse> results) {}

    /** Event query parameters. */
    public record EventQuery(
            UUID tenantId,
            UUID shipmentId,
            String eventType,
            Instant startTime,
            Instant endTime,
            int page,
            int size) {}

    /** Stored event response. */
    public record EventResponse(
            UUID id,
            UUID eventId,
            UUID tenantId,
            UUID shipmentId,
            String eventType,
            Instant eventTime,
            String source,
            Location location,
            Map<String, Object> payload,
            Instant ingestedAt,
            UUID correlationId) {}
}
