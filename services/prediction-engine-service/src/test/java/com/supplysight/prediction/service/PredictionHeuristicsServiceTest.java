package com.supplysight.prediction.service;

import com.supplysight.common.event.TrackingEvent;
import com.supplysight.prediction.entity.ShipmentPrediction.DelayRisk;
import com.supplysight.prediction.service.PredictionHeuristicsService.PredictionContext;
import com.supplysight.prediction.service.PredictionHeuristicsService.PredictionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for PredictionHeuristicsService.
 */
class PredictionHeuristicsServiceTest {

    private PredictionHeuristicsService heuristicsService;

    @BeforeEach
    void setUp() {
        heuristicsService = new PredictionHeuristicsService();
        ReflectionTestUtils.setField(heuristicsService, "defaultAverageSpeed", 50.0);
        ReflectionTestUtils.setField(heuristicsService, "maxReasonableSpeed", 120.0);
        ReflectionTestUtils.setField(heuristicsService, "minSpeedThreshold", 5.0);
        ReflectionTestUtils.setField(heuristicsService, "dwellTimeWarningHours", 4);
        ReflectionTestUtils.setField(heuristicsService, "dwellTimeCriticalHours", 8);
        ReflectionTestUtils.setField(heuristicsService, "delayProbabilityWarning", 0.3);
        ReflectionTestUtils.setField(heuristicsService, "delayProbabilityHigh", 0.6);
    }

    @Test
    @DisplayName("Should compute LOW delay risk for normal event")
    void computePrediction_NormalEvent_LowRisk() {
        // Given
        TrackingEvent event = createEvent("IN_TRANSIT", Map.of("speedKmph", 60));
        PredictionContext context = new PredictionContext(event, null, null, null, null, 1);

        // When
        PredictionResult result = heuristicsService.computePrediction(context);

        // Then
        assertThat(result.delayRisk()).isEqualTo(DelayRisk.LOW);
        assertThat(result.anomalyDetected()).isFalse();
        assertThat(result.averageSpeedKmph()).isEqualTo(60.0);
    }

    @Test
    @DisplayName("Should detect HIGH delay risk for DELAYED event")
    void computePrediction_DelayedEvent_HighRisk() {
        // Given
        TrackingEvent event = createEvent("DELAYED", null);
        PredictionContext context = new PredictionContext(event, null, null, null, null, 5);

        // When
        PredictionResult result = heuristicsService.computePrediction(context);

        // Then
        assertThat(result.delayRisk()).isIn(DelayRisk.MEDIUM, DelayRisk.HIGH);
        assertThat(result.delayProbability()).isGreaterThan(0.3);
    }

    @Test
    @DisplayName("Should detect speed anomaly for excessive speed")
    void computePrediction_ExcessiveSpeed_AnomalyDetected() {
        // Given
        TrackingEvent event = createEvent("IN_TRANSIT", Map.of("speedKmph", 150));
        PredictionContext context = new PredictionContext(event, null, null, null, null, 1);

        // When
        PredictionResult result = heuristicsService.computePrediction(context);

        // Then
        assertThat(result.anomalyDetected()).isTrue();
        assertThat(result.anomalyFlags()).contains("EXCESSIVE_SPEED");
    }

    @Test
    @DisplayName("Should calculate ETA when destination is known")
    void computePrediction_WithDestination_CalculatesEta() {
        // Given
        TrackingEvent event = createEventWithLocation("IN_TRANSIT", 12.9716, 77.5946, Map.of("speedKmph", 50));
        // Destination is about 100km away
        PredictionContext context = new PredictionContext(event, null, 13.9, 77.5946, null, 1);

        // When
        PredictionResult result = heuristicsService.computePrediction(context);

        // Then
        assertThat(result.eta()).isNotNull();
        assertThat(result.distanceRemainingKm()).isGreaterThan(0);
        assertThat(result.etaConfidence()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should detect excessive dwell anomaly")
    void computePrediction_ExcessiveDwell_AnomalyDetected() {
        // Given
        Instant now = Instant.now();
        Instant tenHoursAgo = now.minusSeconds(10 * 3600);

        TrackingEvent currentEvent = createEvent("IN_TRANSIT", null);
        TrackingEvent previousEvent = new TrackingEvent(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "AT_HUB", tenHoursAgo, "SCANNER", null, null, null
        );

        PredictionContext context = new PredictionContext(currentEvent, previousEvent, null, null, null, 5);

        // When
        PredictionResult result = heuristicsService.computePrediction(context);

        // Then
        assertThat(result.anomalyDetected()).isTrue();
        assertThat(result.anomalyFlags()).contains("EXCESSIVE_DWELL");
        assertThat(result.dwellTimeHours()).isGreaterThan(8);
    }

    @Test
    @DisplayName("Should return factors map with all computed values")
    void computePrediction_ReturnsFactors() {
        // Given
        TrackingEvent event = createEvent("IN_TRANSIT", Map.of("speedKmph", 55));
        PredictionContext context = new PredictionContext(event, null, null, null, null, 1);

        // When
        PredictionResult result = heuristicsService.computePrediction(context);

        // Then
        assertThat(result.factors()).isNotEmpty();
        assertThat(result.factors()).containsKey("reportedSpeed");
        assertThat(result.factors()).containsKey("averageSpeedKmph");
        assertThat(result.factors()).containsKey("delayProbability");
        assertThat(result.factors()).containsKey("delayRisk");
    }

    // Helper methods

    private TrackingEvent createEvent(String eventType, Map<String, Object> payload) {
        return new TrackingEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                eventType,
                Instant.now(),
                "GPS_DEVICE",
                null,
                payload,
                null
        );
    }

    private TrackingEvent createEventWithLocation(String eventType, double lat, double lon, Map<String, Object> payload) {
        return new TrackingEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                eventType,
                Instant.now(),
                "GPS_DEVICE",
                new TrackingEvent.Location(lat, lon, null),
                payload,
                null
        );
    }
}
