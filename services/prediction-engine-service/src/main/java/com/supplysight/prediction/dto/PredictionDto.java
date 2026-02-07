package com.supplysight.prediction.dto;

import com.supplysight.prediction.entity.ShipmentPrediction.DelayRisk;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** DTOs for prediction operations. */
public final class PredictionDto {

    private PredictionDto() {}

    /** Prediction response. */
    public record PredictionResponse(
            UUID id,
            UUID shipmentId,
            UUID tenantId,
            UUID eventId,
            Instant eta,
            Double etaConfidence,
            Double delayProbability,
            DelayRisk delayRisk,
            boolean anomalyDetected,
            List<String> anomalyFlags,
            Double distanceRemainingKm,
            Double averageSpeedKmph,
            Double dwellTimeHours,
            Map<String, Object> factors,
            String modelVersion,
            Instant createdAt) {}

    /** Prediction summary for listing. */
    public record PredictionSummary(
            UUID id,
            UUID shipmentId,
            Instant eta,
            Double delayProbability,
            DelayRisk delayRisk,
            boolean anomalyDetected,
            Instant createdAt) {}

    /** Prediction statistics. */
    public record PredictionStats(
            long totalPredictions,
            long highRiskCount,
            long mediumRiskCount,
            long lowRiskCount,
            long anomalyCount,
            double avgDelayProbability) {}
}
