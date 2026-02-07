package com.supplysight.identity.service;

import com.supplysight.common.exception.DuplicateResourceException;
import com.supplysight.common.exception.ResourceNotFoundException;
import com.supplysight.identity.dto.TenantDto;
import com.supplysight.identity.entity.Tenant;
import com.supplysight.identity.entity.Tenant.TenantStatus;
import com.supplysight.identity.repository.TenantRepository;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Service for tenant management operations. */
@Service
@Transactional(readOnly = true)
public class TenantService {

    private static final Logger log = LoggerFactory.getLogger(TenantService.class);

    private final TenantRepository tenantRepository;

    public TenantService(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    /** Create a new tenant. */
    @Transactional
    public TenantDto.Response createTenant(TenantDto.CreateRequest request) {
        log.info("Creating tenant with code: {}", request.code());

        if (tenantRepository.existsByCode(request.code())) {
            throw new DuplicateResourceException("Tenant", request.code());
        }

        Tenant tenant = new Tenant();
        tenant.setName(request.name());
        tenant.setCode(request.code().toLowerCase());
        tenant.setContactEmail(request.contactEmail());
        tenant.setContactPhone(request.contactPhone());
        tenant.setAddress(request.address());
        tenant.setSettings(request.settings());
        tenant.setStatus(TenantStatus.ACTIVE);

        tenant = tenantRepository.save(tenant);
        log.info("Created tenant: {} with id: {}", tenant.getCode(), tenant.getId());

        return toResponse(tenant);
    }

    /** Get tenant by ID. */
    public TenantDto.Response getTenant(UUID tenantId) {
        Tenant tenant = findTenantOrThrow(tenantId);
        return toResponse(tenant);
    }

    /** Get tenant by code. */
    public TenantDto.Response getTenantByCode(String code) {
        Tenant tenant =
                tenantRepository
                        .findByCode(code.toLowerCase())
                        .orElseThrow(() -> new ResourceNotFoundException("Tenant", code));
        return toResponse(tenant);
    }

    /** Update tenant. */
    @Transactional
    public TenantDto.Response updateTenant(UUID tenantId, TenantDto.UpdateRequest request) {
        log.info("Updating tenant: {}", tenantId);

        Tenant tenant = findTenantOrThrow(tenantId);

        if (request.name() != null) {
            tenant.setName(request.name());
        }
        if (request.contactEmail() != null) {
            tenant.setContactEmail(request.contactEmail());
        }
        if (request.contactPhone() != null) {
            tenant.setContactPhone(request.contactPhone());
        }
        if (request.address() != null) {
            tenant.setAddress(request.address());
        }
        if (request.status() != null) {
            tenant.setStatus(request.status());
        }
        if (request.settings() != null) {
            tenant.setSettings(request.settings());
        }

        tenant = tenantRepository.save(tenant);
        log.info("Updated tenant: {}", tenantId);

        return toResponse(tenant);
    }

    /** Soft delete tenant. */
    @Transactional
    public void deleteTenant(UUID tenantId) {
        log.info("Deleting tenant: {}", tenantId);

        Tenant tenant = findTenantOrThrow(tenantId);
        tenant.setStatus(TenantStatus.DELETED);
        tenantRepository.save(tenant);

        log.info("Deleted tenant: {}", tenantId);
    }

    /** List all active tenants. */
    public Page<TenantDto.Summary> listTenants(Pageable pageable) {
        return tenantRepository.findAllActive(pageable).map(this::toSummary);
    }

    /** List tenants by status. */
    public Page<TenantDto.Summary> listTenantsByStatus(TenantStatus status, Pageable pageable) {
        return tenantRepository.findByStatus(status, pageable).map(this::toSummary);
    }

    /** Check if tenant exists and is active. */
    public boolean isTenantActive(UUID tenantId) {
        return tenantRepository
                .findById(tenantId)
                .map(t -> t.getStatus() == TenantStatus.ACTIVE)
                .orElse(false);
    }

    // Helper methods

    private Tenant findTenantOrThrow(UUID tenantId) {
        return tenantRepository
                .findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", tenantId));
    }

    private TenantDto.Response toResponse(Tenant tenant) {
        return new TenantDto.Response(
                tenant.getId(),
                tenant.getName(),
                tenant.getCode(),
                tenant.getStatus(),
                tenant.getContactEmail(),
                tenant.getContactPhone(),
                tenant.getAddress(),
                tenant.getSettings(),
                tenant.getCreatedAt(),
                tenant.getUpdatedAt());
    }

    private TenantDto.Summary toSummary(Tenant tenant) {
        return new TenantDto.Summary(
                tenant.getId(),
                tenant.getName(),
                tenant.getCode(),
                tenant.getStatus(),
                tenant.getCreatedAt());
    }
}
