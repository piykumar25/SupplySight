package com.supplysight.identity.controller;

import com.supplysight.common.dto.ApiResponse;
import com.supplysight.common.security.TenantContext;
import com.supplysight.identity.dto.AuthDto;
import com.supplysight.identity.dto.UserDto;
import com.supplysight.identity.service.AuthenticationService;
import com.supplysight.identity.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Authentication controller for login, token refresh, and logout. */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Authentication", description = "Authentication and session management")
public class AuthController {

    private final AuthenticationService authenticationService;
    private final UserService userService;

    public AuthController(AuthenticationService authenticationService, UserService userService) {
        this.authenticationService = authenticationService;
        this.userService = userService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register", description = "Register a new user and tenant")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "Registration successful"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "400",
                        description = "Invalid input or email already exists")
            })
    public ResponseEntity<ApiResponse<Void>> register(
            @Valid @RequestBody AuthDto.RegisterRequest request) {
        authenticationService.register(request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/login")
    @Operation(
            summary = "Login",
            description = "Authenticate with email and password to receive JWT tokens")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "Login successful",
                        content =
                                @Content(
                                        schema =
                                                @Schema(
                                                        implementation =
                                                                AuthDto.LoginResponse.class))),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "401",
                        description = "Invalid credentials")
            })
    public ResponseEntity<ApiResponse<AuthDto.LoginResponse>> login(
            @Valid @RequestBody AuthDto.LoginRequest request, HttpServletRequest httpRequest) {
        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        AuthDto.LoginResponse response = authenticationService.login(request, ipAddress, userAgent);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/refresh")
    @Operation(
            summary = "Refresh Token",
            description = "Get a new access token using refresh token")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "Token refreshed successfully"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "401",
                        description = "Invalid or expired refresh token")
            })
    public ResponseEntity<ApiResponse<AuthDto.RefreshTokenResponse>> refreshToken(
            @Valid @RequestBody AuthDto.RefreshTokenRequest request) {
        AuthDto.RefreshTokenResponse response = authenticationService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout", description = "Revoke refresh token and end session")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestBody(required = false) AuthDto.LogoutRequest request) {
        authenticationService.logout(request != null ? request : new AuthDto.LogoutRequest(null));
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/logout/all")
    @Operation(
            summary = "Logout All Sessions",
            description = "Revoke all refresh tokens for the current user")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> logoutAll() {
        TenantContext.TenantInfo tenantInfo = TenantContext.get();
        authenticationService.logoutAll(tenantInfo.userId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/me")
    @Operation(
            summary = "Get Current User",
            description = "Get information about the currently authenticated user")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserDto.MeResponse>> getCurrentUser() {
        TenantContext.TenantInfo tenantInfo = TenantContext.get();
        UserDto.MeResponse response =
                userService.getCurrentUser(tenantInfo.userId(), tenantInfo.tenantId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
}
