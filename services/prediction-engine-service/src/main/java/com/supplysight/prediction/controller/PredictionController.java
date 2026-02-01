package com.supplysight.prediction.controller;

import com.supplysight.common.dto.ApiResponse;
import com.supplysight.common.dto.PageResponse;
import com.supplysight.common.exception.ResourceNotFoundException;
import com.supplysight.common.security.TenantContext;
import com.supplysight.prediction.dto.PredictionDto.*;
import com.supplysight.prediction.entity.ShipmentPrediction;
import com.supplysight.prediction.entity.ShipmentPrediction.DelayRisk;
import com.supplysight.prediction.repository.ShipmentPredictionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

        public PredictionController(ShipmentPredictionRepository predictionRepository) {
                this.predictionRepository = predictionRepository;
        }

        @GetMapping("/predictions/{shipmentId}")
        @Operation(summary = "Get Latest Prediction", description = "Get the latest prediction for a shipment")
        public ResponseEntity<ApiResponse<PredictionResponse>> getLatestPrediction(
                        @Parameter(description = "Shipment ID") @PathVariable UUID shipmentId) {
                UUID tenantId = TenantContext.getTenantId();

                ShipmentPrediction prediction = predictionRepository
                                .findLatestByTenantIdAndShipmentId(tenantId, shipmentId)
                                .orElseThrow(() -> new ResourceNotFoundException("Prediction", shipmentId));

                return ResponseEntity.ok(ApiResponse.success(toPredictionResponse(prediction)));
        }

        @GetMapping("/predictions/{shipmentId}/history")
        @Operation(summary = "Get Prediction History", description = "Get all predictions for a shipment")
        public ResponseEntity<ApiResponse<List<PredictionSummary>>> getPredictionHistory(
                        @Parameter(description = "Shipment ID") @PathVariable UUID shipmentId) {
                UUID tenantId = TenantContext.getTenantId();

                List<ShipmentPrediction> predictions = predictionRepository.findByTenantIdAndShipmentId(tenantId,
                                shipmentId);

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
                        @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
                UUID tenantId = TenantContext.getTenantId();
                PageRequest pageRequest = PageRequest.of(page, Math.min(size, 100), Sort.by("createdAt").descending());

                Page<ShipmentPrediction> result = delayRisk != null
                                ? predictionRepository.findByTenantIdAndDelayRisk(tenantId, delayRisk, pageRequest)
                                : predictionRepository.findByTenantId(tenantId, pageRequest);

                Page<PredictionSummary> summaries = result.map(this::toPredictionSummary);

                return ResponseEntity.ok(ApiResponse.success(
                                PageResponse.of(summaries.getContent(), page, size, summaries.getTotalElements())));
        }

        @GetMapping("/predictions/anomalies")
        @Operation(summary = "Get Anomalies", description = "Get predictions with detected anomalies")
        public ResponseEntity<ApiResponse<PageResponse<PredictionSummary>>> getAnomalies(
                        @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
                        @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
                UUID tenantId = TenantContext.getTenantId();
                PageRequest pageRequest = PageRequest.of(page, Math.min(size, 100), Sort.by("createdAt").descending());

                Page<ShipmentPrediction> result = predictionRepository.findAnomaliesByTenantId(tenantId, pageRequest);
                Page<PredictionSummary> summaries = result.map(this::toPredictionSummary);

                return ResponseEntity.ok(ApiResponse.success(
                                PageResponse.of(summaries.getContent(), page, size, summaries.getTotalElements())));
        }

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
                                p.getCreatedAt());
        }

        private PredictionSummary toPredictionSummary(ShipmentPrediction p) {
                return new PredictionSummary(
                                p.getId(),
                                p.getShipmentId(),
                                p.getEta(),
                                p.getDelayProbability(),
                                p.getDelayRisk(),
                                p.isAnomalyDetected(),
                                p.getCreatedAt());
        }
}
