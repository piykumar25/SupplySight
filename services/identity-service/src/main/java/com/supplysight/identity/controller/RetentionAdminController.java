package com.supplysight.identity.controller;

import com.supplysight.common.dto.ApiResponse;
import com.supplysight.identity.entity.RetentionPolicy;
import com.supplysight.identity.repository.RetentionPolicyRepository;
import com.supplysight.identity.service.DataPurgeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Admin controller for data retention policy management.
 */
@RestController
@RequestMapping("/admin/retention")
@Tag(name = "Retention Admin", description = "Data retention policy management (ADMIN only)")
@PreAuthorize("hasRole('ADMIN')")
public class RetentionAdminController {

    private final RetentionPolicyRepository policyRepository;
    private final DataPurgeService purgeService;

    public RetentionAdminController(
            RetentionPolicyRepository policyRepository,
            DataPurgeService purgeService) {
        this.policyRepository = policyRepository;
        this.purgeService = purgeService;
    }

    @GetMapping("/tenants/{tenantId}/policy")
    @Operation(summary = "Get Retention Policy", description = "Get data retention policy for a tenant")
    public ResponseEntity<ApiResponse<RetentionPolicyResponse>> getPolicy(
            @Parameter(description = "Tenant ID") @PathVariable UUID tenantId) {
        RetentionPolicy policy = policyRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new RuntimeException("Retention policy not found for tenant"));
        return ResponseEntity.ok(ApiResponse.success(toResponse(policy)));
    }

    @PutMapping("/tenants/{tenantId}/policy")
    @Operation(summary = "Update Retention Policy", description = "Update data retention policy for a tenant")
    public ResponseEntity<ApiResponse<RetentionPolicyResponse>> updatePolicy(
            @Parameter(description = "Tenant ID") @PathVariable UUID tenantId,
            @Valid @RequestBody UpdatePolicyRequest request) {

        RetentionPolicy policy = policyRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new RuntimeException("Retention policy not found for tenant"));

        if (request.eventRetentionDays != null) {
            policy.setEventRetentionDays(request.eventRetentionDays);
        }
        if (request.alertRetentionDays != null) {
            policy.setAlertRetentionDays(request.alertRetentionDays);
        }
        if (request.shipmentRetentionDays != null) {
            policy.setShipmentRetentionDays(request.shipmentRetentionDays);
        }
        if (request.softDeleteEnabled != null) {
            policy.setSoftDeleteEnabled(request.softDeleteEnabled);
        }
        if (request.softDeleteGraceDays != null) {
            policy.setSoftDeleteGraceDays(request.softDeleteGraceDays);
        }

        policy = policyRepository.save(policy);
        return ResponseEntity.ok(ApiResponse.success(toResponse(policy)));
    }

    @PostMapping("/tenants/{tenantId}/purge")
    @Operation(summary = "Trigger Manual Purge", description = "Manually trigger data purge for a tenant")
    public ResponseEntity<ApiResponse<PurgeResult>> triggerPurge(
            @Parameter(description = "Tenant ID") @PathVariable UUID tenantId) {

        RetentionPolicy policy = policyRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new RuntimeException("Retention policy not found for tenant"));

        int purgedCount = purgeService.purgeDataForTenant(policy);
        return ResponseEntity.ok(ApiResponse.success(new PurgeResult(purgedCount)));
    }

    @DeleteMapping("/tenants/{tenantId}/data")
    @Operation(summary = "Delete Tenant Data (GDPR)", description = "Permanently delete all data for a tenant")
    public ResponseEntity<ApiResponse<String>> deleteTenantData(
            @Parameter(description = "Tenant ID") @PathVariable UUID tenantId,
            @RequestParam(name = "confirm") String confirmation) {

        if (!"DELETE_ALL_DATA".equals(confirmation)) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("INVALID_PARAMETER",
                            "Must provide confirmation parameter: confirm=DELETE_ALL_DATA"));
        }

        purgeService.deleteTenantData(tenantId);
        return ResponseEntity.ok(ApiResponse.success("All tenant data has been permanently deleted"));
    }

    private RetentionPolicyResponse toResponse(RetentionPolicy policy) {
        return new RetentionPolicyResponse(
                policy.getTenantId(),
                policy.getEventRetentionDays(),
                policy.getAlertRetentionDays(),
                policy.getShipmentRetentionDays(),
                policy.getAuditLogRetentionDays(),
                policy.isSoftDeleteEnabled(),
                policy.getSoftDeleteGraceDays());
    }

    // DTOs
    public record UpdatePolicyRequest(
            @Min(7) @Max(3650) Integer eventRetentionDays,
            @Min(7) @Max(365) Integer alertRetentionDays,
            @Min(30) @Max(3650) Integer shipmentRetentionDays,
            Boolean softDeleteEnabled,
            @Min(7) @Max(90) Integer softDeleteGraceDays) {
    }

    public record RetentionPolicyResponse(
            UUID tenantId,
            int eventRetentionDays,
            int alertRetentionDays,
            int shipmentRetentionDays,
            int auditLogRetentionDays,
            boolean softDeleteEnabled,
            int softDeleteGraceDays) {
    }

    public record PurgeResult(int recordsPurged) {
    }
}
