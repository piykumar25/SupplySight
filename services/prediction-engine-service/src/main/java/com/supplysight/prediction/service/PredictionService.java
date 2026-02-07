package com.supplysight.prediction.service;

import com.supplysight.common.event.TrackingEvent;
import com.supplysight.common.kafka.KafkaTopics;
import com.supplysight.prediction.dto.AlertDto;
import com.supplysight.prediction.entity.Alert;
import com.supplysight.prediction.entity.Alert.AlertType;
import com.supplysight.prediction.entity.Alert.Severity;
import com.supplysight.prediction.entity.ShipmentPrediction;
import com.supplysight.prediction.entity.ShipmentPrediction.DelayRisk;
import com.supplysight.prediction.repository.AlertRepository;
import com.supplysight.prediction.repository.ShipmentPredictionRepository;
import com.supplysight.prediction.service.PredictionHeuristicsService.PredictionContext;
import com.supplysight.prediction.service.PredictionHeuristicsService.PredictionResult;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Main prediction service that processes events and generates predictions. */
@Service
public class PredictionService {

    private static final Logger log = LoggerFactory.getLogger(PredictionService.class);

    private final ShipmentPredictionRepository predictionRepository;
    private final AlertRepository alertRepository;
    private final PredictionHeuristicsService heuristicsService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final Counter predictionsGeneratedCounter;
    private final Counter alertsGeneratedCounter;

    public PredictionService(
            ShipmentPredictionRepository predictionRepository,
            AlertRepository alertRepository,
            PredictionHeuristicsService heuristicsService,
            KafkaTemplate<String, Object> kafkaTemplate,
            MeterRegistry meterRegistry) {
        this.predictionRepository = predictionRepository;
        this.alertRepository = alertRepository;
        this.heuristicsService = heuristicsService;
        this.kafkaTemplate = kafkaTemplate;

        this.predictionsGeneratedCounter =
                Counter.builder("predictions.generated")
                        .description("Number of predictions generated")
                        .register(meterRegistry);
        this.alertsGeneratedCounter =
                Counter.builder("alerts.generated")
                        .description("Number of alerts generated")
                        .register(meterRegistry);
    }

    /** Process a validated event and generate prediction. */
    @Transactional
    public ShipmentPrediction processEvent(TrackingEvent event) {
        log.debug("Processing event {} for prediction", event.eventId());

        // Check if prediction already exists for this event
        if (predictionRepository.existsByShipmentIdAndEventId(
                event.shipmentId(), event.eventId())) {
            log.debug("Prediction already exists for event {}", event.eventId());
            return null;
        }

        // Get previous prediction for context
        ShipmentPrediction previousPrediction =
                predictionRepository
                        .findLatestByTenantIdAndShipmentId(event.tenantId(), event.shipmentId())
                        .orElse(null);

        // Build context
        TrackingEvent previousEvent =
                null; // Would need to fetch from visibility service in production
        int eventCount =
                previousPrediction != null
                        ? previousPrediction.getFactors() != null
                                ? getIntFactor(previousPrediction.getFactors(), "eventCount", 0) + 1
                                : 1
                        : 1;

        PredictionContext context =
                new PredictionContext(
                        event,
                        previousEvent,
                        previousPrediction != null
                                ? getDoubleFactor(previousPrediction.getFactors(), "destinationLat")
                                : null,
                        previousPrediction != null
                                ? getDoubleFactor(previousPrediction.getFactors(), "destinationLon")
                                : null,
                        previousPrediction != null ? previousPrediction.getEta() : null,
                        eventCount);

        // Compute prediction
        PredictionResult result = heuristicsService.computePrediction(context);

        // Save prediction
        ShipmentPrediction prediction = createPrediction(event, result, eventCount);
        prediction = predictionRepository.save(prediction);
        predictionsGeneratedCounter.increment();

        log.info(
                "Generated prediction for shipment {}: risk={}, anomaly={}",
                event.shipmentId(),
                result.delayRisk(),
                result.anomalyDetected());

        // Generate alerts if needed
        generateAlerts(prediction, result);

        // Publish prediction to Kafka
        publishPrediction(prediction);

        return prediction;
    }

    private ShipmentPrediction createPrediction(
            TrackingEvent event, PredictionResult result, int eventCount) {
        ShipmentPrediction prediction = new ShipmentPrediction();
        prediction.setTenantId(event.tenantId());
        prediction.setShipmentId(event.shipmentId());
        prediction.setEventId(event.eventId());
        prediction.setEta(result.eta());
        prediction.setEtaConfidence(result.etaConfidence());
        prediction.setDelayProbability(result.delayProbability());
        prediction.setDelayRisk(result.delayRisk());
        prediction.setAnomalyDetected(result.anomalyDetected());
        prediction.setAnomalyFlags(result.anomalyFlags());
        prediction.setDistanceRemainingKm(result.distanceRemainingKm());
        prediction.setAverageSpeedKmph(result.averageSpeedKmph());
        prediction.setDwellTimeHours(result.dwellTimeHours());

        Map<String, Object> factors = new HashMap<>(result.factors());
        factors.put("eventCount", eventCount);
        prediction.setFactors(factors);

        return prediction;
    }

