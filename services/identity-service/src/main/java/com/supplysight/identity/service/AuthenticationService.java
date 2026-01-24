package com.supplysight.identity.service;

import com.supplysight.common.exception.UnauthorizedException;
import com.supplysight.common.exception.ValidationException;
import com.supplysight.common.security.JwtTokenProvider;
import com.supplysight.identity.dto.AuthDto;
import com.supplysight.identity.entity.RefreshToken;
import com.supplysight.identity.entity.Tenant;
import com.supplysight.identity.entity.User;
import com.supplysight.identity.repository.RefreshTokenRepository;
import com.supplysight.identity.repository.TenantRepository;
import com.supplysight.identity.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/**
 * Service for authentication operations.
 */
@Service
@Transactional(readOnly = true)
public class AuthenticationService {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class);
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(30);

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final long accessTokenValidityMs;
    private final long refreshTokenValidityMs;

    public AuthenticationService(
            UserRepository userRepository,
            TenantRepository tenantRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider,
            @Value("${jwt.access-token-validity-ms:3600000}") long accessTokenValidityMs,
            @Value("${jwt.refresh-token-validity-ms:86400000}") long refreshTokenValidityMs) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.accessTokenValidityMs = accessTokenValidityMs;
        this.refreshTokenValidityMs = refreshTokenValidityMs;
    }

    /**
     * Register a new user.
     */
    @Transactional
    public void register(AuthDto.RegisterRequest request) {
        log.info("Registration attempt for email: {}", request.email());

        if (userRepository.existsByEmail(request.email())) {
            throw new ValidationException("Email already registered");
        }

        // Create or find tenant (simplification for demo: creation)
        Tenant tenant = new Tenant();
        tenant.setName(request.tenantName() != null ? request.tenantName() : "Demo Tenant");
        tenant.setCode(request.tenantName() != null ? request.tenantName().toLowerCase().replaceAll("\\s+", "-")
                : "demo-tenant-" + UUID.randomUUID().toString().substring(0, 8));
        tenant.setStatus(Tenant.TenantStatus.ACTIVE);
        tenant = tenantRepository.save(tenant);

        User user = new User();
        user.setTenantId(tenant.getId());
        user.setEmail(request.email().toLowerCase());
        user.setUsername(request.username());
        String[] names = request.fullName().trim().split(" ", 2);
        user.setFirstName(names[0]);
        if (names.length > 1) {
            user.setLastName(names[1]);
        }
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setStatus(User.UserStatus.ACTIVE);
        user.setCreatedAt(Instant.now());
        user.setRoles("USER"); // Default role

        userRepository.save(user);
        log.info("User registered successfully: {}", user.getEmail());
    }

    /**
     * Authenticate user with email and password.
     */
    @Transactional
    public AuthDto.LoginResponse login(AuthDto.LoginRequest request, String ipAddress, String userAgent) {
        log.info("Login attempt for email: {}", request.email());

        // Find user by email
        User user = userRepository.findByEmailForAuth(request.email().toLowerCase())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        // Check if tenant is active
        Tenant tenant = tenantRepository.findById(user.getTenantId())
                .orElseThrow(() -> new UnauthorizedException("Tenant not found"));

        if (tenant.getStatus() != Tenant.TenantStatus.ACTIVE) {
            throw new UnauthorizedException("Tenant is not active");
        }

        // Check if account is locked
        if (user.isLocked()) {
            log.warn("Login attempt for locked account: {}", request.email());
            throw new UnauthorizedException("Account is temporarily locked. Please try again later.");
        }

        // Verify password
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            handleFailedLogin(user);
            throw new UnauthorizedException("Invalid credentials");
        }

        // Update login success
        user.resetFailedAttempts();
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        // Generate tokens
        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getId(),
                user.getTenantId(),
                user.getUsername(),
                user.getRolesSet());

        String refreshToken = createRefreshToken(user, ipAddress, userAgent);

        log.info("Login successful for user: {} (tenant: {})", user.getEmail(), tenant.getCode());

        return AuthDto.LoginResponse.of(
                accessToken,
                refreshToken,
                accessTokenValidityMs / 1000,
                new AuthDto.UserInfo(
                        user.getId(),
                        user.getTenantId(),
                        tenant.getCode(),
                        user.getEmail(),
                        user.getUsername(),
                        user.getFullName(),
                        user.getRolesSet()));
    }

    /**
     * Refresh access token using refresh token.
     */
    @Transactional
    public AuthDto.RefreshTokenResponse refreshToken(AuthDto.RefreshTokenRequest request) {
        String tokenHash = hashToken(request.refreshToken());

        RefreshToken refreshToken = refreshTokenRepository
                .findValidToken(tokenHash, Instant.now())
                .orElseThrow(() -> new UnauthorizedException("Invalid or expired refresh token"));

        User user = userRepository.findById(refreshToken.getUserId())
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        if (user.getStatus() != User.UserStatus.ACTIVE) {
            throw new UnauthorizedException("User account is not active");
        }

        // Generate new access token
        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getId(),
                user.getTenantId(),
                user.getUsername(),
                user.getRolesSet());

        log.info("Token refreshed for user: {}", user.getEmail());

        return AuthDto.RefreshTokenResponse.of(accessToken, accessTokenValidityMs / 1000);
    }

    /**
     * Logout user by revoking refresh token.
     */
    @Transactional
    public void logout(AuthDto.LogoutRequest request) {
        if (request.refreshToken() != null && !request.refreshToken().isBlank()) {
            String tokenHash = hashToken(request.refreshToken());
            refreshTokenRepository.revokeByTokenHash(tokenHash, Instant.now());
            log.info("Refresh token revoked");
        }
    }

    /**
     * Logout from all sessions.
     */
    @Transactional
    public void logoutAll(UUID userId) {
        refreshTokenRepository.revokeAllByUserId(userId, Instant.now());
        log.info("All sessions revoked for user: {}", userId);
    }

    // Helper methods

    private void handleFailedLogin(User user) {
        user.incrementFailedAttempts();

        if (user.getFailedLoginAttempts() >= MAX_FAILED_ATTEMPTS) {
            user.lockAccount(Instant.now().plus(LOCK_DURATION));
            log.warn("Account locked due to too many failed attempts: {}", user.getEmail());
        }

        userRepository.save(user);
    }

    private String createRefreshToken(User user, String ipAddress, String userAgent) {
        // Generate random token
        String token = UUID.randomUUID().toString() + "-" + UUID.randomUUID().toString();
        String tokenHash = hashToken(token);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUserId(user.getId());
        refreshToken.setTokenHash(tokenHash);
        refreshToken.setExpiresAt(Instant.now().plusMillis(refreshTokenValidityMs));
        refreshToken.setIpAddress(ipAddress);
        refreshToken.setDeviceInfo(truncateString(userAgent, 500));

        refreshTokenRepository.save(refreshToken);

        return token;
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    private String truncateString(String str, int maxLength) {
        if (str == null)
            return null;
        return str.length() > maxLength ? str.substring(0, maxLength) : str;
    }
}
