package com.supplysight.prediction.kafka;

import com.supplysight.common.event.TrackingEvent;
import com.supplysight.common.kafka.KafkaTopics;
import com.supplysight.prediction.service.PredictionService;
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
 * Generates predictions for each event.
 */
@Component
public class ValidatedEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(ValidatedEventConsumer.class);

    private final PredictionService predictionService;
    private final Counter eventsReceivedCounter;
    private final Counter eventsProcessedCounter;
    private final Counter eventsFailedCounter;

    public ValidatedEventConsumer(PredictionService predictionService, MeterRegistry meterRegistry) {
        this.predictionService = predictionService;

        this.eventsReceivedCounter = Counter.builder("kafka.prediction.events.received")
                .description("Number of events received for prediction")
                .register(meterRegistry);
        this.eventsProcessedCounter = Counter.builder("kafka.prediction.events.processed")
                .description("Number of events successfully processed for prediction")
                .register(meterRegistry);
        this.eventsFailedCounter = Counter.builder("kafka.prediction.events.failed")
                .description("Number of events that failed prediction processing")
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

        String correlationId = event.metadata() != null && event.metadata().correlationId() != null
                ? event.metadata().correlationId().toString()
                : UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        MDC.put("tenantId", event.tenantId() != null ? event.tenantId().toString() : "unknown");

        log.info("Received event {} for prediction from partition {} offset {}",
                event.eventId(), partition, offset);

        try {
            predictionService.processEvent(event);
            eventsProcessedCounter.increment();
            acknowledgment.acknowledge();
        } catch (Exception e) {
            eventsFailedCounter.increment();
            log.error("Failed to process event {} for prediction: {}", event.eventId(), e.getMessage(), e);
            acknowledgment.acknowledge();
        } finally {
            MDC.clear();
        }
    }
}
