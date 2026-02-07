package com.supplysight.identity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/** DTOs for authentication operations. */
public final class AuthDto {

    private AuthDto() {}

    /** Login request with email and password. */
    public record LoginRequest(
            @NotBlank(message = "Email is required") @Email(message = "Invalid email format")
                    String email,
            @NotBlank(message = "Password is required") String password) {}

    /** Registration request. */
    public record RegisterRequest(
            @NotBlank(message = "Email is required") @Email(message = "Invalid email format")
                    String email,
            @NotBlank(message = "Username is required") String username,
            @NotBlank(message = "Password is required") String password,
            @NotBlank(message = "Full name is required") String fullName,
            String tenantName) {}

    /** Login response with tokens. */
    public record LoginResponse(
            String accessToken,
            String refreshToken,
            String tokenType,
            long expiresIn,
            UserInfo user) {
        public static LoginResponse of(
                String accessToken, String refreshToken, long expiresIn, UserInfo user) {
            return new LoginResponse(accessToken, refreshToken, "Bearer", expiresIn, user);
        }
    }

    /** Refresh token request. */
    public record RefreshTokenRequest(
            @NotBlank(message = "Refresh token is required") String refreshToken) {}

    /** Token refresh response. */
    public record RefreshTokenResponse(String accessToken, String tokenType, long expiresIn) {
        public static RefreshTokenResponse of(String accessToken, long expiresIn) {
            return new RefreshTokenResponse(accessToken, "Bearer", expiresIn);
        }
    }

    /** Logout request. */
    public record LogoutRequest(String refreshToken) {}

    /** User info included in login response. */
    public record UserInfo(
            UUID id,
            UUID tenantId,
            String tenantCode,
            String email,
            String username,
            String fullName,
            Set<String> roles) {}

    /** Token claims for JWT validation. */
    public record TokenClaims(
            UUID userId,
            UUID tenantId,
            String username,
            Set<String> roles,
            Instant issuedAt,
            Instant expiresAt) {}
}
