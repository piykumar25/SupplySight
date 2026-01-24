package com.supplysight.generator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

/**
 * Service for ingesting generated data into the platform via API Gateway.
 */
@Service
public class DataIngestionService {

    private static final Logger log = LoggerFactory.getLogger(DataIngestionService.class);

    @Value("${gateway.url}")
    private String gatewayUrl;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WebClient.Builder webClientBuilder;

    private WebClient webClient;
    private String accessToken;

    public void setAccessToken(String token) {
        this.accessToken = token;
        this.webClient = webClientBuilder
                .baseUrl(gatewayUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
    }

    public void ingestShipment(DataGeneratorService.ShipmentData shipment) {
        if (webClient == null) {
            log.warn("WebClient not initialized. Call setAccessToken first.");
            return;
        }

        Map<String, Object> shipmentData = Map.of(
                "trackingNumber", shipment.getTrackingNumber(),
                "originCity", shipment.getOriginCity(),
                "destinationCity", shipment.getDestinationCity(),
                "weightKg", shipment.getWeightKg(),
                "value", shipment.getValue()
        );

        try {
            webClient.post()
                    .uri("/api/v1/tracking/shipments")
                    .bodyValue(shipmentData)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(10))
                    .doOnSuccess(response -> log.debug("Ingested shipment: {}", shipment.getShipmentId()))
                    .doOnError(error -> log.error("Failed to ingest shipment: {}", error.getMessage()))
                    .block();
        } catch (Exception e) {
            log.error("Error ingesting shipment {}: {}", shipment.getShipmentId(), e.getMessage());
        }
    }

    public void ingestEvent(DataGeneratorService.EventData event) {
        if (webClient == null) {
            log.warn("WebClient not initialized. Call setAccessToken first.");
            return;
        }

        Map<String, Object> eventData = new java.util.HashMap<>();
        eventData.put("eventId", event.getEventId().toString());
        eventData.put("tenantId", event.getTenantId().toString());
        eventData.put("shipmentId", event.getShipmentId().toString());
        eventData.put("eventType", event.getEventType());
        eventData.put("eventTime", event.getEventTime().toString());
        eventData.put("source", event.getSource());
        
        if (event.getLat() != null && event.getLon() != null) {
            Map<String, Object> location = new java.util.HashMap<>();
            location.put("lat", event.getLat());
            location.put("lon", event.getLon());
            if (event.getHubCode() != null) {
                location.put("hubCode", event.getHubCode());
            }
            eventData.put("location", location);
        }
        
        if (event.getPayload() != null) {
            eventData.put("payload", event.getPayload());
        }

        try {
            webClient.post()
                    .uri("/api/v1/events/ingest")
                    .bodyValue(eventData)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(10))
                    .doOnSuccess(response -> log.debug("Ingested event: {} for shipment: {}", 
                            event.getEventId(), event.getShipmentId()))
                    .doOnError(error -> log.error("Failed to ingest event: {}", error.getMessage()))
                    .block();
        } catch (Exception e) {
            log.error("Error ingesting event {}: {}", event.getEventId(), e.getMessage());
        }
    }
}
