package com.supplysight.prediction.service;

import com.supplysight.common.event.TrackingEvent;
import com.supplysight.prediction.entity.ShipmentPrediction;
import com.supplysight.prediction.entity.ShipmentPrediction.DelayRisk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Heuristic-based prediction service.
 * Computes ETA, delay probability, and anomaly detection using baseline heuristics.
 * ML-ready: outputs can be used as features for ML models.
 */
@Service
public class PredictionHeuristicsService {

    private static final Logger log = LoggerFactory.getLogger(PredictionHeuristicsService.class);

    @Value("${prediction.default-average-speed:50.0}")
    private double defaultAverageSpeed;

    @Value("${prediction.max-reasonable-speed:120.0}")
    private double maxReasonableSpeed;

    @Value("${prediction.min-speed-threshold:5.0}")
    private double minSpeedThreshold;

    @Value("${prediction.dwell-time-warning-hours:4}")
    private int dwellTimeWarningHours;

    @Value("${prediction.dwell-time-critical-hours:8}")
    private int dwellTimeCriticalHours;

    @Value("${prediction.delay-probability-warning:0.3}")
    private double delayProbabilityWarning;

    @Value("${prediction.delay-probability-high:0.6}")
    private double delayProbabilityHigh;

    /**
     * Context for making predictions.
     */
    public record PredictionContext(
            TrackingEvent currentEvent,
            TrackingEvent previousEvent,
            Double destinationLat,
            Double destinationLon,
            Instant originalEta,
            int eventCount
    ) {}

    /**
     * Result of prediction computation.
     */
    public record PredictionResult(
            Instant eta,
            Double etaConfidence,
            Double delayProbability,
            DelayRisk delayRisk,
            boolean anomalyDetected,
            List<String> anomalyFlags,
            Double distanceRemainingKm,
            Double averageSpeedKmph,
            Double dwellTimeHours,
            Map<String, Object> factors
    ) {}

    /**
     * Compute prediction from event and context.
     */
    public PredictionResult computePrediction(PredictionContext context) {
        log.debug("Computing prediction for shipment {} event {}", 
                context.currentEvent().shipmentId(), context.currentEvent().eventId());

        List<String> anomalyFlags = new ArrayList<>();
        Map<String, Object> factors = new HashMap<>();

        // Extract speed from payload if available
        Double speed = extractSpeed(context.currentEvent());
        factors.put("reportedSpeed", speed);

        // Calculate distance remaining (if destination known)
        Double distanceRemaining = calculateDistanceRemaining(context);
        factors.put("distanceRemainingKm", distanceRemaining);

        // Calculate average speed from trajectory
        Double averageSpeed = calculateAverageSpeed(context, speed);
        factors.put("averageSpeedKmph", averageSpeed);

        // Calculate dwell time (time since last event)
        Double dwellTime = calculateDwellTime(context);
        factors.put("dwellTimeHours", dwellTime);

        // Detect anomalies
        detectAnomalies(context, speed, averageSpeed, dwellTime, anomalyFlags, factors);

        // Calculate delay probability
        double delayProbability = calculateDelayProbability(context, dwellTime, distanceRemaining, averageSpeed, factors);
        factors.put("delayProbability", delayProbability);

        // Determine delay risk level
        DelayRisk delayRisk = determineDelayRisk(delayProbability, anomalyFlags);
        factors.put("delayRisk", delayRisk.name());

        // Calculate ETA
        Instant eta = calculateEta(context, distanceRemaining, averageSpeed);
        Double etaConfidence = calculateEtaConfidence(context, distanceRemaining, anomalyFlags);
        factors.put("eta", eta != null ? eta.toString() : null);
        factors.put("etaConfidence", etaConfidence);

        return new PredictionResult(
                eta,
                etaConfidence,
                delayProbability,
                delayRisk,
                !anomalyFlags.isEmpty(),
                anomalyFlags,
                distanceRemaining,
                averageSpeed,
                dwellTime,
                factors
        );
    }

    private Double extractSpeed(TrackingEvent event) {
        if (event.payload() == null) return null;
        
        Object speedObj = event.payload().get("speedKmph");
        if (speedObj == null) {
            speedObj = event.payload().get("speed");
        }
        
        if (speedObj instanceof Number) {
            return ((Number) speedObj).doubleValue();
        }
        return null;
    }

    private Double calculateDistanceRemaining(PredictionContext context) {
        if (context.destinationLat() == null || context.destinationLon() == null) {
            return null;
        }

        TrackingEvent.Location location = context.currentEvent().location();
        if (location == null || location.lat() == null || location.lon() == null) {
            return null;
        }

        return haversineDistance(
                location.lat(), location.lon(),
                context.destinationLat(), context.destinationLon()
        );
    }

    private Double calculateAverageSpeed(PredictionContext context, Double reportedSpeed) {
        // Use reported speed if available
        if (reportedSpeed != null && reportedSpeed > 0) {
            return reportedSpeed;
        }

        // Calculate from trajectory if previous event available
        if (context.previousEvent() != null && context.previousEvent().location() != null 
            && context.currentEvent().location() != null) {
            
            TrackingEvent.Location prevLoc = context.previousEvent().location();
            TrackingEvent.Location currLoc = context.currentEvent().location();

            if (prevLoc.lat() != null && currLoc.lat() != null) {
                double distance = haversineDistance(
                        prevLoc.lat(), prevLoc.lon(),
                        currLoc.lat(), currLoc.lon()
                );

                Duration timeDiff = Duration.between(
                        context.previousEvent().eventTime(),
                        context.currentEvent().eventTime()
                );

                double hours = timeDiff.toMillis() / (1000.0 * 60 * 60);
                if (hours > 0) {
                    return distance / hours;
                }
            }
        }

        return defaultAverageSpeed;
    }

