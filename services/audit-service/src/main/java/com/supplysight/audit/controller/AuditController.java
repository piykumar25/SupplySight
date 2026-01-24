package com.supplysight.audit.controller;

import com.supplysight.audit.dto.AuditLogDto;
import com.supplysight.audit.service.AuditQueryService;
import com.supplysight.common.dto.ApiResponse;
import com.supplysight.common.dto.PageResponse;
import com.supplysight.common.security.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for audit log queries.
 */
@RestController
@RequestMapping("/api/v1/audit")
@Tag(name = "Audit", description = "Audit log queries and compliance reporting")
public class AuditController {

    private final AuditQueryService auditQueryService;

    public AuditController(AuditQueryService auditQueryService) {
        this.auditQueryService = auditQueryService;
    }

    @GetMapping("/logs")
    @Operation(summary = "Get audit logs", description = "Get paginated audit logs for the tenant")
    public ResponseEntity<ApiResponse<PageResponse<AuditLogDto.Response>>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        UUID tenantId = TenantContext.getCurrentTenant()
                .orElseThrow(() -> new IllegalStateException("Tenant context not available"));
        
        PageResponse<AuditLogDto.Response> response = auditQueryService.getAuditLogs(tenantId, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/logs/search")
    @Operation(summary = "Search audit logs", description = "Search audit logs with filters")
    public ResponseEntity<ApiResponse<PageResponse<AuditLogDto.Response>>> searchAuditLogs(
            @RequestBody AuditLogDto.SearchRequest request) {
        
        UUID tenantId = TenantContext.getCurrentTenant()
                .orElseThrow(() -> new IllegalStateException("Tenant context not available"));
        
        PageResponse<AuditLogDto.Response> response = auditQueryService.searchAuditLogs(tenantId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/logs/user/{userId}")
    @Operation(summary = "Get user audit logs", description = "Get audit logs for a specific user")
    public ResponseEntity<ApiResponse<PageResponse<AuditLogDto.Response>>> getAuditLogsByUser(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        UUID tenantId = TenantContext.getCurrentTenant()
                .orElseThrow(() -> new IllegalStateException("Tenant context not available"));
        
        PageResponse<AuditLogDto.Response> response = auditQueryService.getAuditLogsByUser(
                tenantId, userId, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/logs/resource/{resourceType}/{resourceId}")
    @Operation(summary = "Get resource audit logs", description = "Get audit logs for a specific resource")
    public ResponseEntity<ApiResponse<PageResponse<AuditLogDto.Response>>> getAuditLogsByResource(
            @PathVariable String resourceType,
            @PathVariable String resourceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        UUID tenantId = TenantContext.getCurrentTenant()
                .orElseThrow(() -> new IllegalStateException("Tenant context not available"));
        
        PageResponse<AuditLogDto.Response> response = auditQueryService.getAuditLogsByResource(
                tenantId, resourceType, resourceId, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/logs/correlation/{correlationId}")
    @Operation(summary = "Get log by correlation ID", description = "Get audit log by correlation ID")
    public ResponseEntity<ApiResponse<AuditLogDto.Response>> getAuditLogByCorrelationId(
            @PathVariable String correlationId) {
        
        return auditQueryService.getAuditLogByCorrelationId(correlationId)
                .map(log -> ResponseEntity.ok(ApiResponse.success(log)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/summary")
    @Operation(summary = "Get audit summary", description = "Get audit summary statistics for the tenant")
    public ResponseEntity<ApiResponse<AuditLogDto.AuditSummary>> getAuditSummary() {
        UUID tenantId = TenantContext.getCurrentTenant()
                .orElseThrow(() -> new IllegalStateException("Tenant context not available"));
        
        AuditLogDto.AuditSummary summary = auditQueryService.getAuditSummary(tenantId);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @GetMapping("/report/compliance")
    @Operation(summary = "Generate compliance report", description = "Generate compliance report for a date range")
    public ResponseEntity<ApiResponse<AuditLogDto.ComplianceReport>> getComplianceReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        UUID tenantId = TenantContext.getCurrentTenant()
                .orElseThrow(() -> new IllegalStateException("Tenant context not available"));
        
        AuditLogDto.ComplianceReport report = auditQueryService.generateComplianceReport(
                tenantId, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    @GetMapping("/stats/daily")
    @Operation(summary = "Get daily statistics", description = "Get daily audit statistics for a date range")
    public ResponseEntity<ApiResponse<List<AuditLogDto.DailyStats>>> getDailyStats(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        UUID tenantId = TenantContext.getCurrentTenant()
                .orElseThrow(() -> new IllegalStateException("Tenant context not available"));
        
        List<AuditLogDto.DailyStats> stats = auditQueryService.getDailyStats(tenantId, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}
