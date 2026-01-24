package com.supplysight.prediction.controller;

import com.supplysight.common.dto.ApiResponse;
import com.supplysight.common.dto.PageResponse;
import com.supplysight.common.exception.ResourceNotFoundException;
import com.supplysight.common.security.TenantContext;
import com.supplysight.prediction.dto.AlertDto.*;
import com.supplysight.prediction.dto.PredictionDto.*;
import com.supplysight.prediction.entity.Alert;
import com.supplysight.prediction.entity.ShipmentPrediction;
import com.supplysight.prediction.entity.ShipmentPrediction.DelayRisk;
import com.supplysight.prediction.repository.AlertRepository;
import com.supplysight.prediction.repository.ShipmentPredictionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for prediction and alert queries.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Predictions", description = "Prediction and alert APIs")
@PreAuthorize("isAuthenticated()")
public class PredictionController {

    private final ShipmentPredictionRepository predictionRepository;
    private final AlertRepository alertRepository;

    public PredictionController(
            ShipmentPredictionRepository predictionRepository,
            AlertRepository alertRepository
    ) {
        this.predictionRepository = predictionRepository;
        this.alertRepository = alertRepository;
    }

    @GetMapping("/predictions/{shipmentId}")
    @Operation(summary = "Get Latest Prediction", description = "Get the latest prediction for a shipment")
    public ResponseEntity<ApiResponse<PredictionResponse>> getLatestPrediction(
            @Parameter(description = "Shipment ID") @PathVariable UUID shipmentId
    ) {
        UUID tenantId = TenantContext.getTenantId();

        ShipmentPrediction prediction = predictionRepository
                .findLatestByTenantIdAndShipmentId(tenantId, shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Prediction", shipmentId));

        return ResponseEntity.ok(ApiResponse.success(toPredictionResponse(prediction)));
    }

    @GetMapping("/predictions/{shipmentId}/history")
    @Operation(summary = "Get Prediction History", description = "Get all predictions for a shipment")
    public ResponseEntity<ApiResponse<List<PredictionSummary>>> getPredictionHistory(
            @Parameter(description = "Shipment ID") @PathVariable UUID shipmentId
    ) {
        UUID tenantId = TenantContext.getTenantId();

        List<ShipmentPrediction> predictions = predictionRepository
                .findByTenantIdAndShipmentId(tenantId, shipmentId);

        List<PredictionSummary> summaries = predictions.stream()
                .map(this::toPredictionSummary)
                .toList();

        return ResponseEntity.ok(ApiResponse.success(summaries));
    }

    @GetMapping("/predictions")
    @Operation(summary = "List Predictions", description = "List predictions with optional risk filter")
    public ResponseEntity<ApiResponse<PageResponse<PredictionSummary>>> listPredictions(
            @Parameter(description = "Filter by delay risk") @RequestParam(required = false) DelayRisk delayRisk,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size
    ) {
        UUID tenantId = TenantContext.getTenantId();
        PageRequest pageRequest = PageRequest.of(page, Math.min(size, 100), Sort.by("createdAt").descending());

        Page<ShipmentPrediction> result = delayRisk != null
                ? predictionRepository.findByTenantIdAndDelayRisk(tenantId, delayRisk, pageRequest)
                : predictionRepository.findByTenantId(tenantId, pageRequest);

        Page<PredictionSummary> summaries = result.map(this::toPredictionSummary);

        return ResponseEntity.ok(ApiResponse.success(
                PageResponse.of(summaries.getContent(), page, size, summaries.getTotalElements())
        ));
    }

    @GetMapping("/predictions/anomalies")
    @Operation(summary = "Get Anomalies", description = "Get predictions with detected anomalies")
    public ResponseEntity<ApiResponse<PageResponse<PredictionSummary>>> getAnomalies(
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size
    ) {
        UUID tenantId = TenantContext.getTenantId();
        PageRequest pageRequest = PageRequest.of(page, Math.min(size, 100), Sort.by("createdAt").descending());

        Page<ShipmentPrediction> result = predictionRepository.findAnomaliesByTenantId(tenantId, pageRequest);
        Page<PredictionSummary> summaries = result.map(this::toPredictionSummary);

        return ResponseEntity.ok(ApiResponse.success(
                PageResponse.of(summaries.getContent(), page, size, summaries.getTotalElements())
        ));
    }

    @GetMapping("/alerts")
    @Operation(summary = "List Alerts", description = "List all alerts with optional filters")
    public ResponseEntity<ApiResponse<PageResponse<AlertSummary>>> listAlerts(
            @Parameter(description = "Show only unacknowledged") @RequestParam(defaultValue = "false") boolean unacknowledgedOnly,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size
    ) {
        UUID tenantId = TenantContext.getTenantId();
        PageRequest pageRequest = PageRequest.of(page, Math.min(size, 100), Sort.by("createdAt").descending());

        Page<Alert> result = unacknowledgedOnly
                ? alertRepository.findUnacknowledgedByTenantId(tenantId, pageRequest)
                : alertRepository.findByTenantId(tenantId, pageRequest);

        Page<AlertSummary> summaries = result.map(this::toAlertSummary);

        return ResponseEntity.ok(ApiResponse.success(
                PageResponse.of(summaries.getContent(), page, size, summaries.getTotalElements())
        ));
    }

    @GetMapping("/alerts/{shipmentId}")
    @Operation(summary = "Get Shipment Alerts", description = "Get all alerts for a shipment")
    public ResponseEntity<ApiResponse<List<AlertSummary>>> getShipmentAlerts(
            @Parameter(description = "Shipment ID") @PathVariable UUID shipmentId
    ) {
        UUID tenantId = TenantContext.getTenantId();

        List<Alert> alerts = alertRepository.findByTenantIdAndShipmentId(tenantId, shipmentId);
        List<AlertSummary> summaries = alerts.stream().map(this::toAlertSummary).toList();

        return ResponseEntity.ok(ApiResponse.success(summaries));
    }

    @PostMapping("/alerts/{alertId}/acknowledge")
    @Operation(summary = "Acknowledge Alert", description = "Mark an alert as acknowledged")
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'OPS_USER')")
    public ResponseEntity<ApiResponse<AlertResponse>> acknowledgeAlert(
            @Parameter(description = "Alert ID") @PathVariable UUID alertId
    ) {
        UUID tenantId = TenantContext.getTenantId();
        UUID userId = TenantContext.getUserId();

        Alert alert = alertRepository.findById(alertId)
                .filter(a -> a.getTenantId().equals(tenantId))
                .orElseThrow(() -> new ResourceNotFoundException("Alert", alertId));

        alert.acknowledge(userId);
        alert = alertRepository.save(alert);

        return ResponseEntity.ok(ApiResponse.success(toAlertResponse(alert)));
    }

