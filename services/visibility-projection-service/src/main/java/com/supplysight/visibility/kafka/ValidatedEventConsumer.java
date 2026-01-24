package com.supplysight.visibility.kafka;

import com.supplysight.common.event.TrackingEvent;
import com.supplysight.common.kafka.KafkaTopics;
import com.supplysight.visibility.service.VisibilityProjectionService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Kafka consumer for validated tracking events.
 * Projects events to materialized views.
 */
@Component
public class ValidatedEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(ValidatedEventConsumer.class);

    private final VisibilityProjectionService projectionService;
    private final Counter eventsReceivedCounter;
    private final Counter eventsProcessedCounter;
    private final Counter eventsFailedCounter;

    public ValidatedEventConsumer(VisibilityProjectionService projectionService, MeterRegistry meterRegistry) {
        this.projectionService = projectionService;

        this.eventsReceivedCounter = Counter.builder("kafka.validated.events.received")
                .description("Number of validated events received")
                .register(meterRegistry);
        this.eventsProcessedCounter = Counter.builder("kafka.validated.events.processed")
                .description("Number of validated events successfully processed")
                .register(meterRegistry);
        this.eventsFailedCounter = Counter.builder("kafka.validated.events.failed")
                .description("Number of validated events that failed processing")
                .register(meterRegistry);
    }

    @KafkaListener(
            topics = KafkaTopics.TRACKING_EVENTS_VALIDATED,
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeValidatedEvent(
            @Payload TrackingEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment
    ) {
        eventsReceivedCounter.increment();

        // Set MDC context
        String correlationId = event.metadata() != null && event.metadata().correlationId() != null
                ? event.metadata().correlationId().toString()
                : UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        MDC.put("tenantId", event.tenantId() != null ? event.tenantId().toString() : "unknown");

        log.info("Received validated event: {} for shipment {} from partition {} offset {}",
                event.eventId(), event.shipmentId(), partition, offset);

        try {
            projectionService.projectEvent(event);
            eventsProcessedCounter.increment();
            log.debug("Successfully projected event {}", event.eventId());
            acknowledgment.acknowledge();
        } catch (Exception e) {
            eventsFailedCounter.increment();
            log.error("Failed to project event {}: {}", event.eventId(), e.getMessage(), e);
            // Still acknowledge to avoid infinite retry
            // In production, consider sending to DLQ
            acknowledgment.acknowledge();
        } finally {
            MDC.clear();
        }
    }
}
