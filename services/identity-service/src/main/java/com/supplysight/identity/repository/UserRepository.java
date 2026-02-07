package com.supplysight.identity.repository;

import com.supplysight.identity.entity.User;
import com.supplysight.identity.entity.User.UserStatus;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for User entity operations. All queries are tenant-scoped for multi-tenant isolation.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    // Tenant-scoped queries
    @Query("SELECT u FROM User u WHERE u.tenantId = :tenantId AND u.email = :email")
    Optional<User> findByTenantIdAndEmail(
            @Param("tenantId") UUID tenantId, @Param("email") String email);

    @Query("SELECT u FROM User u WHERE u.tenantId = :tenantId AND u.username = :username")
    Optional<User> findByTenantIdAndUsername(
            @Param("tenantId") UUID tenantId, @Param("username") String username);

    @Query("SELECT u FROM User u WHERE u.tenantId = :tenantId AND u.id = :userId")
    Optional<User> findByTenantIdAndId(
            @Param("tenantId") UUID tenantId, @Param("userId") UUID userId);

    @Query("SELECT u FROM User u WHERE u.tenantId = :tenantId AND u.status != 'DELETED'")
    Page<User> findAllByTenantId(@Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.tenantId = :tenantId AND u.status = :status")
    Page<User> findByTenantIdAndStatus(
            @Param("tenantId") UUID tenantId,
            @Param("status") UserStatus status,
            Pageable pageable);

    // Existence checks
    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.tenantId = :tenantId AND u.email = :email")
    boolean existsByTenantIdAndEmail(
            @Param("tenantId") UUID tenantId, @Param("email") String email);

    @Query(
            "SELECT COUNT(u) > 0 FROM User u WHERE u.tenantId = :tenantId AND u.username = :username")
    boolean existsByTenantIdAndUsername(
            @Param("tenantId") UUID tenantId, @Param("username") String username);

    // Count queries
    @Query("SELECT COUNT(u) FROM User u WHERE u.tenantId = :tenantId AND u.status != 'DELETED'")
    long countByTenantId(@Param("tenantId") UUID tenantId);

    @Query("SELECT COUNT(u) FROM User u WHERE u.tenantId = :tenantId AND u.status = :status")
    long countByTenantIdAndStatus(
            @Param("tenantId") UUID tenantId, @Param("status") UserStatus status);

    // Update queries
    @Modifying
    @Query(
            "UPDATE User u SET u.lastLoginAt = :loginTime, u.failedLoginAttempts = 0, u.lockedUntil = NULL WHERE u.id = :userId")
    void updateLoginSuccess(@Param("userId") UUID userId, @Param("loginTime") Instant loginTime);

    @Modifying
    @Query(
            "UPDATE User u SET u.failedLoginAttempts = u.failedLoginAttempts + 1 WHERE u.id = :userId")
    void incrementFailedAttempts(@Param("userId") UUID userId);

    @Modifying
    @Query("UPDATE User u SET u.lockedUntil = :lockUntil WHERE u.id = :userId")
    void lockUser(@Param("userId") UUID userId, @Param("lockUntil") Instant lockUntil);

    // Global query for authentication (used before tenant context is established)
    @Query("SELECT u FROM User u WHERE u.email = :email AND u.status = 'ACTIVE'")
    Optional<User> findByEmailForAuth(@Param("email") String email);

    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.email = :email")
    boolean existsByEmail(@Param("email") String email);
}
