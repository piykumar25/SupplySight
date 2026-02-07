package com.supplysight.ingestion.service;

import com.supplysight.common.event.TrackingEvent;
import com.supplysight.common.exception.ValidationException;
import com.supplysight.common.kafka.KafkaTopics;
import com.supplysight.ingestion.dto.EventIngestionDto;
import com.supplysight.ingestion.dto.EventIngestionDto.*;
import com.supplysight.ingestion.entity.ProcessedEvent;
import com.supplysight.ingestion.entity.ProcessedEvent.EventSource;
import com.supplysight.ingestion.entity.TrackingEventEntity;
import com.supplysight.ingestion.repository.ProcessedEventRepository;
import com.supplysight.ingestion.repository.TrackingEventRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Service for event ingestion, validation, deduplication, and publishing. */
@Service
public class EventIngestionService {

    private static final Logger log = LoggerFactory.getLogger(EventIngestionService.class);

    private final TrackingEventRepository trackingEventRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final KafkaTemplate<String, TrackingEvent> kafkaTemplate;
    private final EventValidationService validationService;
    private final Counter eventsIngestedCounter;
    private final Counter eventsDuplicatedCounter;
    private final Counter eventsRejectedCounter;

    @Value("${ingestion.late-arrival-tolerance-hours:72}")
    private int lateArrivalToleranceHours;

    public EventIngestionService(
            TrackingEventRepository trackingEventRepository,
            ProcessedEventRepository processedEventRepository,
            KafkaTemplate<String, TrackingEvent> kafkaTemplate,
            EventValidationService validationService,
            MeterRegistry meterRegistry) {
        this.trackingEventRepository = trackingEventRepository;
        this.processedEventRepository = processedEventRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.validationService = validationService;

        // Initialize metrics
        this.eventsIngestedCounter =
                Counter.builder("events.ingested")
                        .description("Number of events successfully ingested")
                        .register(meterRegistry);
        this.eventsDuplicatedCounter =
                Counter.builder("events.duplicated")
                        .description("Number of duplicate events rejected")
                        .register(meterRegistry);
        this.eventsRejectedCounter =
                Counter.builder("events.rejected")
                        .description("Number of events rejected due to validation")
                        .register(meterRegistry);
    }

    /** Ingest a single event via REST API. */
    @Transactional
    public IngestResponse ingestEvent(IngestRequest request, String sourceIp, UUID correlationId) {
        log.debug("Ingesting event: {} for shipment: {}", request.eventId(), request.shipmentId());

        // Validate the event
        try {
            validationService.validateEvent(request);
        } catch (ValidationException e) {
            log.warn("Event validation failed for {}: {}", request.eventId(), e.getMessage());
            eventsRejectedCounter.increment();
            return IngestResponse.rejected(request.eventId(), e.getMessage());
        }

        // Check for duplicate
        if (isEventProcessed(request.eventId())) {
            log.debug("Duplicate event detected: {}", request.eventId());
            eventsDuplicatedCounter.increment();
            return IngestResponse.duplicate(request.eventId());
        }

        // Store the event
        TrackingEventEntity entity = createEventEntity(request, sourceIp, correlationId);
        trackingEventRepository.save(entity);

        // Mark as processed
        markAsProcessed(request.eventId(), request.tenantId(), EventSource.REST);

        // Publish to validated events topic
        publishValidatedEvent(request, correlationId);

        eventsIngestedCounter.increment();
        log.info(
                "Event ingested successfully: {} for shipment: {}",
                request.eventId(),
                request.shipmentId());

        return IngestResponse.accepted(request.eventId());
    }

    /** Ingest a batch of events. */
    @Transactional
    public BatchIngestResponse ingestBatch(
            BatchIngestRequest request, String sourceIp, UUID correlationId) {
        log.info("Ingesting batch of {} events", request.events().size());

        List<IngestResponse> results = new ArrayList<>();
        int accepted = 0;
        int duplicates = 0;
        int rejected = 0;

        // Get existing event IDs for batch deduplication
        Set<UUID> eventIds =
                request.events().stream().map(IngestRequest::eventId).collect(Collectors.toSet());
        Set<UUID> existingIds =
                new HashSet<>(processedEventRepository.findExistingEventIds(eventIds));

        for (IngestRequest event : request.events()) {
            // Check for duplicate
            if (existingIds.contains(event.eventId())) {
                results.add(IngestResponse.duplicate(event.eventId()));
                duplicates++;
                eventsDuplicatedCounter.increment();
                continue;
            }

            // Validate
            try {
                validationService.validateEvent(event);
            } catch (ValidationException e) {
                results.add(IngestResponse.rejected(event.eventId(), e.getMessage()));
                rejected++;
                eventsRejectedCounter.increment();
                continue;
            }

            // Store and publish
            TrackingEventEntity entity = createEventEntity(event, sourceIp, correlationId);
            trackingEventRepository.save(entity);
            markAsProcessed(event.eventId(), event.tenantId(), EventSource.REST);
            publishValidatedEvent(event, correlationId);

            results.add(IngestResponse.accepted(event.eventId()));
            accepted++;
            eventsIngestedCounter.increment();
        }

        log.info(
                "Batch ingestion complete: {} accepted, {} duplicates, {} rejected",
                accepted,
                duplicates,
                rejected);

        return new BatchIngestResponse(
                request.events().size(), accepted, duplicates, rejected, results);
    }

