package com.supplysight.generator.service;

import com.github.javafaker.Faker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Service for generating realistic sample data.
 */
@Service
public class DataGeneratorService {

    private static final Logger log = LoggerFactory.getLogger(DataGeneratorService.class);
    private final Faker faker = new Faker();

    @Value("${generator.events-per-shipment:10}")
    private int eventsPerShipment;

    // Indian cities with coordinates
    private static final Map<String, double[]> INDIAN_CITIES = Map.of(
            "Mumbai", new double[] { 19.0760, 72.8777 },
            "Delhi", new double[] { 28.6139, 77.2090 },
            "Bangalore", new double[] { 12.9716, 77.5946 },
            "Hyderabad", new double[] { 17.3850, 78.4867 },
            "Chennai", new double[] { 13.0827, 80.2707 },
            "Kolkata", new double[] { 22.5726, 88.3639 },
            "Pune", new double[] { 18.5204, 73.8567 },
            "Ahmedabad", new double[] { 23.0225, 72.5714 });

    private static final List<String> EVENT_TYPES = List.of(
            "CREATED", "PICKED_UP", "IN_TRANSIT", "AT_HUB", "OUT_FOR_DELIVERY", "DELIVERED", "EXCEPTION");

    private static final List<String> EVENT_SOURCES = List.of(
            "GPS_DEVICE", "SCANNER", "MOBILE_APP", "API_WEBHOOK");

    /**
     * Generate a shipment with realistic data.
     */
    public ShipmentData generateShipment(UUID tenantId) {
        String originCity = getRandomCity();
        String destinationCity = getRandomCity();
        while (destinationCity.equals(originCity)) {
            destinationCity = getRandomCity();
        }

        double[] originCoords = INDIAN_CITIES.get(originCity);
        double[] destCoords = INDIAN_CITIES.get(destinationCity);

        return ShipmentData.builder()
                .shipmentId(UUID.randomUUID())
                .tenantId(tenantId)
                .trackingNumber(faker.code().ean13())
                .originCity(originCity)
                .destinationCity(destinationCity)
                .originLat(originCoords[0])
                .originLon(originCoords[1])
                .destinationLat(destCoords[0])
                .destinationLon(destCoords[1])
                .weightKg(ThreadLocalRandom.current().nextDouble(1.0, 50.0))
                .value(ThreadLocalRandom.current().nextDouble(100.0, 10000.0))
                .createdAt(Instant.now().minusSeconds(ThreadLocalRandom.current().nextLong(86400, 604800)))
                .build();
    }

    /**
     * Generate tracking events for a shipment.
     */
    public List<EventData> generateEventsForShipment(ShipmentData shipment) {
        List<EventData> events = new ArrayList<>();
        Instant currentTime = shipment.getCreatedAt();
        String currentCity = shipment.getOriginCity();
        double currentLat = shipment.getOriginLat();
        double currentLon = shipment.getOriginLon();

        // Calculate distance and estimated transit time
        double distance = calculateDistance(
                shipment.getOriginLat(), shipment.getOriginLon(),
                shipment.getDestinationLat(), shipment.getDestinationLon());
        long estimatedDurationHours = (long) (distance / 60.0); // Assume 60 km/h average

        int eventCount = 0;
        for (String eventType : EVENT_TYPES) {
            if (eventCount >= eventsPerShipment)
                break;

            // Progress towards destination
            if (eventType.equals("IN_TRANSIT") && eventCount < eventsPerShipment - 2) {
                // Generate multiple IN_TRANSIT events
                int transitEvents = Math.min(3, eventsPerShipment - eventCount - 2);
                for (int i = 0; i < transitEvents; i++) {
                    double progress = (double) (i + 1) / (transitEvents + 1);
                    currentLat = shipment.getOriginLat() +
                            (shipment.getDestinationLat() - shipment.getOriginLat()) * progress;
                    currentLon = shipment.getOriginLon() +
                            (shipment.getDestinationLon() - shipment.getOriginLon()) * progress;
                    currentCity = getNearestCity(currentLat, currentLon);

                    EventData event = EventData.builder()
                            .eventId(UUID.randomUUID())
                            .shipmentId(shipment.getShipmentId())
                            .tenantId(shipment.getTenantId())
                            .eventType("IN_TRANSIT")
                            .eventTime(currentTime
                                    .plusSeconds(estimatedDurationHours * 3600 / (transitEvents + 1) * (i + 1)))
                            .source(getRandomElement(EVENT_SOURCES))
                            .lat(currentLat + ThreadLocalRandom.current().nextDouble(-0.1, 0.1))
                            .lon(currentLon + ThreadLocalRandom.current().nextDouble(-0.1, 0.1))
                            .hubCode(currentCity.substring(0, 3).toUpperCase() + "-HUB-" +
                                    String.format("%02d", ThreadLocalRandom.current().nextInt(1, 10)))
                            .payload(Map.of(
                                    "speedKmph", ThreadLocalRandom.current().nextInt(40, 80),
                                    "heading", ThreadLocalRandom.current().nextInt(0, 360)))
                            .build();
                    events.add(event);
                    eventCount++;
                }
                continue;
            }

            // Update location for other events
            if (eventType.equals("AT_HUB") || eventType.equals("OUT_FOR_DELIVERY")) {
                currentCity = getNearestCity(currentLat, currentLon);
                double[] cityCoords = INDIAN_CITIES.get(currentCity);
                currentLat = cityCoords[0];
                currentLon = cityCoords[1];
            }

            EventData event = EventData.builder()
                    .eventId(UUID.randomUUID())
                    .shipmentId(shipment.getShipmentId())
                    .tenantId(shipment.getTenantId())
                    .eventType(eventType)
                    .eventTime(currentTime.plusSeconds(estimatedDurationHours * 3600 * eventCount / eventsPerShipment))
                    .source(getRandomElement(EVENT_SOURCES))
                    .lat(currentLat)
                    .lon(currentLon)
                    .hubCode(eventType.equals("AT_HUB") || eventType.equals("OUT_FOR_DELIVERY")
                            ? currentCity.substring(0, 3).toUpperCase() + "-HUB-01"
                            : null)
                    .payload(generatePayloadForEvent(eventType))
                    .build();

            events.add(event);
            eventCount++;
        }

        return events;
    }

