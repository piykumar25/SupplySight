package com.supplysight.identity.repository;

import com.supplysight.identity.entity.TenantQuota;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Repository for TenantQuota entity. */
@Repository
public interface TenantQuotaRepository extends JpaRepository<TenantQuota, UUID> {

    /** Find quota by tenant ID. */
    Optional<TenantQuota> findByTenantId(UUID tenantId);

    /** Check if quota exists for tenant. */
    boolean existsByTenantId(UUID tenantId);

    /** Delete quota by tenant ID. */
    void deleteByTenantId(UUID tenantId);
}
