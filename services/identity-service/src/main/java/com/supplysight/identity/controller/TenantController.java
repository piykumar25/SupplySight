package com.supplysight.identity.controller;

import com.supplysight.common.dto.ApiResponse;
import com.supplysight.common.dto.PageResponse;
import com.supplysight.identity.dto.TenantDto;
import com.supplysight.identity.entity.Tenant.TenantStatus;
import com.supplysight.identity.service.TenantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller for tenant management operations.
 * All endpoints require ADMIN role.
 */
@RestController
@RequestMapping("/api/v1/tenants")
@Tag(name = "Tenants", description = "Tenant management (ADMIN only)")
@PreAuthorize("hasRole('ADMIN')")
public class TenantController {

    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @PostMapping
    @Operation(summary = "Create Tenant", description = "Create a new tenant (ADMIN only)")
    public ResponseEntity<ApiResponse<TenantDto.Response>> createTenant(
            @Valid @RequestBody TenantDto.CreateRequest request
    ) {
        TenantDto.Response response = tenantService.createTenant(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping("/{tenantId}")
    @Operation(summary = "Get Tenant", description = "Get tenant by ID")
    public ResponseEntity<ApiResponse<TenantDto.Response>> getTenant(
            @Parameter(description = "Tenant ID") @PathVariable UUID tenantId
    ) {
        TenantDto.Response response = tenantService.getTenant(tenantId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/code/{code}")
    @Operation(summary = "Get Tenant by Code", description = "Get tenant by unique code")
    public ResponseEntity<ApiResponse<TenantDto.Response>> getTenantByCode(
            @Parameter(description = "Tenant code") @PathVariable String code
    ) {
        TenantDto.Response response = tenantService.getTenantByCode(code);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{tenantId}")
    @Operation(summary = "Update Tenant", description = "Update tenant details")
    public ResponseEntity<ApiResponse<TenantDto.Response>> updateTenant(
            @Parameter(description = "Tenant ID") @PathVariable UUID tenantId,
            @Valid @RequestBody TenantDto.UpdateRequest request
    ) {
        TenantDto.Response response = tenantService.updateTenant(tenantId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{tenantId}")
    @Operation(summary = "Delete Tenant", description = "Soft delete a tenant")
    public ResponseEntity<ApiResponse<Void>> deleteTenant(
            @Parameter(description = "Tenant ID") @PathVariable UUID tenantId
    ) {
        tenantService.deleteTenant(tenantId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping
    @Operation(summary = "List Tenants", description = "List all active tenants with pagination")
    public ResponseEntity<ApiResponse<PageResponse<TenantDto.Summary>>> listTenants(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort field") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction") @RequestParam(defaultValue = "desc") String sortDir,
            @Parameter(description = "Filter by status") @RequestParam(required = false) TenantStatus status
    ) {
        Sort sort = sortDir.equalsIgnoreCase("asc") 
                ? Sort.by(sortBy).ascending() 
                : Sort.by(sortBy).descending();
        PageRequest pageRequest = PageRequest.of(page, Math.min(size, 100), sort);

        Page<TenantDto.Summary> result = status != null 
                ? tenantService.listTenantsByStatus(status, pageRequest)
                : tenantService.listTenants(pageRequest);

        return ResponseEntity.ok(ApiResponse.success(
                PageResponse.of(result.getContent(), page, size, result.getTotalElements())
        ));
    }
}