    private void generateAlerts(ShipmentPrediction prediction, PredictionResult result) {
        // Alert for high delay risk
        if (result.delayRisk() == DelayRisk.HIGH) {
            Alert alert =
                    createAlert(
                            prediction,
                            AlertType.DELAY_RISK_HIGH,
                            Severity.WARNING,
                            String.format(
                                    "High delay risk detected for shipment. Delay probability: %.1f%%",
                                    result.delayProbability() * 100));
            alertRepository.save(alert);
            alertsGeneratedCounter.increment();
            publishAlert(alert);
        }

        // Alert for anomaly detection
        if (result.anomalyDetected()) {
            for (String anomalyFlag : result.anomalyFlags()) {
                AlertType alertType = mapAnomalyToAlertType(anomalyFlag);
                Severity severity = mapAnomalyToSeverity(anomalyFlag);

                Alert alert =
                        createAlert(
                                prediction,
                                alertType,
                                severity,
                                String.format("Anomaly detected: %s", anomalyFlag));
                alert.setDetails(Map.of("anomalyFlag", anomalyFlag, "factors", result.factors()));
                alertRepository.save(alert);
                alertsGeneratedCounter.increment();
                publishAlert(alert);
            }
        }
    }

    private Alert createAlert(
            ShipmentPrediction prediction, AlertType type, Severity severity, String message) {
        Alert alert = new Alert();
        alert.setTenantId(prediction.getTenantId());
        alert.setShipmentId(prediction.getShipmentId());
        alert.setPredictionId(prediction.getId());
        alert.setAlertType(type);
        alert.setSeverity(severity);
        alert.setMessage(message);
        return alert;
    }

    private AlertType mapAnomalyToAlertType(String anomalyFlag) {
        return switch (anomalyFlag) {
            case "EXCESSIVE_SPEED", "UNUSUALLY_SLOW" -> AlertType.SPEED_ANOMALY;
            case "EXCESSIVE_DWELL" -> AlertType.EXCESSIVE_DWELL;
            case "ROUTE_DEVIATION" -> AlertType.ROUTE_DEVIATION;
            default -> AlertType.ANOMALY_DETECTED;
        };
    }

    private Severity mapAnomalyToSeverity(String anomalyFlag) {
        return switch (anomalyFlag) {
            case "EXCESSIVE_DWELL", "EXCEPTION_REPORTED" -> Severity.CRITICAL;
            case "DELAY_REPORTED" -> Severity.WARNING;
            default -> Severity.INFO;
        };
    }

    private void publishPrediction(ShipmentPrediction prediction) {
        Map<String, Object> message =
                Map.of(
                        "predictionId", prediction.getId().toString(),
                        "tenantId", prediction.getTenantId().toString(),
                        "shipmentId", prediction.getShipmentId().toString(),
                        "eventId", prediction.getEventId().toString(),
                        "eta", prediction.getEta() != null ? prediction.getEta().toString() : null,
                        "delayProbability", prediction.getDelayProbability(),
                        "delayRisk", prediction.getDelayRisk().name(),
                        "anomalyDetected", prediction.isAnomalyDetected(),
                        "createdAt", prediction.getCreatedAt().toString());

        kafkaTemplate.send(
                KafkaTopics.TRACKING_PREDICTIONS, prediction.getShipmentId().toString(), message);
    }

    private void publishAlert(Alert alert) {
        AlertDto.AlertMessage message =
                new AlertDto.AlertMessage(
                        alert.getId(),
                        alert.getTenantId(),
                        alert.getShipmentId(),
                        alert.getAlertType().name(),
                        alert.getSeverity().name(),
                        alert.getMessage(),
                        alert.getCreatedAt());

        kafkaTemplate.send(KafkaTopics.TRACKING_ALERTS, alert.getShipmentId().toString(), message);
        log.info("Published alert {} for shipment {}", alert.getId(), alert.getShipmentId());
    }

    private Double getDoubleFactor(Map<String, Object> factors, String key) {
        if (factors == null) return null;
        Object value = factors.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return null;
    }

    private int getIntFactor(Map<String, Object> factors, String key, int defaultValue) {
        if (factors == null) return defaultValue;
        Object value = factors.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }
}
