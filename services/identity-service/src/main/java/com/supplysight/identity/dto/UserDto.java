package com.supplysight.identity.dto;

import com.supplysight.identity.entity.User.UserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * DTOs for User operations.
 */
public final class UserDto {

    private UserDto() {}

    /**
     * Request to create a new user.
     */
    public record CreateRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 100, message = "Username must be between 3 and 100 characters")
        String username,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
        String password,

        @Size(max = 100)
        String firstName,

        @Size(max = 100)
        String lastName,

        Set<String> roles
    ) {}

    /**
     * Request to update a user.
     */
    public record UpdateRequest(
        @Email(message = "Invalid email format")
        String email,

        @Size(min = 3, max = 100, message = "Username must be between 3 and 100 characters")
        String username,

        @Size(max = 100)
        String firstName,

        @Size(max = 100)
        String lastName,

        Set<String> roles,

        UserStatus status
    ) {}

    /**
     * Request to change password.
     */
    public record ChangePasswordRequest(
        @NotBlank(message = "Current password is required")
        String currentPassword,

        @NotBlank(message = "New password is required")
        @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
        String newPassword
    ) {}

    /**
     * User response DTO.
     */
    public record Response(
        UUID id,
        UUID tenantId,
        String email,
        String username,
        String firstName,
        String lastName,
        String fullName,
        Set<String> roles,
        UserStatus status,
        Instant lastLoginAt,
        Instant createdAt,
        Instant updatedAt
    ) {}

    /**
     * Summary response for listing users.
     */
    public record Summary(
        UUID id,
        String email,
        String username,
        String fullName,
        Set<String> roles,
        UserStatus status
    ) {}

    /**
     * Current user info (for /me endpoint).
     */
    public record MeResponse(
        UUID id,
        UUID tenantId,
        String tenantName,
        String tenantCode,
        String email,
        String username,
        String firstName,
        String lastName,
        String fullName,
        Set<String> roles
    ) {}
}
