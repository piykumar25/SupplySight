package com.supplysight.identity.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.supplysight.identity.dto.AuthDto;
import com.supplysight.identity.dto.TenantDto;
import com.supplysight.identity.dto.UserDto;
import com.supplysight.identity.entity.Tenant;
import com.supplysight.identity.entity.User;
import com.supplysight.identity.repository.TenantRepository;
import com.supplysight.identity.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for authentication endpoints using Testcontainers.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@Transactional
class AuthenticationIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("supplysight_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${jwt.secret}")
    private String jwtSecret;

    private Tenant testTenant;
    private User testUser;

    @BeforeEach
    void setUp() {
        // Create test tenant
        testTenant = new Tenant();
        testTenant.setName("Integration Test Company");
        testTenant.setCode("int-test-" + System.currentTimeMillis());
        testTenant.setContactEmail("test@integration.com");
        testTenant = tenantRepository.save(testTenant);

        // Create test user
        testUser = new User();
        testUser.setTenantId(testTenant.getId());
        testUser.setEmail("user@integration.com");
        testUser.setUsername("intuser");
        testUser.setPasswordHash(passwordEncoder.encode("password123"));
        testUser.setFirstName("Integration");
        testUser.setLastName("User");
        testUser.setRolesSet(Set.of("ADMIN"));
        testUser = userRepository.save(testUser);
    }

    @Test
    @DisplayName("Should login successfully and return valid JWT")
    void login_Success() throws Exception {
        AuthDto.LoginRequest request = new AuthDto.LoginRequest(
                "user@integration.com", "password123"
        );

        MvcResult result = mockMvc.perform(post("/api/v1/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.user.email").value("user@integration.com"))
                .andExpect(jsonPath("$.data.user.tenantId").value(testTenant.getId().toString()))
                .andReturn();

        // Parse response and verify JWT claims
        String responseBody = result.getResponse().getContentAsString();
        var response = objectMapper.readTree(responseBody);
        String accessToken = response.get("data").get("accessToken").asText();

        // Decode and verify JWT
        Claims claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(accessToken)
                .getPayload();

        assertThat(claims.getSubject()).isEqualTo(testUser.getId().toString());
        assertThat(claims.get("tenantId", String.class)).isEqualTo(testTenant.getId().toString());
        assertThat(claims.get("username", String.class)).isEqualTo("intuser");
    }

    @Test
    @DisplayName("Should reject login with wrong password")
    void login_WrongPassword() throws Exception {
        AuthDto.LoginRequest request = new AuthDto.LoginRequest(
                "user@integration.com", "wrongpassword"
        );

        mockMvc.perform(post("/api/v1/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("Should reject login with non-existent email")
    void login_NonExistentEmail() throws Exception {
        AuthDto.LoginRequest request = new AuthDto.LoginRequest(
                "nonexistent@example.com", "password123"
        );

        mockMvc.perform(post("/api/v1/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Should get current user info with valid token")
    void getCurrentUser_Success() throws Exception {
        // First login to get token
        String accessToken = loginAndGetToken();

        mockMvc.perform(get("/api/v1/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("user@integration.com"))
                .andExpect(jsonPath("$.data.tenantId").value(testTenant.getId().toString()))
                .andExpect(jsonPath("$.data.tenantCode").value(testTenant.getCode()));
    }

    @Test
    @DisplayName("Should reject request without token")
    void getCurrentUser_NoToken() throws Exception {
        mockMvc.perform(get("/api/v1/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should reject request with invalid token")
    void getCurrentUser_InvalidToken() throws Exception {
        mockMvc.perform(get("/api/v1/me")
                        .header("Authorization", "Bearer invalid.token.here"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should refresh token successfully")
    void refreshToken_Success() throws Exception {
        // First login to get tokens
        AuthDto.LoginRequest loginRequest = new AuthDto.LoginRequest(
                "user@integration.com", "password123"
        );

        MvcResult loginResult = mockMvc.perform(post("/api/v1/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        var loginResponse = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String refreshToken = loginResponse.get("data").get("refreshToken").asText();

        // Refresh token
        AuthDto.RefreshTokenRequest refreshRequest = new AuthDto.RefreshTokenRequest(refreshToken);

        mockMvc.perform(post("/api/v1/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"));
    }

    @Test
    @DisplayName("Should logout successfully")
    void logout_Success() throws Exception {
        // Login first
        AuthDto.LoginRequest loginRequest = new AuthDto.LoginRequest(
                "user@integration.com", "password123"
        );

        MvcResult loginResult = mockMvc.perform(post("/api/v1/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        var loginResponse = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String accessToken = loginResponse.get("data").get("accessToken").asText();
        String refreshToken = loginResponse.get("data").get("refreshToken").asText();

        // Logout
        AuthDto.LogoutRequest logoutRequest = new AuthDto.LogoutRequest(refreshToken);

        mockMvc.perform(post("/api/v1/logout")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logoutRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Verify refresh token is revoked
        AuthDto.RefreshTokenRequest refreshRequest = new AuthDto.RefreshTokenRequest(refreshToken);

        mockMvc.perform(post("/api/v1/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isUnauthorized());
    }

    private String loginAndGetToken() throws Exception {
        AuthDto.LoginRequest request = new AuthDto.LoginRequest(
                "user@integration.com", "password123"
        );

        MvcResult result = mockMvc.perform(post("/api/v1/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        var response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.get("data").get("accessToken").asText();
    }
}
