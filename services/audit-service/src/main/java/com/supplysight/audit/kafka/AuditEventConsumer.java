package com.supplysight.audit.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.supplysight.audit.entity.AuditLog;
import com.supplysight.audit.repository.AuditLogRepository;
import com.supplysight.common.event.AuditEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Kafka consumer for audit events. Persists all audit events to the database. */
@Component
public class AuditEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(AuditEventConsumer.class);

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;
    private final Counter eventsProcessedCounter;
    private final Counter eventsFailedCounter;
    private final Counter eventsDuplicateCounter;

    public AuditEventConsumer(
            AuditLogRepository auditLogRepository,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
        this.eventsProcessedCounter =
                Counter.builder("audit.events.processed")
                        .description("Number of audit events processed")
                        .register(meterRegistry);
        this.eventsFailedCounter =
                Counter.builder("audit.events.failed")
                        .description("Number of audit events failed")
                        .register(meterRegistry);
        this.eventsDuplicateCounter =
                Counter.builder("audit.events.duplicate")
                        .description("Number of duplicate audit events")
                        .register(meterRegistry);
    }

    @KafkaListener(topics = "${audit.topics.audit}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeAuditEvent(String message) {
        try {
            AuditEvent event = objectMapper.readValue(message, AuditEvent.class);

            // Check for duplicate
            if (auditLogRepository.findByEventId(event.eventId()).isPresent()) {
                log.debug("Duplicate audit event: {}", event.eventId());
                eventsDuplicateCounter.increment();
                return;
            }

            // Persist audit log
            AuditLog auditLog =
                    AuditLog.builder()
                            .eventId(event.eventId())
                            .tenantId(event.tenantId())
                            .userId(event.userId())
                            .username(event.username())
                            .action(event.action())
                            .resourceType(event.resourceType())
                            .resourceId(event.resourceId())
                            .eventTime(event.timestamp())
                            .sourceIp(event.sourceIp())
                            .userAgent(event.userAgent())
                            .details(event.details())
                            .correlationId(event.correlationId())
                            .build();

            auditLogRepository.save(auditLog);
            eventsProcessedCounter.increment();

            log.debug(
                    "Persisted audit event: {} - {} - {}",
                    event.eventId(),
                    event.action(),
                    event.resourceType());

        } catch (Exception e) {
            log.error("Failed to process audit event: {}", e.getMessage(), e);
            eventsFailedCounter.increment();
        }
    }
}
