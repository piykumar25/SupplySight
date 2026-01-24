package com.supplysight.audit.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supplysight.audit.entity.AlertHistory;
import com.supplysight.audit.repository.AlertHistoryRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Kafka consumer for alert events.
 * Persists alert history for tracking alert lifecycle.
 */
@Component
public class AlertEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(AlertEventConsumer.class);

    private final AlertHistoryRepository alertHistoryRepository;
    private final ObjectMapper objectMapper;
    private final Counter alertsProcessedCounter;
    private final Counter alertsFailedCounter;

    public AlertEventConsumer(
            AlertHistoryRepository alertHistoryRepository,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry
    ) {
        this.alertHistoryRepository = alertHistoryRepository;
        this.objectMapper = objectMapper;
        this.alertsProcessedCounter = Counter.builder("audit.alerts.processed")
                .description("Number of alerts processed")
                .register(meterRegistry);
        this.alertsFailedCounter = Counter.builder("audit.alerts.failed")
                .description("Number of alerts failed to process")
                .register(meterRegistry);
    }

    @KafkaListener(topics = "${audit.topics.alerts}", groupId = "${spring.kafka.consumer.group-id}-alerts")
    public void consumeAlertEvent(String message) {
        try {
            JsonNode alertNode = objectMapper.readTree(message);

            UUID alertId = UUID.fromString(alertNode.get("id").asText());
            
            // Check for duplicate
            if (alertHistoryRepository.findByAlertId(alertId).isPresent()) {
                log.debug("Duplicate alert event: {}", alertId);
                return;
            }

            // Extract details
            Map<String, Object> details = new HashMap<>();
            if (alertNode.has("factors")) {
                details.put("factors", objectMapper.convertValue(alertNode.get("factors"), Map.class));
            }

            AlertHistory alertHistory = AlertHistory.builder()
                    .alertId(alertId)
                    .tenantId(UUID.fromString(alertNode.get("tenantId").asText()))
                    .shipmentId(alertNode.has("shipmentId") ? 
                            UUID.fromString(alertNode.get("shipmentId").asText()) : null)
                    .alertType(alertNode.get("alertType").asText())
                    .severity(alertNode.has("severity") ? alertNode.get("severity").asText() : "MEDIUM")
                    .message(alertNode.has("message") ? alertNode.get("message").asText() : null)
                    .status("OPEN")
                    .details(details)
                    .build();

            alertHistoryRepository.save(alertHistory);
            alertsProcessedCounter.increment();

            log.info("Persisted alert: {} - {} for shipment {}", 
                    alertId, alertHistory.getAlertType(), alertHistory.getShipmentId());

        } catch (Exception e) {
            log.error("Failed to process alert event: {}", e.getMessage(), e);
            alertsFailedCounter.increment();
        }
    }
}
