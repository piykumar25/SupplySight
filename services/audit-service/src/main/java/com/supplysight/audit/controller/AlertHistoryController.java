package com.supplysight.audit.controller;

import com.supplysight.audit.dto.AlertHistoryDto;
import com.supplysight.audit.service.AlertHistoryService;
import com.supplysight.common.dto.ApiResponse;
import com.supplysight.common.dto.PageResponse;
import com.supplysight.common.security.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for alert history queries.
 */
@RestController
@RequestMapping("/api/v1/audit/alerts")
@Tag(name = "Alert History", description = "Alert history queries and management")
public class AlertHistoryController {

    private final AlertHistoryService alertHistoryService;

    public AlertHistoryController(AlertHistoryService alertHistoryService) {
        this.alertHistoryService = alertHistoryService;
    }

    @GetMapping
    @Operation(summary = "Get alert history", description = "Get paginated alert history for the tenant")
    public ResponseEntity<ApiResponse<PageResponse<AlertHistoryDto.Response>>> getAlertHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        UUID tenantId = TenantContext.getCurrentTenant()
                .orElseThrow(() -> new IllegalStateException("Tenant context not available"));
        
        PageResponse<AlertHistoryDto.Response> response = alertHistoryService.getAlertHistory(
                tenantId, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get alerts by status", description = "Get alerts filtered by status")
    public ResponseEntity<ApiResponse<PageResponse<AlertHistoryDto.Response>>> getAlertsByStatus(
            @PathVariable String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        UUID tenantId = TenantContext.getCurrentTenant()
                .orElseThrow(() -> new IllegalStateException("Tenant context not available"));
        
        PageResponse<AlertHistoryDto.Response> response = alertHistoryService.getAlertsByStatus(
                tenantId, status, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/open")
    @Operation(summary = "Get open alerts", description = "Get all open and acknowledged alerts")
    public ResponseEntity<ApiResponse<List<AlertHistoryDto.Response>>> getOpenAlerts() {
        UUID tenantId = TenantContext.getCurrentTenant()
                .orElseThrow(() -> new IllegalStateException("Tenant context not available"));
        
        List<AlertHistoryDto.Response> alerts = alertHistoryService.getOpenAlerts(tenantId);
        return ResponseEntity.ok(ApiResponse.success(alerts));
    }

    @GetMapping("/shipment/{shipmentId}")
    @Operation(summary = "Get alerts by shipment", description = "Get alert history for a specific shipment")
    public ResponseEntity<ApiResponse<PageResponse<AlertHistoryDto.Response>>> getAlertsByShipment(
            @PathVariable UUID shipmentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        UUID tenantId = TenantContext.getCurrentTenant()
                .orElseThrow(() -> new IllegalStateException("Tenant context not available"));
        
        PageResponse<AlertHistoryDto.Response> response = alertHistoryService.getAlertsByShipment(
                tenantId, shipmentId, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/summary")
    @Operation(summary = "Get alert summary", description = "Get alert summary statistics")
    public ResponseEntity<ApiResponse<AlertHistoryDto.AlertSummary>> getAlertSummary() {
        UUID tenantId = TenantContext.getCurrentTenant()
                .orElseThrow(() -> new IllegalStateException("Tenant context not available"));
        
        AlertHistoryDto.AlertSummary summary = alertHistoryService.getAlertSummary(tenantId);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @PostMapping("/{alertId}/acknowledge")
    @Operation(summary = "Acknowledge alert", description = "Mark an alert as acknowledged")
    public ResponseEntity<ApiResponse<AlertHistoryDto.Response>> acknowledgeAlert(
            @PathVariable UUID alertId) {
        
        UUID userId = TenantContext.getCurrentTenantInfo()
                .map(TenantContext.TenantInfo::userId)
                .orElseThrow(() -> new IllegalStateException("User context not available"));
        
        return alertHistoryService.acknowledgeAlert(alertId, userId)
                .map(alert -> ResponseEntity.ok(ApiResponse.success(alert)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{alertId}/resolve")
    @Operation(summary = "Resolve alert", description = "Mark an alert as resolved")
    public ResponseEntity<ApiResponse<AlertHistoryDto.Response>> resolveAlert(
            @PathVariable UUID alertId) {
        
        UUID userId = TenantContext.getCurrentTenantInfo()
                .map(TenantContext.TenantInfo::userId)
                .orElseThrow(() -> new IllegalStateException("User context not available"));
        
        return alertHistoryService.resolveAlert(alertId, userId)
                .map(alert -> ResponseEntity.ok(ApiResponse.success(alert)))
                .orElse(ResponseEntity.notFound().build());
    }
}
