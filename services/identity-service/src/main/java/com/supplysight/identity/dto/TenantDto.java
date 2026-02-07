package com.supplysight.identity.dto;

import com.supplysight.identity.entity.Tenant.TenantStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** DTOs for Tenant operations. */
public final class TenantDto {

    private TenantDto() {}

    /** Request to create a new tenant. */
    public record CreateRequest(
            @NotBlank(message = "Name is required")
                    @Size(min = 2, max = 255, message = "Name must be between 2 and 255 characters")
                    String name,
            @NotBlank(message = "Code is required")
                    @Size(min = 2, max = 50, message = "Code must be between 2 and 50 characters")
                    @Pattern(
                            regexp = "^[a-z0-9-]+$",
                            message =
                                    "Code must contain only lowercase letters, numbers, and hyphens")
                    String code,
            @Email(message = "Invalid email format") String contactEmail,
            @Size(max = 50) String contactPhone,
            String address,
            Map<String, Object> settings) {}

    /** Request to update a tenant. */
    public record UpdateRequest(
            @Size(min = 2, max = 255, message = "Name must be between 2 and 255 characters")
                    String name,
            @Email(message = "Invalid email format") String contactEmail,
            @Size(max = 50) String contactPhone,
            String address,
            TenantStatus status,
            Map<String, Object> settings) {}

    /** Tenant response DTO. */
    public record Response(
            UUID id,
            String name,
            String code,
            TenantStatus status,
            String contactEmail,
            String contactPhone,
            String address,
            Map<String, Object> settings,
            Instant createdAt,
            Instant updatedAt) {}

    /** Summary response for listing tenants. */
    public record Summary(
            UUID id, String name, String code, TenantStatus status, Instant createdAt) {}
}
