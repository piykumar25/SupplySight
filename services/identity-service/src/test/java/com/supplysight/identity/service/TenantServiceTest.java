package com.supplysight.identity.service;

import com.supplysight.common.exception.DuplicateResourceException;
import com.supplysight.common.exception.ResourceNotFoundException;
import com.supplysight.identity.dto.TenantDto;
import com.supplysight.identity.entity.Tenant;
import com.supplysight.identity.entity.Tenant.TenantStatus;
import com.supplysight.identity.repository.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TenantService.
 */
@ExtendWith(MockitoExtension.class)
class TenantServiceTest {

    @Mock
    private TenantRepository tenantRepository;

    @InjectMocks
    private TenantService tenantService;

    private Tenant testTenant;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        testTenant = new Tenant(tenantId);
        testTenant.setName("Test Company");
        testTenant.setCode("test-company");
        testTenant.setStatus(TenantStatus.ACTIVE);
        testTenant.setContactEmail("test@example.com");
        testTenant.setCreatedAt(Instant.now());
    }

    @Test
    @DisplayName("Should create tenant successfully")
    void createTenant_Success() {
        // Given
        TenantDto.CreateRequest request = new TenantDto.CreateRequest(
                "New Company", "new-company", "contact@new.com", null, null, null
        );
        when(tenantRepository.existsByCode("new-company")).thenReturn(false);
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(invocation -> {
            Tenant t = invocation.getArgument(0);
            t.setCreatedAt(Instant.now());
            return t;
        });

        // When
        TenantDto.Response response = tenantService.createTenant(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("New Company");
        assertThat(response.code()).isEqualTo("new-company");
        assertThat(response.status()).isEqualTo(TenantStatus.ACTIVE);
        verify(tenantRepository).save(any(Tenant.class));
    }

    @Test
    @DisplayName("Should throw exception when creating tenant with duplicate code")
    void createTenant_DuplicateCode() {
        // Given
        TenantDto.CreateRequest request = new TenantDto.CreateRequest(
                "Duplicate Company", "existing-code", null, null, null, null
        );
        when(tenantRepository.existsByCode("existing-code")).thenReturn(true);

        // When/Then
        assertThatThrownBy(() -> tenantService.createTenant(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("existing-code");
        verify(tenantRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should get tenant by ID")
    void getTenant_Success() {
        // Given
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(testTenant));

        // When
        TenantDto.Response response = tenantService.getTenant(tenantId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(tenantId);
        assertThat(response.name()).isEqualTo("Test Company");
    }

    @Test
    @DisplayName("Should throw exception when tenant not found")
    void getTenant_NotFound() {
        // Given
        UUID unknownId = UUID.randomUUID();
        when(tenantRepository.findById(unknownId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> tenantService.getTenant(unknownId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should update tenant successfully")
    void updateTenant_Success() {
        // Given
        TenantDto.UpdateRequest request = new TenantDto.UpdateRequest(
                "Updated Company", "updated@example.com", null, null, null, null
        );
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(testTenant));
        when(tenantRepository.save(any(Tenant.class))).thenReturn(testTenant);

        // When
        TenantDto.Response response = tenantService.updateTenant(tenantId, request);

        // Then
        assertThat(response).isNotNull();
        verify(tenantRepository).save(testTenant);
        assertThat(testTenant.getName()).isEqualTo("Updated Company");
        assertThat(testTenant.getContactEmail()).isEqualTo("updated@example.com");
    }

    @Test
    @DisplayName("Should soft delete tenant")
    void deleteTenant_Success() {
        // Given
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(testTenant));
        when(tenantRepository.save(any(Tenant.class))).thenReturn(testTenant);

        // When
        tenantService.deleteTenant(tenantId);

        // Then
        assertThat(testTenant.getStatus()).isEqualTo(TenantStatus.DELETED);
        verify(tenantRepository).save(testTenant);
    }

    @Test
    @DisplayName("Should check if tenant is active")
    void isTenantActive_True() {
        // Given
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(testTenant));

        // When
        boolean isActive = tenantService.isTenantActive(tenantId);

        // Then
        assertThat(isActive).isTrue();
    }

    @Test
    @DisplayName("Should return false for inactive tenant")
    void isTenantActive_False() {
        // Given
        testTenant.setStatus(TenantStatus.SUSPENDED);
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(testTenant));

        // When
        boolean isActive = tenantService.isTenantActive(tenantId);

        // Then
        assertThat(isActive).isFalse();
    }
}
