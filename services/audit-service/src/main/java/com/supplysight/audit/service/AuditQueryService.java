package com.supplysight.audit.service;

import com.supplysight.audit.dto.AuditLogDto;
import com.supplysight.audit.entity.AuditLog;
import com.supplysight.audit.repository.AuditLogRepository;
import com.supplysight.audit.repository.AuditStatisticsRepository;
import com.supplysight.common.dto.PageResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Service for querying audit logs and generating reports. */
@Service
@Transactional(readOnly = true)
public class AuditQueryService {

    private final AuditLogRepository auditLogRepository;
    private final AuditStatisticsRepository statisticsRepository;

    public AuditQueryService(
            AuditLogRepository auditLogRepository, AuditStatisticsRepository statisticsRepository) {
        this.auditLogRepository = auditLogRepository;
        this.statisticsRepository = statisticsRepository;
    }

    /** Get paginated audit logs for a tenant. */
    public PageResponse<AuditLogDto.Response> getAuditLogs(UUID tenantId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<AuditLog> logs =
                auditLogRepository.findByTenantIdOrderByEventTimeDesc(tenantId, pageable);
        return mapToPageResponse(logs);
    }

    /** Search audit logs with filters. */
    public PageResponse<AuditLogDto.Response> searchAuditLogs(
            UUID tenantId, AuditLogDto.SearchRequest request) {

        Instant startTime =
                request.getStartTime() != null
                        ? request.getStartTime()
                        : Instant.now().minus(30, ChronoUnit.DAYS);
        Instant endTime = request.getEndTime() != null ? request.getEndTime() : Instant.now();

        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());

        Page<AuditLog> logs =
                auditLogRepository.searchAuditLogs(
                        tenantId,
                        request.getAction(),
                        request.getUserId(),
                        request.getResourceType(),
                        startTime,
                        endTime,
                        pageable);

        return mapToPageResponse(logs);
    }

    /** Get audit logs for a specific user. */
    public PageResponse<AuditLogDto.Response> getAuditLogsByUser(
            UUID tenantId, UUID userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<AuditLog> logs =
                auditLogRepository.findByTenantIdAndUserIdOrderByEventTimeDesc(
                        tenantId, userId, pageable);
        return mapToPageResponse(logs);
    }

    /** Get audit logs for a specific resource. */
    public PageResponse<AuditLogDto.Response> getAuditLogsByResource(
            UUID tenantId, String resourceType, String resourceId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<AuditLog> logs =
                auditLogRepository.findByTenantIdAndResourceTypeAndResourceIdOrderByEventTimeDesc(
                        tenantId, resourceType, resourceId, pageable);
        return mapToPageResponse(logs);
    }

    /** Get audit log by correlation ID. */
    public Optional<AuditLogDto.Response> getAuditLogByCorrelationId(String correlationId) {
        return auditLogRepository.findByCorrelationId(correlationId).map(this::mapToResponse);
    }

    /** Get audit summary for a tenant. */
    public AuditLogDto.AuditSummary getAuditSummary(UUID tenantId) {
        Instant now = Instant.now();
        Instant last24Hours = now.minus(24, ChronoUnit.HOURS);
        Instant last30Days = now.minus(30, ChronoUnit.DAYS);

        long totalEvents = auditLogRepository.countRecentByTenant(tenantId, last30Days);
        long last24HoursCount = auditLogRepository.countRecentByTenant(tenantId, last24Hours);

        List<Object[]> actionCounts =
                auditLogRepository.countByActionForTenant(tenantId, last30Days, now);

        Map<String, Long> actionCountMap =
                actionCounts.stream()
                        .collect(Collectors.toMap(row -> (String) row[0], row -> (Long) row[1]));

        return AuditLogDto.AuditSummary.builder()
                .tenantId(tenantId)
                .totalEvents(totalEvents)
                .last24Hours(last24HoursCount)
                .actionCounts(actionCountMap)
                .generatedAt(now)
                .build();
    }

    /** Generate compliance report for a date range. */
    public AuditLogDto.ComplianceReport generateComplianceReport(
            UUID tenantId, LocalDate startDate, LocalDate endDate) {

        Instant startTime = startDate.atStartOfDay().toInstant(java.time.ZoneOffset.UTC);
        Instant endTime = endDate.plusDays(1).atStartOfDay().toInstant(java.time.ZoneOffset.UTC);

        // Get action summary
        List<Object[]> actionSummary =
                statisticsRepository.getActionSummary(tenantId, startDate, endDate);
        Map<String, Long> eventsByAction =
                actionSummary.stream()
                        .collect(Collectors.toMap(row -> (String) row[0], row -> (Long) row[1]));

        long totalEvents = eventsByAction.values().stream().mapToLong(Long::longValue).sum();

        return AuditLogDto.ComplianceReport.builder()
                .tenantId(tenantId)
                .startDate(startDate)
                .endDate(endDate)
                .totalAuditEvents(totalEvents)
                .eventsByAction(eventsByAction)
                .eventsByUser(new HashMap<>()) // Can be enhanced to include user breakdown
                .eventsByResourceType(
                        new HashMap<>()) // Can be enhanced to include resource breakdown
                .generatedAt(Instant.now())
                .build();
    }

    /** Get daily statistics for a date range. */
    public List<AuditLogDto.DailyStats> getDailyStats(
            UUID tenantId, LocalDate startDate, LocalDate endDate) {

        List<Object[]> dailyTotals =
                statisticsRepository.getDailyTotals(tenantId, startDate, endDate);

        return dailyTotals.stream()
                .map(
                        row ->
                                AuditLogDto.DailyStats.builder()
                                        .date((LocalDate) row[0])
                                        .count((Long) row[1])
                                        .build())
                .collect(Collectors.toList());
    }

    private PageResponse<AuditLogDto.Response> mapToPageResponse(Page<AuditLog> page) {
        List<AuditLogDto.Response> content =
                page.getContent().stream().map(this::mapToResponse).collect(Collectors.toList());

        return PageResponse.of(content, page.getNumber(), page.getSize(), page.getTotalElements());
    }

    private AuditLogDto.Response mapToResponse(AuditLog auditLog) {
        return AuditLogDto.Response.builder()
                .eventId(auditLog.getEventId())
                .tenantId(auditLog.getTenantId())
                .userId(auditLog.getUserId())
                .username(auditLog.getUsername())
                .action(auditLog.getAction())
                .resourceType(auditLog.getResourceType())
                .resourceId(auditLog.getResourceId())
                .eventTime(auditLog.getEventTime())
                .sourceIp(auditLog.getSourceIp())
                .userAgent(auditLog.getUserAgent())
                .details(auditLog.getDetails())
                .correlationId(auditLog.getCorrelationId())
                .createdAt(auditLog.getCreatedAt())
                .build();
    }
}
