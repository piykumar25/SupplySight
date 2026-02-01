package com.supplysight.prediction.controller;

import com.supplysight.common.dto.ApiResponse;
import com.supplysight.common.dto.PageResponse;
import com.supplysight.common.exception.ResourceNotFoundException;
import com.supplysight.common.security.TenantContext;
import com.supplysight.prediction.dto.AlertDto.*;
import com.supplysight.prediction.entity.Alert;
import com.supplysight.prediction.entity.Alert.Severity;
import com.supplysight.prediction.repository.AlertRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * REST controller for alert management with real-time SSE streaming.
 */
@RestController
@RequestMapping("/api/v1/alerts")
@Tag(name = "Alerts", description = "Alert management and real-time streaming")
@PreAuthorize("hasAnyRole('ADMIN', 'OPS_USER')")
public class AlertController {

    private static final Logger log = LoggerFactory.getLogger(AlertController.class);

    private final AlertRepository alertRepository;

    // SSE emitters by tenant
    private final Map<UUID, List<SseEmitter>> sseEmitters = new ConcurrentHashMap<>();
    private final ExecutorService sseExecutor = Executors.newCachedThreadPool();

    public AlertController(AlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    // ==================== LIST & QUERY ENDPOINTS ====================

    @GetMapping
    @Operation(summary = "List Alerts", description = "Get paginated list of alerts with optional filters")
    public ResponseEntity<ApiResponse<PageResponse<AlertSummary>>> listAlerts(
            @Parameter(description = "Filter by severity") @RequestParam(required = false) Severity severity,
            @Parameter(description = "Show only unacknowledged") @RequestParam(defaultValue = "false") boolean unacknowledgedOnly,
            @Parameter(description = "Show only unresolved") @RequestParam(defaultValue = "false") boolean unresolvedOnly,
            @Parameter(description = "Search query") @RequestParam(required = false) String search,
            @Parameter(description = "From date") @RequestParam(required = false) Instant fromDate,
            @Parameter(description = "To date") @RequestParam(required = false) Instant toDate,
            @Parameter(description = "Sort field") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction") @RequestParam(defaultValue = "desc") String sortDir,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        UUID tenantId = TenantContext.getTenantId();

        Sort sort = Sort.by(sortDir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, sortBy);
        PageRequest pageRequest = PageRequest.of(page, Math.min(size, 100), sort);

        Page<Alert> alertPage;

        // Apply filters
        if (unacknowledgedOnly) {
            alertPage = alertRepository.findByTenantIdAndAcknowledgedFalse(tenantId, pageRequest);
        } else if (unresolvedOnly) {
            alertPage = alertRepository.findByTenantIdAndResolvedFalse(tenantId, pageRequest);
        } else if (severity != null) {
            alertPage = alertRepository.findByTenantIdAndSeverity(tenantId, severity, pageRequest);
        } else {
            alertPage = alertRepository.findByTenantId(tenantId, pageRequest);
        }

        PageResponse<AlertSummary> response = PageResponse.of(
                alertPage.getContent().stream().map(this::toAlertSummary).toList(),
                alertPage.getNumber(),
                alertPage.getSize(),
                alertPage.getTotalElements());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{alertId}")
    @Operation(summary = "Get Alert", description = "Get a single alert by ID")
    public ResponseEntity<ApiResponse<AlertResponse>> getAlert(
            @Parameter(description = "Alert ID") @PathVariable UUID alertId) {
        UUID tenantId = TenantContext.getTenantId();

        Alert alert = alertRepository.findById(alertId)
                .filter(a -> a.getTenantId().equals(tenantId))
                .orElseThrow(() -> new ResourceNotFoundException("Alert", alertId));

        return ResponseEntity.ok(ApiResponse.success(toAlertResponse(alert)));
    }

    @GetMapping("/shipment/{shipmentId}")
    @Operation(summary = "Get Shipment Alerts", description = "Get all alerts for a specific shipment")
    public ResponseEntity<ApiResponse<List<AlertSummary>>> getShipmentAlerts(
            @Parameter(description = "Shipment ID") @PathVariable UUID shipmentId) {
        UUID tenantId = TenantContext.getTenantId();

        List<Alert> alerts = alertRepository.findByTenantIdAndShipmentIdOrderByCreatedAtDesc(tenantId, shipmentId);

        return ResponseEntity.ok(ApiResponse.success(
                alerts.stream().map(this::toAlertSummary).toList()));
    }

    @GetMapping("/count")
    @Operation(summary = "Get Alert Counts", description = "Get total, unacknowledged, and unresolved alert counts")
    public ResponseEntity<ApiResponse<AlertCount>> getAlertCount() {
        UUID tenantId = TenantContext.getTenantId();

        long total = alertRepository.countByTenantId(tenantId);
        long unacknowledged = alertRepository.countByTenantIdAndAcknowledgedFalse(tenantId);
        long unresolved = alertRepository.countByTenantIdAndResolvedFalse(tenantId);

        return ResponseEntity.ok(ApiResponse.success(new AlertCount(total, unacknowledged, unresolved)));
    }

    // ==================== ACTION ENDPOINTS ====================

    @PostMapping("/{alertId}/acknowledge")
    @Operation(summary = "Acknowledge Alert", description = "Mark an alert as acknowledged")
    @Transactional
    public ResponseEntity<ApiResponse<AlertResponse>> acknowledgeAlert(
            @Parameter(description = "Alert ID") @PathVariable UUID alertId,
            @RequestBody(required = false) AcknowledgeRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        UUID userId = TenantContext.getUserId();

        Alert alert = alertRepository.findById(alertId)
                .filter(a -> a.getTenantId().equals(tenantId))
                .orElseThrow(() -> new ResourceNotFoundException("Alert", alertId));

        alert.acknowledge(userId);
        alert = alertRepository.save(alert);

        // Broadcast SSE event
        broadcastAlertEvent(tenantId, "alert:acknowledged", alert);

        log.info("Alert {} acknowledged by user {}", alertId, userId);
        return ResponseEntity.ok(ApiResponse.success(toAlertResponse(alert)));
    }

    @PostMapping("/{alertId}/resolve")
    @Operation(summary = "Resolve Alert", description = "Mark an alert as resolved")
    @Transactional
    public ResponseEntity<ApiResponse<AlertResponse>> resolveAlert(
            @Parameter(description = "Alert ID") @PathVariable UUID alertId,
            @RequestBody(required = false) ResolveRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        UUID userId = TenantContext.getUserId();

        Alert alert = alertRepository.findById(alertId)
                .filter(a -> a.getTenantId().equals(tenantId))
                .orElseThrow(() -> new ResourceNotFoundException("Alert", alertId));

        if (request != null && request.comment() != null) {
            alert.resolveWithComment(userId, request.comment());
        } else {
            alert.resolve(userId);
        }
        alert = alertRepository.save(alert);

        // Broadcast SSE event
        broadcastAlertEvent(tenantId, "alert:resolved", alert);

        log.info("Alert {} resolved by user {}", alertId, userId);
        return ResponseEntity.ok(ApiResponse.success(toAlertResponse(alert)));
    }

    // ==================== BULK ENDPOINTS ====================

    @PostMapping("/bulk/acknowledge")
    @Operation(summary = "Bulk Acknowledge", description = "Acknowledge multiple alerts at once")
    @Transactional
    public ResponseEntity<ApiResponse<BulkActionResponse>> bulkAcknowledge(
            @RequestBody BulkActionRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        UUID userId = TenantContext.getUserId();

        int successCount = 0;
        List<UUID> failedIds = new ArrayList<>();

        for (UUID alertId : request.alertIds()) {
            try {
                Alert alert = alertRepository.findById(alertId)
                        .filter(a -> a.getTenantId().equals(tenantId))
                        .orElse(null);

                if (alert != null && !alert.isAcknowledged()) {
                    alert.acknowledge(userId);
                    alertRepository.save(alert);
                    broadcastAlertEvent(tenantId, "alert:acknowledged", alert);
                    successCount++;
                } else if (alert == null) {
                    failedIds.add(alertId);
                } else {
                    successCount++; // Already acknowledged, count as success
                }
            } catch (Exception e) {
                log.error("Failed to acknowledge alert {}: {}", alertId, e.getMessage());
                failedIds.add(alertId);
            }
        }

        log.info("Bulk acknowledge: {} success, {} failed by user {}", successCount, failedIds.size(), userId);
        return ResponseEntity.ok(ApiResponse.success(
                new BulkActionResponse(successCount, failedIds.size(), failedIds)));
    }

    @PostMapping("/bulk/resolve")
    @Operation(summary = "Bulk Resolve", description = "Resolve multiple alerts at once")
    @Transactional
    public ResponseEntity<ApiResponse<BulkActionResponse>> bulkResolve(
            @RequestBody BulkActionRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        UUID userId = TenantContext.getUserId();

        int successCount = 0;
        List<UUID> failedIds = new ArrayList<>();

        for (UUID alertId : request.alertIds()) {
            try {
                Alert alert = alertRepository.findById(alertId)
                        .filter(a -> a.getTenantId().equals(tenantId))
                        .orElse(null);

                if (alert != null && !alert.isResolved()) {
                    if (request.comment() != null) {
                        alert.resolveWithComment(userId, request.comment());
                    } else {
                        alert.resolve(userId);
                    }
                    alertRepository.save(alert);
                    broadcastAlertEvent(tenantId, "alert:resolved", alert);
                    successCount++;
                } else if (alert == null) {
                    failedIds.add(alertId);
                } else {
                    successCount++; // Already resolved, count as success
                }
            } catch (Exception e) {
                log.error("Failed to resolve alert {}: {}", alertId, e.getMessage());
                failedIds.add(alertId);
            }
        }

        log.info("Bulk resolve: {} success, {} failed by user {}", successCount, failedIds.size(), userId);
        return ResponseEntity.ok(ApiResponse.success(
                new BulkActionResponse(successCount, failedIds.size(), failedIds)));
    }

    // ==================== SSE STREAMING ====================

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Alert Stream", description = "Real-time SSE stream for alert updates")
    public SseEmitter streamAlerts() {
        UUID tenantId = TenantContext.getTenantId();

        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        // Add emitter to tenant's list
        sseEmitters.computeIfAbsent(tenantId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        // Handle completion and timeout
        emitter.onCompletion(() -> removeEmitter(tenantId, emitter));
        emitter.onTimeout(() -> removeEmitter(tenantId, emitter));
        emitter.onError(e -> {
            log.debug("SSE error for tenant {}: {}", tenantId, e.getMessage());
            removeEmitter(tenantId, emitter);
        });

        // Send initial heartbeat
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("{\"status\":\"connected\",\"timestamp\":\"" + Instant.now() + "\"}"));
        } catch (Exception e) {
            log.debug("Failed to send SSE connect: {}", e.getMessage());
        }

        log.info("SSE client connected for tenant {}", tenantId);
        return emitter;
    }

    // ==================== HELPER METHODS ====================

    private void removeEmitter(UUID tenantId, SseEmitter emitter) {
        List<SseEmitter> emitters = sseEmitters.get(tenantId);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                sseEmitters.remove(tenantId);
            }
        }
    }

    private void broadcastAlertEvent(UUID tenantId, String eventType, Alert alert) {
        List<SseEmitter> emitters = sseEmitters.get(tenantId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        AlertEvent event = new AlertEvent(eventType, toAlertSummary(alert), Instant.now());

        sseExecutor.execute(() -> {
            List<SseEmitter> deadEmitters = new ArrayList<>();

            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event()
                            .name(eventType)
                            .data(event));
                } catch (Exception e) {
                    log.debug("Failed to send SSE event to client: {}", e.getMessage());
                    deadEmitters.add(emitter);
                }
            }

            // Cleanup dead emitters
            deadEmitters.forEach(e -> removeEmitter(tenantId, e));
        });
    }

    /**
     * Broadcast a new alert event (called from AlertService).
     */
    public void broadcastNewAlert(Alert alert) {
        broadcastAlertEvent(alert.getTenantId(), "alert:new", alert);
    }

    private AlertResponse toAlertResponse(Alert a) {
        return new AlertResponse(
                a.getId(),
                a.getShipmentId(),
                a.getTenantId(),
                a.getPredictionId(),
                a.getAlertType(),
                a.getSeverity(),
                a.getMessage(),
                a.getDetails(),
                a.isAcknowledged(),
                a.getAcknowledgedAt(),
                a.getAcknowledgedBy(),
                a.isResolved(),
                a.getResolvedAt(),
                a.getResolvedBy(),
                a.getResolutionComment(),
                a.getCreatedAt());
    }

    private AlertSummary toAlertSummary(Alert a) {
        return new AlertSummary(
                a.getId(),
                a.getShipmentId(),
                a.getAlertType(),
                a.getSeverity(),
                a.getMessage(),
                a.isAcknowledged(),
                a.isResolved(),
                a.getCreatedAt());
    }
}
