package com.supplysight.audit.service;

import com.supplysight.audit.dto.AlertHistoryDto;
import com.supplysight.audit.entity.AlertHistory;
import com.supplysight.audit.repository.AlertHistoryRepository;
import com.supplysight.common.dto.PageResponse;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Service for managing alert history. */
@Service
@Transactional(readOnly = true)
public class AlertHistoryService {

    private final AlertHistoryRepository alertHistoryRepository;

    public AlertHistoryService(AlertHistoryRepository alertHistoryRepository) {
        this.alertHistoryRepository = alertHistoryRepository;
    }

    /** Get paginated alert history for a tenant. */
    public PageResponse<AlertHistoryDto.Response> getAlertHistory(
            UUID tenantId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<AlertHistory> alerts =
                alertHistoryRepository.findByTenantIdOrderByCreatedAtDesc(tenantId, pageable);
        return mapToPageResponse(alerts);
    }

    /** Get alert history by status. */
    public PageResponse<AlertHistoryDto.Response> getAlertsByStatus(
            UUID tenantId, String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<AlertHistory> alerts =
                alertHistoryRepository.findByTenantIdAndStatusOrderByCreatedAtDesc(
                        tenantId, status, pageable);
        return mapToPageResponse(alerts);
    }

    /** Get open alerts for a tenant. */
    public List<AlertHistoryDto.Response> getOpenAlerts(UUID tenantId) {
        List<AlertHistory> openAlerts =
                alertHistoryRepository.findOpenAlerts(tenantId, List.of("OPEN", "ACKNOWLEDGED"));
        return openAlerts.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    /** Get alert history for a shipment. */
    public PageResponse<AlertHistoryDto.Response> getAlertsByShipment(
            UUID tenantId, UUID shipmentId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<AlertHistory> alerts =
                alertHistoryRepository.findByTenantIdAndShipmentIdOrderByCreatedAtDesc(
                        tenantId, shipmentId, pageable);
        return mapToPageResponse(alerts);
    }

    /** Get alert summary for a tenant. */
    public AlertHistoryDto.AlertSummary getAlertSummary(UUID tenantId) {
        List<Object[]> statusCounts = alertHistoryRepository.countByStatus(tenantId);
        Map<String, Long> statusMap =
                statusCounts.stream()
                        .collect(Collectors.toMap(row -> (String) row[0], row -> (Long) row[1]));

        Instant last30Days = Instant.now().minus(30, ChronoUnit.DAYS);
        List<Object[]> typeCounts = alertHistoryRepository.countByAlertType(tenantId, last30Days);
        Map<String, Long> typeMap =
                typeCounts.stream()
                        .collect(Collectors.toMap(row -> (String) row[0], row -> (Long) row[1]));

        long total = statusMap.values().stream().mapToLong(Long::longValue).sum();
        long open = statusMap.getOrDefault("OPEN", 0L);
        long acknowledged = statusMap.getOrDefault("ACKNOWLEDGED", 0L);
        long resolved = statusMap.getOrDefault("RESOLVED", 0L);

        return AlertHistoryDto.AlertSummary.builder()
                .tenantId(tenantId)
                .totalAlerts(total)
                .openAlerts(open)
                .acknowledgedAlerts(acknowledged)
                .resolvedAlerts(resolved)
                .alertsByType(typeMap)
                .generatedAt(Instant.now())
                .build();
    }

    /** Acknowledge an alert. */
    @Transactional
    public Optional<AlertHistoryDto.Response> acknowledgeAlert(UUID alertId, UUID userId) {
        return alertHistoryRepository
                .findByAlertId(alertId)
                .map(
                        alert -> {
                            alert.setStatus("ACKNOWLEDGED");
                            alert.setAcknowledgedBy(userId);
                            alert.setAcknowledgedAt(Instant.now());
                            return mapToResponse(alertHistoryRepository.save(alert));
                        });
    }

    /** Resolve an alert. */
    @Transactional
    public Optional<AlertHistoryDto.Response> resolveAlert(UUID alertId, UUID userId) {
        return alertHistoryRepository
                .findByAlertId(alertId)
                .map(
                        alert -> {
                            alert.setStatus("RESOLVED");
                            if (alert.getAcknowledgedBy() == null) {
                                alert.setAcknowledgedBy(userId);
                                alert.setAcknowledgedAt(Instant.now());
                            }
                            alert.setResolvedAt(Instant.now());
                            return mapToResponse(alertHistoryRepository.save(alert));
                        });
    }

    private PageResponse<AlertHistoryDto.Response> mapToPageResponse(Page<AlertHistory> page) {
        List<AlertHistoryDto.Response> content =
                page.getContent().stream().map(this::mapToResponse).collect(Collectors.toList());

        return PageResponse.of(content, page.getNumber(), page.getSize(), page.getTotalElements());
    }

    private AlertHistoryDto.Response mapToResponse(AlertHistory alert) {
        return AlertHistoryDto.Response.builder()
                .id(alert.getId())
                .alertId(alert.getAlertId())
                .tenantId(alert.getTenantId())
                .shipmentId(alert.getShipmentId())
                .alertType(alert.getAlertType())
                .severity(alert.getSeverity())
                .message(alert.getMessage())
                .status(alert.getStatus())
                .acknowledgedBy(alert.getAcknowledgedBy())
                .acknowledgedAt(alert.getAcknowledgedAt())
                .resolvedAt(alert.getResolvedAt())
                .createdAt(alert.getCreatedAt())
                .details(alert.getDetails())
                .build();
    }
}
