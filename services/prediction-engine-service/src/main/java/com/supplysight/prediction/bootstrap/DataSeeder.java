package com.supplysight.prediction.bootstrap;

import com.supplysight.prediction.entity.ShipmentPrediction;
import com.supplysight.prediction.entity.ShipmentPrediction.DelayRisk;
import com.supplysight.prediction.repository.ShipmentPredictionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@Profile("!prod")
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final ShipmentPredictionRepository repository;

    private static final UUID DEMO_TENANT_ID = UUID.fromString("9163981b-29ca-4765-b5ef-a1e838db11d2");

    // Fixed UUIDs matching Visibility Service
    public static final UUID SHIPMENT_ID_1 = UUID.fromString("11111111-1111-1111-1111-111111111111");
    public static final UUID SHIPMENT_ID_2 = UUID.fromString("22222222-2222-2222-2222-222222222222");
    public static final UUID SHIPMENT_ID_3 = UUID.fromString("33333333-3333-3333-3333-333333333333");
    public static final UUID SHIPMENT_ID_4 = UUID.fromString("44444444-4444-4444-4444-444444444444");
    public static final UUID SHIPMENT_ID_5 = UUID.fromString("55555555-5555-5555-5555-555555555555");

    public DataSeeder(ShipmentPredictionRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("Checking for demo predictions...");

        if (repository.count() > 0) {
            log.info("Predictions already exist. Skipping seed.");
            return;
        }

        log.info("No predictions found. Seeding demo data for Tenant {}", DEMO_TENANT_ID);

        List<ShipmentPrediction> predictions = Arrays.asList(
                createPrediction(SHIPMENT_ID_1, 2, 0.1, DelayRisk.LOW, false),
                createPrediction(SHIPMENT_ID_2, -1, 0.0, DelayRisk.LOW, false),
                createPrediction(SHIPMENT_ID_3, 4, 0.8, DelayRisk.HIGH, true),
                createPrediction(SHIPMENT_ID_4, 3, 0.2, DelayRisk.LOW, false),
                createPrediction(SHIPMENT_ID_5, 5, 0.05, DelayRisk.LOW, false));

        repository.saveAll(predictions);
        log.info("Created {} demo predictions.", predictions.size());
    }

    private ShipmentPrediction createPrediction(UUID shipmentId, int daysToEta, Double delayProb, DelayRisk risk,
            boolean anomaly) {
        ShipmentPrediction p = new ShipmentPrediction();
        p.setTenantId(DEMO_TENANT_ID);
        p.setShipmentId(shipmentId);
        p.setEventId(UUID.randomUUID());
        p.setEta(Instant.now().plus(daysToEta, ChronoUnit.DAYS));
        p.setEtaConfidence(0.95);
        p.setDelayProbability(delayProb);
        p.setDelayRisk(risk);
        p.setAnomalyDetected(anomaly);

        if (anomaly) {
            p.setAnomalyFlags(Collections.singletonList("DELAY_REPORTED"));
        } else {
            p.setAnomalyFlags(Collections.emptyList());
        }

        p.setDistanceRemainingKm(100.0 * (daysToEta > 0 ? daysToEta : 0));
        p.setAverageSpeedKmph(60.0);
        p.setDwellTimeHours(0.0);
        p.setModelVersion("v1.0-demo");

        // Populate factors to prevent frontend crash
        Map<String, Object> factors = new HashMap<>();
        factors.put("distanceRemaining", p.getDistanceRemainingKm());
        factors.put("averageSpeed", p.getAverageSpeedKmph());
        factors.put("historicalOnTime", 0.98);
        factors.put("weatherImpact", 0.05);
        p.setFactors(factors);

        return p;
    }
}
