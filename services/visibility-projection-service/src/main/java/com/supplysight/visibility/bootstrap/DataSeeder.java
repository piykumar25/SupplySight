package com.supplysight.visibility.bootstrap;

import com.supplysight.visibility.entity.ShipmentCurrentState;
import com.supplysight.visibility.repository.ShipmentCurrentStateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Automatically seeds demo shipments on application startup.
 * Runs only if data is missing.
 */
@Component
@Profile("!prod")
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final ShipmentCurrentStateRepository repository;

    // Must match the Tenant ID in Identity Service DataSeeder
    private static final UUID DEMO_TENANT_ID = UUID.fromString("9163981b-29ca-4765-b5ef-a1e838db11d2");

    public DataSeeder(ShipmentCurrentStateRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("Checking for demo shipments...");

        if (repository.count() > 0) {
            log.info("Shipments already exist. Skipping seed.");
            return;
        }

        log.info("No shipments found. creating demo data for Tenant {}", DEMO_TENANT_ID);

        List<ShipmentCurrentState> shipments = Arrays.asList(
                createShipment("IN_TRANSIT", "LOCATION_UPDATE", 40.7128, -74.0060, "New York, NY", "Los Angeles, CA", 2,
                        0.1),
                createShipment("DELIVERED", "DELIVERED", 34.0522, -118.2437, "Chicago, IL", "Miami, FL", -1, 0.0),
                createShipment("DELAYED", "DELAY_ALERT", 51.5074, -0.1278, "London, UK", "Paris, FR", 4, 0.8),
                createShipment("PICKED_UP", "PICKUP", 37.7749, -122.4194, "San Francisco, CA", "Seattle, WA", 3, 0.2),
                createShipment("IN_TRANSIT", "DEPARTURE", 35.6762, 139.6503, "Tokyo, JP", "Singapore, SG", 5, 0.05));

        repository.saveAll(shipments);
        log.info("Created {} demo shipments.", shipments.size());
    }

    private ShipmentCurrentState createShipment(String status, String lastEventType, Double lat, Double lon,
            String origin, String dest, int daysToEta, Double risk) {
        ShipmentCurrentState s = new ShipmentCurrentState();
        s.setTenantId(DEMO_TENANT_ID);
        s.setShipmentId(UUID.randomUUID());
        s.setStatus(status);
        s.setLastEventId(UUID.randomUUID());
        s.setLastEventTime(Instant.now().minus(1, ChronoUnit.HOURS));
        s.setLastEventType(lastEventType);

        // Location
        s.setLocationLat(lat);
        s.setLocationLon(lon);

        // Origin/Dest dummy coordinates (approx)
        s.setOriginLat(0.0);
        s.setOriginLon(0.0);
        s.setDestinationLat(0.0);
        s.setDestinationLon(0.0);

        s.setEta(Instant.now().plus(daysToEta, ChronoUnit.DAYS));
        s.setDelayProbability(risk);

        return s;
    }
}
