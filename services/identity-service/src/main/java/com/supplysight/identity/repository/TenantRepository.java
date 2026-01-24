package com.supplysight.identity.repository;

import com.supplysight.identity.entity.Tenant;
import com.supplysight.identity.entity.Tenant.TenantStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Tenant entity operations.
 */
@Repository
public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    Optional<Tenant> findByCode(String code);

    boolean existsByCode(String code);

    @Query("SELECT t FROM Tenant t WHERE t.status = :status")
    Page<Tenant> findByStatus(@Param("status") TenantStatus status, Pageable pageable);

    @Query("SELECT t FROM Tenant t WHERE t.status != 'DELETED'")
    Page<Tenant> findAllActive(Pageable pageable);

    @Query("SELECT COUNT(t) FROM Tenant t WHERE t.status = :status")
    long countByStatus(@Param("status") TenantStatus status);
}