    @GetMapping("/alerts/count")
    @Operation(summary = "Get Alert Count", description = "Get count of alerts including unacknowledged")
    public ResponseEntity<ApiResponse<AlertCount>> getAlertCount() {
        UUID tenantId = TenantContext.getTenantId();

        long total = alertRepository.findByTenantId(tenantId, PageRequest.of(0, 1)).getTotalElements();
        long unacknowledged = alertRepository.countUnacknowledgedByTenantId(tenantId);

        return ResponseEntity.ok(ApiResponse.success(new AlertCount(total, unacknowledged)));
    }

    // Mapping methods

    private PredictionResponse toPredictionResponse(ShipmentPrediction p) {
        return new PredictionResponse(
                p.getId(),
                p.getShipmentId(),
                p.getTenantId(),
                p.getEventId(),
                p.getEta(),
                p.getEtaConfidence(),
                p.getDelayProbability(),
                p.getDelayRisk(),
                p.isAnomalyDetected(),
                p.getAnomalyFlags(),
                p.getDistanceRemainingKm(),
                p.getAverageSpeedKmph(),
                p.getDwellTimeHours(),
                p.getFactors(),
                p.getModelVersion(),
                p.getCreatedAt()
        );
    }

    private PredictionSummary toPredictionSummary(ShipmentPrediction p) {
        return new PredictionSummary(
                p.getId(),
                p.getShipmentId(),
                p.getEta(),
                p.getDelayProbability(),
                p.getDelayRisk(),
                p.isAnomalyDetected(),
                p.getCreatedAt()
        );
    }

    private AlertSummary toAlertSummary(Alert a) {
        return new AlertSummary(
                a.getId(),
                a.getShipmentId(),
                a.getAlertType(),
                a.getSeverity(),
                a.getMessage(),
                a.isAcknowledged(),
                a.getCreatedAt()
        );
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
                a.getCreatedAt()
        );
    }
}
