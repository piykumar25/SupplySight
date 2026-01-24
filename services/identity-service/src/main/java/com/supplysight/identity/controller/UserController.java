package com.supplysight.identity.controller;

import com.supplysight.common.dto.ApiResponse;
import com.supplysight.common.dto.PageResponse;
import com.supplysight.common.security.TenantContext;
import com.supplysight.identity.dto.UserDto;
import com.supplysight.identity.entity.User.UserStatus;
import com.supplysight.identity.service.UserService;
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
 * Controller for user management operations.
 * All operations are tenant-scoped.
 */
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "User management (tenant-scoped)")
@PreAuthorize("isAuthenticated()")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    @Operation(summary = "Create User", description = "Create a new user within the current tenant (ADMIN or OPS_USER)")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPS_USER')")
    public ResponseEntity<ApiResponse<UserDto.Response>> createUser(
            @Valid @RequestBody UserDto.CreateRequest request
    ) {
        UUID tenantId = TenantContext.getTenantId();
        UserDto.Response response = userService.createUser(tenantId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get User", description = "Get user by ID within the current tenant")
    public ResponseEntity<ApiResponse<UserDto.Response>> getUser(
            @Parameter(description = "User ID") @PathVariable UUID userId
    ) {
        UUID tenantId = TenantContext.getTenantId();
        UserDto.Response response = userService.getUser(tenantId, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{userId}")
    @Operation(summary = "Update User", description = "Update user details (ADMIN or OPS_USER)")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPS_USER')")
    public ResponseEntity<ApiResponse<UserDto.Response>> updateUser(
            @Parameter(description = "User ID") @PathVariable UUID userId,
            @Valid @RequestBody UserDto.UpdateRequest request
    ) {
        UUID tenantId = TenantContext.getTenantId();
        UserDto.Response response = userService.updateUser(tenantId, userId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{userId}/password")
    @Operation(summary = "Change Password", description = "Change user password (own password or ADMIN)")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Parameter(description = "User ID") @PathVariable UUID userId,
            @Valid @RequestBody UserDto.ChangePasswordRequest request
    ) {
        UUID tenantId = TenantContext.getTenantId();
        UUID currentUserId = TenantContext.getUserId();

        // Users can change their own password, or ADMIN can change any password
        if (!userId.equals(currentUserId) && !TenantContext.get().roles().contains("ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    ApiResponse.error("FORBIDDEN", "Cannot change another user's password")
            );
        }

        userService.changePassword(tenantId, userId, request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "Delete User", description = "Soft delete a user (ADMIN only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @Parameter(description = "User ID") @PathVariable UUID userId
    ) {
        UUID tenantId = TenantContext.getTenantId();
        userService.deleteUser(tenantId, userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping
    @Operation(summary = "List Users", description = "List all users in the current tenant with pagination")
    public ResponseEntity<ApiResponse<PageResponse<UserDto.Summary>>> listUsers(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort field") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction") @RequestParam(defaultValue = "desc") String sortDir,
            @Parameter(description = "Filter by status") @RequestParam(required = false) UserStatus status
    ) {
        UUID tenantId = TenantContext.getTenantId();

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        PageRequest pageRequest = PageRequest.of(page, Math.min(size, 100), sort);

        Page<UserDto.Summary> result = status != null
                ? userService.listUsersByStatus(tenantId, status, pageRequest)
                : userService.listUsers(tenantId, pageRequest);

        return ResponseEntity.ok(ApiResponse.success(
                PageResponse.of(result.getContent(), page, size, result.getTotalElements())
        ));
    }
}
