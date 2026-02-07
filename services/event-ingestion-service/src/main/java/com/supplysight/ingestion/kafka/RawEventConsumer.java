package com.supplysight.ingestion.kafka;

import com.supplysight.common.event.TrackingEvent;
import com.supplysight.common.kafka.KafkaTopics;
import com.supplysight.ingestion.service.EventIngestionService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for raw tracking events from external sources. Processes events from
 * tracking.events.raw topic.
 */
@Component
public class RawEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(RawEventConsumer.class);

    private final EventIngestionService eventIngestionService;
    private final Counter rawEventsReceivedCounter;
    private final Counter rawEventsProcessedCounter;
    private final Counter rawEventsFailedCounter;

    public RawEventConsumer(
            EventIngestionService eventIngestionService, MeterRegistry meterRegistry) {
        this.eventIngestionService = eventIngestionService;

        this.rawEventsReceivedCounter =
                Counter.builder("kafka.raw.events.received")
                        .description("Number of raw events received from Kafka")
                        .register(meterRegistry);
        this.rawEventsProcessedCounter =
                Counter.builder("kafka.raw.events.processed")
                        .description("Number of raw events successfully processed")
                        .register(meterRegistry);
        this.rawEventsFailedCounter =
                Counter.builder("kafka.raw.events.failed")
                        .description("Number of raw events that failed processing")
                        .register(meterRegistry);
    }

    @KafkaListener(
            topics = KafkaTopics.TRACKING_EVENTS_RAW,
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory")
    public void consumeRawEvent(
            @Payload TrackingEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {
        rawEventsReceivedCounter.increment();

        // Set MDC context for logging
        String correlationId =
                event.metadata() != null && event.metadata().correlationId() != null
                        ? event.metadata().correlationId().toString()
                        : UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        MDC.put("tenantId", event.tenantId() != null ? event.tenantId().toString() : "unknown");

        log.info(
                "Received raw event: {} from partition {} offset {}",
                event.eventId(),
                partition,
                offset);

        try {
            boolean processed = eventIngestionService.processRawEvent(event);

            if (processed) {
                rawEventsProcessedCounter.increment();
                log.debug("Raw event {} processed successfully", event.eventId());
            } else {
                log.debug("Raw event {} was duplicate or invalid", event.eventId());
            }

            // Acknowledge the message
            acknowledgment.acknowledge();

        } catch (Exception e) {
            rawEventsFailedCounter.increment();
            log.error("Failed to process raw event {}: {}", event.eventId(), e.getMessage(), e);

            // Still acknowledge to prevent infinite retry loop
            // In production, you might want to send to a DLQ instead
            acknowledgment.acknowledge();
        } finally {
            MDC.clear();
        }
    }
}