    private Double calculateDwellTime(PredictionContext context) {
        if (context.previousEvent() == null) {
            return 0.0;
        }

        Duration timeDiff = Duration.between(
                context.previousEvent().eventTime(),
                context.currentEvent().eventTime()
        );

        return timeDiff.toMillis() / (1000.0 * 60 * 60);
    }

    private void detectAnomalies(PredictionContext context, Double speed, Double averageSpeed,
                                  Double dwellTime, List<String> anomalyFlags, Map<String, Object> factors) {
        // Speed anomaly detection
        if (speed != null) {
            if (speed > maxReasonableSpeed) {
                anomalyFlags.add("EXCESSIVE_SPEED");
                factors.put("anomalyExcessiveSpeed", speed);
            }
            if (speed < minSpeedThreshold && !"AT_HUB".equals(context.currentEvent().eventType())) {
                anomalyFlags.add("UNUSUALLY_SLOW");
                factors.put("anomalySlowSpeed", speed);
            }
        }

        // Dwell time anomaly
        if (dwellTime != null && dwellTime > dwellTimeCriticalHours) {
            anomalyFlags.add("EXCESSIVE_DWELL");
            factors.put("anomalyDwellTime", dwellTime);
        }

        // Event type anomalies
        String eventType = context.currentEvent().eventType();
        if ("EXCEPTION".equals(eventType)) {
            anomalyFlags.add("EXCEPTION_REPORTED");
        }
        if ("DELAYED".equals(eventType)) {
            anomalyFlags.add("DELAY_REPORTED");
        }
    }

    private double calculateDelayProbability(PredictionContext context, Double dwellTime,
                                              Double distanceRemaining, Double averageSpeed,
                                              Map<String, Object> factors) {
        double probability = 0.0;

        // Factor 1: Event type
        String eventType = context.currentEvent().eventType();
        if ("DELAYED".equals(eventType)) {
            probability += 0.4;
        } else if ("EXCEPTION".equals(eventType)) {
            probability += 0.3;
        } else if ("AT_HUB".equals(eventType)) {
            probability += 0.1;
        }

        // Factor 2: Dwell time
        if (dwellTime != null) {
            if (dwellTime > dwellTimeCriticalHours) {
                probability += 0.3;
            } else if (dwellTime > dwellTimeWarningHours) {
                probability += 0.15;
            }
        }

        // Factor 3: Speed vs expected
        if (averageSpeed != null && averageSpeed < defaultAverageSpeed * 0.5) {
            probability += 0.15;
        }

        // Factor 4: ETA slip
        if (context.originalEta() != null && distanceRemaining != null && averageSpeed != null) {
            double hoursToDestination = distanceRemaining / Math.max(averageSpeed, 1.0);
            Instant projectedEta = context.currentEvent().eventTime().plusSeconds((long)(hoursToDestination * 3600));
            
            if (projectedEta.isAfter(context.originalEta())) {
                Duration slip = Duration.between(context.originalEta(), projectedEta);
                double slipHours = slip.toHours();
                probability += Math.min(slipHours * 0.05, 0.3);
                factors.put("etaSlipHours", slipHours);
            }
        }

        // Cap probability at 0.95
        return Math.min(probability, 0.95);
    }

    private DelayRisk determineDelayRisk(double delayProbability, List<String> anomalyFlags) {
        if (delayProbability >= delayProbabilityHigh || anomalyFlags.contains("EXCESSIVE_DWELL")) {
            return DelayRisk.HIGH;
        }
        if (delayProbability >= delayProbabilityWarning || !anomalyFlags.isEmpty()) {
            return DelayRisk.MEDIUM;
        }
        return DelayRisk.LOW;
    }

    private Instant calculateEta(PredictionContext context, Double distanceRemaining, Double averageSpeed) {
        if (distanceRemaining == null || averageSpeed == null || averageSpeed <= 0) {
            return context.originalEta();
        }

        double hoursToDestination = distanceRemaining / averageSpeed;
        return context.currentEvent().eventTime().plusSeconds((long)(hoursToDestination * 3600));
    }

    private Double calculateEtaConfidence(PredictionContext context, Double distanceRemaining, 
                                           List<String> anomalyFlags) {
        double confidence = 0.7; // Base confidence

        // More events = higher confidence
        if (context.eventCount() > 10) {
            confidence += 0.1;
        } else if (context.eventCount() > 5) {
            confidence += 0.05;
        }

        // Known destination = higher confidence
        if (distanceRemaining != null) {
            confidence += 0.1;
        }

        // Anomalies reduce confidence
        confidence -= anomalyFlags.size() * 0.1;

        return Math.max(0.1, Math.min(confidence, 0.95));
    }

    /**
     * Calculate distance between two points using Haversine formula.
     */
    private double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0; // Earth's radius in km

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }
}