    /** Process event from Kafka raw topic. */
    @Transactional
    public boolean processRawEvent(TrackingEvent event) {
        log.debug("Processing raw event from Kafka: {}", event.eventId());

        // Check for duplicate
        if (isEventProcessed(event.eventId())) {
            log.debug("Duplicate raw event detected: {}", event.eventId());
            eventsDuplicatedCounter.increment();
            return false;
        }

        // Convert to IngestRequest for validation
        IngestRequest request = convertToIngestRequest(event);

        try {
            validationService.validateEvent(request);
        } catch (ValidationException e) {
            log.warn("Raw event validation failed for {}: {}", event.eventId(), e.getMessage());
            eventsRejectedCounter.increment();
            return false;
        }

        // Store the event
        TrackingEventEntity entity =
                createEventEntity(
                        request,
                        null,
                        event.metadata() != null ? event.metadata().correlationId() : null);
        trackingEventRepository.save(entity);

        // Mark as processed
        markAsProcessed(event.eventId(), event.tenantId(), EventSource.KAFKA_RAW);

        // Publish to validated events topic
        publishValidatedEvent(
                request, event.metadata() != null ? event.metadata().correlationId() : null);

        eventsIngestedCounter.increment();
        log.info("Raw event processed successfully: {}", event.eventId());

        return true;
    }

    /** Check if event has already been processed. */
    public boolean isEventProcessed(UUID eventId) {
        return processedEventRepository.existsByEventId(eventId);
    }

    // Private helper methods

    private void markAsProcessed(UUID eventId, UUID tenantId, EventSource source) {
        ProcessedEvent processed = new ProcessedEvent(eventId, tenantId, source);
        processedEventRepository.save(processed);
    }

    private TrackingEventEntity createEventEntity(
            IngestRequest request, String sourceIp, UUID correlationId) {
        TrackingEventEntity entity = new TrackingEventEntity();
        entity.setEventId(request.eventId());
        entity.setTenantId(request.tenantId());
        entity.setShipmentId(request.shipmentId());
        entity.setEventType(request.eventType());
        entity.setEventTime(request.eventTime());
        entity.setSource(request.source());

        if (request.location() != null) {
            entity.setLocationLat(request.location().lat());
            entity.setLocationLon(request.location().lon());
            entity.setLocationHubCode(request.location().hubCode());
        }

        entity.setPayload(request.payload());
        entity.setSourceIp(sourceIp);
        entity.setCorrelationId(
                correlationId != null
                        ? correlationId
                        : (request.metadata() != null ? request.metadata().correlationId() : null));

        // Build metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("ingestedAt", Instant.now().toString());
        if (correlationId != null) {
            metadata.put("correlationId", correlationId.toString());
        }
        entity.setMetadata(metadata);

        return entity;
    }

    private void publishValidatedEvent(IngestRequest request, UUID correlationId) {
        TrackingEvent.Location location = null;
        if (request.location() != null) {
            location =
                    new TrackingEvent.Location(
                            request.location().lat(),
                            request.location().lon(),
                            request.location().hubCode());
        }

        TrackingEvent.EventMetadata metadata =
                new TrackingEvent.EventMetadata(
                        Instant.now(),
                        correlationId != null
                                ? correlationId
                                : (request.metadata() != null
                                        ? request.metadata().correlationId()
                                        : UUID.randomUUID()),
                        null,
                        null);

        TrackingEvent event =
                new TrackingEvent(
                        request.eventId(),
                        request.tenantId(),
                        request.shipmentId(),
                        request.eventType(),
                        request.eventTime(),
                        request.source(),
                        location,
                        request.payload(),
                        metadata);

        // Use shipmentId as key for partition ordering
        String key = request.shipmentId().toString();

        CompletableFuture<SendResult<String, TrackingEvent>> future =
                kafkaTemplate.send(KafkaTopics.TRACKING_EVENTS_VALIDATED, key, event);

        future.whenComplete(
                (result, ex) -> {
                    if (ex != null) {
                        log.error(
                                "Failed to publish validated event {}: {}",
                                request.eventId(),
                                ex.getMessage());
                    } else {
                        log.debug(
                                "Published validated event {} to partition {}",
                                request.eventId(),
                                result.getRecordMetadata().partition());
                    }
                });
    }

    private IngestRequest convertToIngestRequest(TrackingEvent event) {
        EventIngestionDto.Location location = null;
        if (event.location() != null) {
            location =
                    new EventIngestionDto.Location(
                            event.location().lat(),
                            event.location().lon(),
                            event.location().hubCode());
        }

        EventIngestionDto.Metadata metadata = null;
        if (event.metadata() != null) {
            metadata =
                    new EventIngestionDto.Metadata(
                            event.metadata().correlationId(), event.metadata().ingestedAt());
        }

        return new IngestRequest(
                event.eventId(),
                event.tenantId(),
                event.shipmentId(),
                event.eventType(),
                event.eventTime(),
                event.source(),
                location,
                event.payload(),
                metadata);
    }
}
