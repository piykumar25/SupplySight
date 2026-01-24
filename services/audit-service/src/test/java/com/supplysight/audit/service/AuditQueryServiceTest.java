package com.supplysight.audit.service;

import com.supplysight.audit.dto.AuditLogDto;
import com.supplysight.audit.entity.AuditLog;
import com.supplysight.audit.repository.AuditLogRepository;
import com.supplysight.audit.repository.AuditStatisticsRepository;
import com.supplysight.common.dto.PageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditQueryServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private AuditStatisticsRepository statisticsRepository;

    private AuditQueryService auditQueryService;

    private UUID tenantId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        auditQueryService = new AuditQueryService(auditLogRepository, statisticsRepository);
        tenantId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }

    @Test
    void getAuditLogs_shouldReturnPaginatedLogs() {
        // Given
        AuditLog log = createAuditLog();
        Page<AuditLog> page = new PageImpl<>(List.of(log), PageRequest.of(0, 20), 1);
        when(auditLogRepository.findByTenantIdOrderByEventTimeDesc(eq(tenantId), any(PageRequest.class)))
                .thenReturn(page);

        // When
        PageResponse<AuditLogDto.Response> result = auditQueryService.getAuditLogs(tenantId, 0, 20);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getAction()).isEqualTo("LOGIN_SUCCESS");
    }

    @Test
    void getAuditLogsByUser_shouldReturnUserLogs() {
        // Given
        AuditLog log = createAuditLog();
        Page<AuditLog> page = new PageImpl<>(List.of(log), PageRequest.of(0, 20), 1);
        when(auditLogRepository.findByTenantIdAndUserIdOrderByEventTimeDesc(
                eq(tenantId), eq(userId), any(PageRequest.class)))
                .thenReturn(page);

        // When
        PageResponse<AuditLogDto.Response> result = auditQueryService.getAuditLogsByUser(
                tenantId, userId, 0, 20);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getUserId()).isEqualTo(userId);
    }

    @Test
    void getAuditLogByCorrelationId_shouldReturnLog() {
        // Given
        String correlationId = UUID.randomUUID().toString();
        AuditLog log = createAuditLog();
        when(auditLogRepository.findByCorrelationId(correlationId))
                .thenReturn(Optional.of(log));

        // When
        Optional<AuditLogDto.Response> result = auditQueryService.getAuditLogByCorrelationId(correlationId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getAction()).isEqualTo("LOGIN_SUCCESS");
    }

    @Test
    void getAuditSummary_shouldReturnSummaryStats() {
        // Given
        when(auditLogRepository.countRecentByTenant(eq(tenantId), any(Instant.class)))
                .thenReturn(100L);
        when(auditLogRepository.countByActionForTenant(eq(tenantId), any(Instant.class), any(Instant.class)))
                .thenReturn(List.of(
                        new Object[]{"LOGIN_SUCCESS", 50L},
                        new Object[]{"USER_CREATED", 30L},
                        new Object[]{"SHIPMENT_CREATED", 20L}
                ));

        // When
        AuditLogDto.AuditSummary summary = auditQueryService.getAuditSummary(tenantId);

        // Then
        assertThat(summary.getTenantId()).isEqualTo(tenantId);
        assertThat(summary.getTotalEvents()).isEqualTo(100L);
        assertThat(summary.getActionCounts()).containsKeys("LOGIN_SUCCESS", "USER_CREATED", "SHIPMENT_CREATED");
    }

    @Test
    void searchAuditLogs_shouldFilterByAction() {
        // Given
        AuditLogDto.SearchRequest request = AuditLogDto.SearchRequest.builder()
                .action("LOGIN_SUCCESS")
                .page(0)
                .size(20)
                .build();

        AuditLog log = createAuditLog();
        Page<AuditLog> page = new PageImpl<>(List.of(log), PageRequest.of(0, 20), 1);
        when(auditLogRepository.searchAuditLogs(
                eq(tenantId), eq("LOGIN_SUCCESS"), isNull(), isNull(),
                any(Instant.class), any(Instant.class), any(PageRequest.class)))
                .thenReturn(page);

        // When
        PageResponse<AuditLogDto.Response> result = auditQueryService.searchAuditLogs(tenantId, request);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getAction()).isEqualTo("LOGIN_SUCCESS");
    }

    private AuditLog createAuditLog() {
        return AuditLog.builder()
                .id(1L)
                .eventId(UUID.randomUUID())
                .tenantId(tenantId)
                .userId(userId)
                .username("testuser")
                .action("LOGIN_SUCCESS")
                .resourceType("USER")
                .resourceId(userId.toString())
                .eventTime(Instant.now())
                .sourceIp("192.168.1.1")
                .details(Map.of("browser", "Chrome"))
                .correlationId(UUID.randomUUID().toString())
                .createdAt(Instant.now())
                .build();
    }
}
