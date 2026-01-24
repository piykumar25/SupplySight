package com.supplysight.identity.service;

import com.supplysight.common.exception.DuplicateResourceException;
import com.supplysight.common.exception.ForbiddenException;
import com.supplysight.common.exception.ResourceNotFoundException;
import com.supplysight.common.exception.ValidationException;
import com.supplysight.common.security.TenantContext;
import com.supplysight.identity.dto.UserDto;
import com.supplysight.identity.entity.Tenant;
import com.supplysight.identity.entity.User;
import com.supplysight.identity.entity.User.UserStatus;
import com.supplysight.identity.repository.TenantRepository;
import com.supplysight.identity.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

/**
 * Service for user management operations.
 * All operations are tenant-scoped for multi-tenant isolation.
 */
@Service
@Transactional(readOnly = true)
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private static final Set<String> VALID_ROLES = Set.of("ADMIN", "OPS_USER", "VIEWER");

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, TenantRepository tenantRepository, 
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Create a new user within a tenant.
     */
    @Transactional
    public UserDto.Response createUser(UUID tenantId, UserDto.CreateRequest request) {
        log.info("Creating user {} for tenant {}", request.email(), tenantId);

        // Validate tenant exists
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", tenantId));

        // Check for duplicates
        if (userRepository.existsByTenantIdAndEmail(tenantId, request.email())) {
            throw new DuplicateResourceException("User with email", request.email());
        }
        if (userRepository.existsByTenantIdAndUsername(tenantId, request.username())) {
            throw new DuplicateResourceException("User with username", request.username());
        }

        // Validate and normalize roles
        Set<String> roles = validateAndNormalizeRoles(request.roles());

        User user = new User();
        user.setTenantId(tenantId);
        user.setEmail(request.email().toLowerCase());
        user.setUsername(request.username());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setRolesSet(roles);
        user.setStatus(UserStatus.ACTIVE);

        user = userRepository.save(user);
        log.info("Created user {} with id {} for tenant {}", user.getEmail(), user.getId(), tenantId);

        return toResponse(user);
    }

    /**
     * Get user by ID (tenant-scoped).
     */
    public UserDto.Response getUser(UUID tenantId, UUID userId) {
        User user = findUserOrThrow(tenantId, userId);
        return toResponse(user);
    }

    /**
     * Get current user info.
     */
    public UserDto.MeResponse getCurrentUser(UUID userId, UUID tenantId) {
        User user = userRepository.findByTenantIdAndId(tenantId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", tenantId));

        return new UserDto.MeResponse(
                user.getId(),
                user.getTenantId(),
                tenant.getName(),
                tenant.getCode(),
                user.getEmail(),
                user.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                user.getFullName(),
                user.getRolesSet()
        );
    }

    /**
     * Update user (tenant-scoped).
     */
    @Transactional
    public UserDto.Response updateUser(UUID tenantId, UUID userId, UserDto.UpdateRequest request) {
        log.info("Updating user {} for tenant {}", userId, tenantId);

        User user = findUserOrThrow(tenantId, userId);

        if (request.email() != null && !request.email().equalsIgnoreCase(user.getEmail())) {
            if (userRepository.existsByTenantIdAndEmail(tenantId, request.email())) {
                throw new DuplicateResourceException("User with email", request.email());
            }
            user.setEmail(request.email().toLowerCase());
        }

        if (request.username() != null && !request.username().equals(user.getUsername())) {
            if (userRepository.existsByTenantIdAndUsername(tenantId, request.username())) {
                throw new DuplicateResourceException("User with username", request.username());
            }
            user.setUsername(request.username());
        }

        if (request.firstName() != null) {
            user.setFirstName(request.firstName());
        }
        if (request.lastName() != null) {
            user.setLastName(request.lastName());
        }
        if (request.roles() != null) {
            user.setRolesSet(validateAndNormalizeRoles(request.roles()));
        }
        if (request.status() != null) {
            user.setStatus(request.status());
        }

        user = userRepository.save(user);
        log.info("Updated user {} for tenant {}", userId, tenantId);

        return toResponse(user);
    }

    /**
     * Change user password.
     */
    @Transactional
    public void changePassword(UUID tenantId, UUID userId, UserDto.ChangePasswordRequest request) {
        log.info("Changing password for user {}", userId);

        User user = findUserOrThrow(tenantId, userId);

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new ValidationException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        log.info("Password changed for user {}", userId);
    }

    /**
     * Delete user (soft delete).
     */
    @Transactional
    public void deleteUser(UUID tenantId, UUID userId) {
        log.info("Deleting user {} for tenant {}", userId, tenantId);

        User user = findUserOrThrow(tenantId, userId);
        
        // Prevent self-deletion
        UUID currentUserId = TenantContext.getUserId();
        if (userId.equals(currentUserId)) {
            throw new ForbiddenException("Cannot delete your own account");
        }

        user.setStatus(UserStatus.DELETED);
        userRepository.save(user);

        log.info("Deleted user {} for tenant {}", userId, tenantId);
    }

    /**
     * List users for a tenant.
     */
    public Page<UserDto.Summary> listUsers(UUID tenantId, Pageable pageable) {
        return userRepository.findAllByTenantId(tenantId, pageable)
                .map(this::toSummary);
    }

    /**
     * List users by status.
     */
    public Page<UserDto.Summary> listUsersByStatus(UUID tenantId, UserStatus status, Pageable pageable) {
        return userRepository.findByTenantIdAndStatus(tenantId, status, pageable)
                .map(this::toSummary);
    }

    // Helper methods

    private User findUserOrThrow(UUID tenantId, UUID userId) {
        return userRepository.findByTenantIdAndId(tenantId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }

    private Set<String> validateAndNormalizeRoles(Set<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return Set.of("VIEWER");
        }

        Set<String> normalizedRoles = new java.util.HashSet<>();
        for (String role : roles) {
            String upperRole = role.toUpperCase();
            if (!VALID_ROLES.contains(upperRole)) {
                throw new ValidationException("Invalid role: " + role + ". Valid roles are: " + VALID_ROLES);
            }
            normalizedRoles.add(upperRole);
        }
        return normalizedRoles;
    }

    private UserDto.Response toResponse(User user) {
        return new UserDto.Response(
                user.getId(),
                user.getTenantId(),
                user.getEmail(),
                user.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                user.getFullName(),
                user.getRolesSet(),
                user.getStatus(),
                user.getLastLoginAt(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    private UserDto.Summary toSummary(User user) {
        return new UserDto.Summary(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                user.getFullName(),
                user.getRolesSet(),
                user.getStatus()
        );
    }
}
