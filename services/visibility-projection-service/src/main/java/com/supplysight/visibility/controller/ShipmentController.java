package com.supplysight.visibility.controller;

import com.supplysight.common.dto.ApiResponse;
import com.supplysight.common.dto.PageResponse;
import com.supplysight.common.security.TenantContext;
import com.supplysight.visibility.dto.ShipmentDto.*;
import com.supplysight.visibility.service.ShipmentQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for shipment visibility APIs. Provides low-latency read access to shipment state
 * and timeline.
 */
@RestController
@RequestMapping("/api/v1/shipments")
@Tag(name = "Shipment Visibility", description = "Real-time shipment visibility APIs")
@PreAuthorize("isAuthenticated()")
public class ShipmentController {

    private final ShipmentQueryService queryService;

    public ShipmentController(ShipmentQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping("/{shipmentId}")
    @Operation(
            summary = "Get Shipment Current State",
            description = "Get the current state of a shipment with low latency")
    public ResponseEntity<ApiResponse<CurrentStateResponse>> getShipmentCurrentState(
            @Parameter(description = "Shipment ID") @PathVariable UUID shipmentId) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new SecurityException("Tenant context not available");
        }
        CurrentStateResponse response = queryService.getShipmentCurrentState(tenantId, shipmentId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{shipmentId}/timeline")
    @Operation(
            summary = "Get Shipment Timeline",
            description = "Get the complete event timeline for a shipment, sorted by event time")
    public ResponseEntity<ApiResponse<TimelineResponse>> getShipmentTimeline(
            @Parameter(description = "Shipment ID") @PathVariable UUID shipmentId) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new SecurityException("Tenant context not available");
        }
        TimelineResponse response = queryService.getShipmentTimeline(tenantId, shipmentId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    @Operation(
            summary = "List Shipments",
            description = "List all shipments with pagination and optional filtering")
    public ResponseEntity<ApiResponse<PageResponse<ShipmentSummary>>> listShipments(
            @Parameter(description = "Filter by status") @RequestParam(required = false)
                    String status,
            @Parameter(description = "From date") @RequestParam(required = false) Instant fromDate,
            @Parameter(description = "To date") @RequestParam(required = false) Instant toDate,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0")
                    int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort field") @RequestParam(defaultValue = "updatedAt")
                    String sortBy,
            @Parameter(description = "Sort direction") @RequestParam(defaultValue = "desc")
                    String sortDir) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new SecurityException("Tenant context not available");
        }

        ShipmentQuery query =
                new ShipmentQuery(status, fromDate, toDate, page, size, sortBy, sortDir);
        Page<ShipmentSummary> result = queryService.listShipments(tenantId, query);

        return ResponseEntity.ok(
                ApiResponse.success(
                        PageResponse.of(
                                result.getContent(), page, size, result.getTotalElements())));
    }

    @GetMapping("/active")
    @Operation(
            summary = "Get Active Shipments",
            description = "Get all currently active (in-transit) shipments")
    public ResponseEntity<ApiResponse<List<CurrentStateResponse>>> getActiveShipments() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new SecurityException("Tenant context not available");
        }
        List<CurrentStateResponse> response = queryService.getActiveShipments(tenantId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/dashboard/stats")
    @Operation(
            summary = "Get Dashboard Statistics",
            description = "Get aggregated statistics for the dashboard")
    public ResponseEntity<ApiResponse<DashboardStats>> getDashboardStats() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new SecurityException("Tenant context not available");
        }
        DashboardStats response = queryService.getDashboardStats(tenantId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
