package com.supplysight.identity.controller;

import com.supplysight.common.dto.ApiResponse;
import com.supplysight.identity.dto.TenantQuotaDto;
import com.supplysight.identity.service.TenantQuotaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Admin controller for tenant quota and usage management. All endpoints require ADMIN role. */
@RestController
@RequestMapping("/admin/tenants")
@Tag(name = "Tenant Admin", description = "Tenant quota and usage management (ADMIN only)")
@PreAuthorize("hasRole('ADMIN')")
public class TenantAdminController {

    private final TenantQuotaService quotaService;

    public TenantAdminController(TenantQuotaService quotaService) {
        this.quotaService = quotaService;
    }

    @GetMapping("/{tenantId}/limits")
    @Operation(summary = "Get Tenant Limits", description = "Get quota limits for a tenant")
    public ResponseEntity<ApiResponse<TenantQuotaDto.LimitsResponse>> getLimits(
            @Parameter(description = "Tenant ID") @PathVariable UUID tenantId) {
        TenantQuotaDto.LimitsResponse response = quotaService.getLimits(tenantId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{tenantId}/limits")
    @Operation(summary = "Update Tenant Limits", description = "Update quota limits for a tenant")
    public ResponseEntity<ApiResponse<TenantQuotaDto.LimitsResponse>> updateLimits(
            @Parameter(description = "Tenant ID") @PathVariable UUID tenantId,
            @Valid @RequestBody TenantQuotaDto.UpdateLimitsRequest request) {
        TenantQuotaDto.LimitsResponse response = quotaService.updateLimits(tenantId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{tenantId}/usage")
    @Operation(
            summary = "Get Tenant Usage",
            description = "Get current resource usage for a tenant")
    public ResponseEntity<ApiResponse<TenantQuotaDto.UsageResponse>> getUsage(
            @Parameter(description = "Tenant ID") @PathVariable UUID tenantId) {
        TenantQuotaDto.UsageResponse response = quotaService.getUsage(tenantId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{tenantId}/quota-summary")
    @Operation(
            summary = "Get Quota Summary",
            description = "Get combined limits and usage for a tenant")
    public ResponseEntity<ApiResponse<TenantQuotaDto.QuotaSummary>> getQuotaSummary(
            @Parameter(description = "Tenant ID") @PathVariable UUID tenantId) {
        TenantQuotaDto.LimitsResponse limits = quotaService.getLimits(tenantId);
        TenantQuotaDto.UsageResponse usage = quotaService.getUsage(tenantId);
        TenantQuotaDto.QuotaSummary summary = new TenantQuotaDto.QuotaSummary(limits, usage);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }
}
