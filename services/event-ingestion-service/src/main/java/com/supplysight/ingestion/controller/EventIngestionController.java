package com.supplysight.ingestion.controller;

import com.supplysight.common.dto.ApiResponse;
import com.supplysight.common.security.TenantContext;
import com.supplysight.ingestion.dto.EventIngestionDto.*;
import com.supplysight.ingestion.service.EventIngestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** REST controller for event ingestion. */
@RestController
@RequestMapping("/api/v1/events")
@Tag(name = "Event Ingestion", description = "Tracking event ingestion endpoints")
@PreAuthorize("isAuthenticated()")
public class EventIngestionController {

    private final EventIngestionService eventIngestionService;

    public EventIngestionController(EventIngestionService eventIngestionService) {
        this.eventIngestionService = eventIngestionService;
    }

    @PostMapping("/ingest")
    @Operation(summary = "Ingest Event", description = "Ingest a single tracking event")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "202",
                        description = "Event accepted for processing",
                        content =
                                @Content(schema = @Schema(implementation = IngestResponse.class))),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "400",
                        description = "Invalid event data"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "409",
                        description = "Duplicate event")
            })
    @PreAuthorize("hasAnyRole('ADMIN', 'OPS_USER')")
    public ResponseEntity<ApiResponse<IngestResponse>> ingestEvent(
            @Valid @RequestBody IngestRequest request, HttpServletRequest httpRequest) {
        String sourceIp = getClientIp(httpRequest);
        UUID correlationId = getCorrelationId();

        // Validate tenant access
        validateTenantAccess(request.tenantId());

        IngestResponse response =
                eventIngestionService.ingestEvent(request, sourceIp, correlationId);

        HttpStatus status =
                switch (response.status()) {
                    case "ACCEPTED" -> HttpStatus.ACCEPTED;
                    case "DUPLICATE" -> HttpStatus.OK; // Idempotent - return OK for duplicates
                    case "REJECTED" -> HttpStatus.BAD_REQUEST;
                    default -> HttpStatus.INTERNAL_SERVER_ERROR;
                };

        return ResponseEntity.status(status).body(ApiResponse.success(response));
    }

    @PostMapping("/ingest/batch")
    @Operation(
            summary = "Batch Ingest Events",
            description = "Ingest multiple tracking events in a batch")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "202",
                        description = "Batch processed",
                        content =
                                @Content(
                                        schema =
                                                @Schema(
                                                        implementation =
                                                                BatchIngestResponse.class))),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "400",
                        description = "Invalid batch data")
            })
    @PreAuthorize("hasAnyRole('ADMIN', 'OPS_USER')")
    public ResponseEntity<ApiResponse<BatchIngestResponse>> ingestBatch(
            @Valid @RequestBody BatchIngestRequest request, HttpServletRequest httpRequest) {
        String sourceIp = getClientIp(httpRequest);
        UUID correlationId = getCorrelationId();

        // Validate tenant access for all events
        UUID tenantId = TenantContext.getTenantId();
        for (IngestRequest event : request.events()) {
            if (!event.tenantId().equals(tenantId) && !isAdmin()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(
                                ApiResponse.error(
                                        "FORBIDDEN", "Cannot ingest events for other tenants"));
            }
        }

        BatchIngestResponse response =
                eventIngestionService.ingestBatch(request, sourceIp, correlationId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.success(response));
    }

    @GetMapping("/status/{eventId}")
    @Operation(summary = "Check Event Status", description = "Check if an event has been processed")
    public ResponseEntity<ApiResponse<EventStatusResponse>> checkEventStatus(
            @PathVariable UUID eventId) {
        boolean processed = eventIngestionService.isEventProcessed(eventId);
        EventStatusResponse response =
                new EventStatusResponse(
                        eventId,
                        processed ? "PROCESSED" : "NOT_FOUND",
                        processed
                                ? "Event has been processed"
                                : "Event not found in processed events");
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // Helper methods

    private void validateTenantAccess(UUID requestTenantId) {
        UUID contextTenantId = TenantContext.getTenantId();
        if (!requestTenantId.equals(contextTenantId) && !isAdmin()) {
            throw new com.supplysight.common.exception.ForbiddenException(
                    "Cannot ingest events for other tenants");
        }
    }

    private boolean isAdmin() {
        TenantContext.TenantInfo info = TenantContext.get();
        return info != null && info.roles() != null && info.roles().contains("ADMIN");
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }

    private UUID getCorrelationId() {
        String correlationId = MDC.get("correlationId");
        if (correlationId != null) {
            try {
                return UUID.fromString(correlationId);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return UUID.randomUUID();
    }

    /** Response for event status check. */
    public record EventStatusResponse(UUID eventId, String status, String message) {}
}
