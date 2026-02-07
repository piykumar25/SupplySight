package com.supplysight.common.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * JWT token provider for authentication and authorization. Handles token generation, validation,
 * and claim extraction.
 */
@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    private static final String CLAIM_TENANT_ID = "tenantId";
    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_USERNAME = "username";

    private final SecretKey secretKey;
    private final long accessTokenValidityMs;
    private final long refreshTokenValidityMs;
    private final String issuer;

    public JwtTokenProvider(
            @Value("${jwt.secret:defaultSecretKeyThatShouldBeReplacedInProduction123456}")
                    String secret,
            @Value("${jwt.access-token-validity-ms:3600000}") long accessTokenValidityMs,
            @Value("${jwt.refresh-token-validity-ms:86400000}") long refreshTokenValidityMs,
            @Value("${jwt.issuer:supplysight}") String issuer) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenValidityMs = accessTokenValidityMs;
        this.refreshTokenValidityMs = refreshTokenValidityMs;
        this.issuer = issuer;
    }

    /** Generate access token with user claims. */
    public String generateAccessToken(
            UUID userId, UUID tenantId, String username, Set<String> roles) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(accessTokenValidityMs);

        return Jwts.builder()
                .subject(userId.toString())
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .claim(CLAIM_TENANT_ID, tenantId.toString())
                .claim(CLAIM_USERNAME, username)
                .claim(CLAIM_ROLES, roles)
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    /** Generate refresh token. */
    public String generateRefreshToken(UUID userId, UUID tenantId) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(refreshTokenValidityMs);

        return Jwts.builder()
                .subject(userId.toString())
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .claim(CLAIM_TENANT_ID, tenantId.toString())
                .claim("type", "refresh")
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    /** Validate token and return claims. */
    public Optional<Claims> validateToken(String token) {
        try {
            Claims claims =
                    Jwts.parser()
                            .verifyWith(secretKey)
                            .build()
                            .parseSignedClaims(token)
                            .getPayload();
            return Optional.of(claims);
        } catch (ExpiredJwtException e) {
            log.warn("JWT token expired: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("Malformed JWT token: {}", e.getMessage());
        } catch (SecurityException e) {
            log.warn("Invalid JWT signature: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT token is empty or null: {}", e.getMessage());
        }
        return Optional.empty();
    }

    /** Extract tenant info from claims. */
    public TenantContext.TenantInfo extractTenantInfo(Claims claims) {
        UUID userId = UUID.fromString(claims.getSubject());
        UUID tenantId = UUID.fromString(claims.get(CLAIM_TENANT_ID, String.class));
        String username = claims.get(CLAIM_USERNAME, String.class);

        @SuppressWarnings("unchecked")
        List<String> rolesList = claims.get(CLAIM_ROLES, List.class);
        Set<String> roles = rolesList != null ? new HashSet<>(rolesList) : Set.of();

        return new TenantContext.TenantInfo(tenantId, userId, username, roles);
    }

    /** Extract user ID from token. */
    public Optional<UUID> getUserIdFromToken(String token) {
        return validateToken(token).map(claims -> UUID.fromString(claims.getSubject()));
    }

    /** Extract tenant ID from token. */
    public Optional<UUID> getTenantIdFromToken(String token) {
        return validateToken(token)
                .map(claims -> UUID.fromString(claims.get(CLAIM_TENANT_ID, String.class)));
    }

    /** Check if token is a refresh token. */
    public boolean isRefreshToken(String token) {
        return validateToken(token)
                .map(claims -> "refresh".equals(claims.get("type", String.class)))
                .orElse(false);
    }
}