    private Map<String, Object> generatePayloadForEvent(String eventType) {
        Map<String, Object> payload = new HashMap<>();
        switch (eventType) {
            case "CREATED":
                payload.put("createdBy", faker.name().fullName());
                break;
            case "PICKED_UP":
                payload.put("driverName", faker.name().fullName());
                payload.put("vehicleNumber", faker.regexify("[A-Z]{2}[0-9]{2}[A-Z]{2}[0-9]{4}"));
                break;
            case "IN_TRANSIT":
                payload.put("speedKmph", ThreadLocalRandom.current().nextInt(40, 80));
                payload.put("heading", ThreadLocalRandom.current().nextInt(0, 360));
                break;
            case "AT_HUB":
                payload.put("hubName", faker.company().name() + " Hub");
                payload.put("dwellTimeMinutes", ThreadLocalRandom.current().nextInt(30, 240));
                break;
            case "OUT_FOR_DELIVERY":
                payload.put("driverName", faker.name().fullName());
                payload.put("estimatedDeliveryTime",
                        Instant.now().plus(2, java.time.temporal.ChronoUnit.HOURS).toString());
                break;
            case "DELIVERED":
                payload.put("deliveredTo", faker.name().fullName());
                payload.put("signature", faker.regexify("[A-Z]{6,10}"));
                break;
            case "EXCEPTION":
                payload.put("reason", getRandomElement(List.of(
                        "Address not found", "Recipient unavailable", "Damaged package", "Weather delay")));
                break;
        }
        return payload;
    }

    private String getRandomCity() {
        return getRandomElement(new ArrayList<>(INDIAN_CITIES.keySet()));
    }

    private String getNearestCity(double lat, double lon) {
        String nearest = null;
        double minDistance = Double.MAX_VALUE;

        for (Map.Entry<String, double[]> entry : INDIAN_CITIES.entrySet()) {
            double distance = calculateDistance(lat, lon, entry.getValue()[0], entry.getValue()[1]);
            if (distance < minDistance) {
                minDistance = distance;
                nearest = entry.getKey();
            }
        }
        return nearest != null ? nearest : "Mumbai";
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        // Haversine formula
        final int R = 6371; // Earth radius in km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                        * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private <T> T getRandomElement(List<T> list) {
        return list.get(ThreadLocalRandom.current().nextInt(list.size()));
    }

    // Data classes
    @lombok.Data
    @lombok.Builder
    public static class ShipmentData {
        private UUID shipmentId;
        private UUID tenantId;
        private String trackingNumber;
        private String originCity;
        private String destinationCity;
        private double originLat;
        private double originLon;
        private double destinationLat;
        private double destinationLon;
        private double weightKg;
        private double value;
        private Instant createdAt;
    }

    @lombok.Data
    @lombok.Builder
    public static class EventData {
        private UUID eventId;
        private UUID shipmentId;
        private UUID tenantId;
        private String eventType;
        private Instant eventTime;
        private String source;
        private Double lat;
        private Double lon;
        private String hubCode;
        private Map<String, Object> payload;
    }
}
