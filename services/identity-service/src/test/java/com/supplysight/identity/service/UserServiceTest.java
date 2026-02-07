package com.supplysight.identity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.supplysight.common.exception.DuplicateResourceException;
import com.supplysight.common.exception.ResourceNotFoundException;
import com.supplysight.common.exception.ValidationException;
import com.supplysight.identity.dto.UserDto;
import com.supplysight.identity.entity.Tenant;
import com.supplysight.identity.entity.User;
import com.supplysight.identity.entity.User.UserStatus;
import com.supplysight.identity.repository.TenantRepository;
import com.supplysight.identity.repository.UserRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Unit tests for UserService. */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;

    @Mock private TenantRepository tenantRepository;

    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks private UserService userService;

    private UUID tenantId;
    private UUID userId;
    private Tenant testTenant;
    private User testUser;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        userId = UUID.randomUUID();

        testTenant = new Tenant(tenantId);
        testTenant.setName("Test Company");
        testTenant.setCode("test-company");

        testUser = new User(userId);
        testUser.setTenantId(tenantId);
        testUser.setEmail("test@example.com");
        testUser.setUsername("testuser");
        testUser.setPasswordHash("hashedPassword");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setRolesSet(Set.of("VIEWER"));
        testUser.setStatus(UserStatus.ACTIVE);
        testUser.setCreatedAt(Instant.now());
    }

    @Test
    @DisplayName("Should create user successfully")
    void createUser_Success() {
        // Given
        UserDto.CreateRequest request =
                new UserDto.CreateRequest(
                        "newuser@example.com",
                        "newuser",
                        "password123",
                        "New",
                        "User",
                        Set.of("OPS_USER"));
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(testTenant));
        when(userRepository.existsByTenantIdAndEmail(tenantId, "newuser@example.com"))
                .thenReturn(false);
        when(userRepository.existsByTenantIdAndUsername(tenantId, "newuser")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class)))
                .thenAnswer(
                        invocation -> {
                            User u = invocation.getArgument(0);
                            u.setCreatedAt(Instant.now());
                            return u;
                        });

        // When
        UserDto.Response response = userService.createUser(tenantId, request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.email()).isEqualTo("newuser@example.com");
        assertThat(response.username()).isEqualTo("newuser");
        assertThat(response.roles()).contains("OPS_USER");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when tenant not found")
    void createUser_TenantNotFound() {
        // Given
        UserDto.CreateRequest request =
                new UserDto.CreateRequest(
                        "newuser@example.com", "newuser", "password123", null, null, null);
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> userService.createUser(tenantId, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should throw exception for duplicate email")
    void createUser_DuplicateEmail() {
        // Given
        UserDto.CreateRequest request =
                new UserDto.CreateRequest(
                        "existing@example.com", "newuser", "password123", null, null, null);
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(testTenant));
        when(userRepository.existsByTenantIdAndEmail(tenantId, "existing@example.com"))
                .thenReturn(true);

        // When/Then
        assertThatThrownBy(() -> userService.createUser(tenantId, request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("email");
    }

    @Test
    @DisplayName("Should throw exception for invalid role")
    void createUser_InvalidRole() {
        // Given
        UserDto.CreateRequest request =
                new UserDto.CreateRequest(
                        "newuser@example.com",
                        "newuser",
                        "password123",
                        null,
                        null,
                        Set.of("INVALID_ROLE"));
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(testTenant));
        when(userRepository.existsByTenantIdAndEmail(tenantId, "newuser@example.com"))
                .thenReturn(false);
        when(userRepository.existsByTenantIdAndUsername(tenantId, "newuser")).thenReturn(false);

        // When/Then
        assertThatThrownBy(() -> userService.createUser(tenantId, request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Invalid role");
    }

    @Test
    @DisplayName("Should assign default VIEWER role when no roles specified")
    void createUser_DefaultRole() {
        // Given
        UserDto.CreateRequest request =
                new UserDto.CreateRequest(
                        "newuser@example.com", "newuser", "password123", null, null, null);
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(testTenant));
        when(userRepository.existsByTenantIdAndEmail(tenantId, "newuser@example.com"))
                .thenReturn(false);
        when(userRepository.existsByTenantIdAndUsername(tenantId, "newuser")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class)))
                .thenAnswer(
                        invocation -> {
                            User u = invocation.getArgument(0);
                            u.setCreatedAt(Instant.now());
                            return u;
                        });

        // When
        UserDto.Response response = userService.createUser(tenantId, request);

        // Then
        assertThat(response.roles()).containsExactly("VIEWER");
    }

    @Test
    @DisplayName("Should get user by ID")
    void getUser_Success() {
        // Given
        when(userRepository.findByTenantIdAndId(tenantId, userId))
                .thenReturn(Optional.of(testUser));

        // When
        UserDto.Response response = userService.getUser(tenantId, userId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(userId);
        assertThat(response.email()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("Should throw exception when user not found")
    void getUser_NotFound() {
        // Given
        UUID unknownId = UUID.randomUUID();
        when(userRepository.findByTenantIdAndId(tenantId, unknownId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> userService.getUser(tenantId, unknownId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should update user successfully")
    void updateUser_Success() {
        // Given
        UserDto.UpdateRequest request =
                new UserDto.UpdateRequest(null, null, "Updated", "Name", null, null);
        when(userRepository.findByTenantIdAndId(tenantId, userId))
                .thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        UserDto.Response response = userService.updateUser(tenantId, userId, request);

        // Then
        assertThat(response).isNotNull();
        assertThat(testUser.getFirstName()).isEqualTo("Updated");
        assertThat(testUser.getLastName()).isEqualTo("Name");
    }

    @Test
    @DisplayName("Should change password successfully")
    void changePassword_Success() {
        // Given
        UserDto.ChangePasswordRequest request =
                new UserDto.ChangePasswordRequest("oldPassword", "newPassword123");
        when(userRepository.findByTenantIdAndId(tenantId, userId))
                .thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("oldPassword", "hashedPassword")).thenReturn(true);
        when(passwordEncoder.encode("newPassword123")).thenReturn("newHashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        userService.changePassword(tenantId, userId, request);

        // Then
        assertThat(testUser.getPasswordHash()).isEqualTo("newHashedPassword");
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("Should throw exception for wrong current password")
    void changePassword_WrongCurrentPassword() {
        // Given
        UserDto.ChangePasswordRequest request =
                new UserDto.ChangePasswordRequest("wrongPassword", "newPassword123");
        when(userRepository.findByTenantIdAndId(tenantId, userId))
                .thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongPassword", "hashedPassword")).thenReturn(false);

        // When/Then
        assertThatThrownBy(() -> userService.changePassword(tenantId, userId, request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Current password is incorrect");
    }
}
