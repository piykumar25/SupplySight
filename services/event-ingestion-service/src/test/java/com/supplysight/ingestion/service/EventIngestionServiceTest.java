package com.supplysight.ingestion.service;

import com.supplysight.common.event.TrackingEvent;
import com.supplysight.ingestion.dto.EventIngestionDto.*;
import com.supplysight.ingestion.entity.ProcessedEvent;
import com.supplysight.ingestion.entity.TrackingEventEntity;
import com.supplysight.ingestion.repository.ProcessedEventRepository;
import com.supplysight.ingestion.repository.TrackingEventRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EventIngestionService.
 */
@ExtendWith(MockitoExtension.class)
class EventIngestionServiceTest {

    @Mock
    private TrackingEventRepository trackingEventRepository;

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @Mock
    private KafkaTemplate<String, TrackingEvent> kafkaTemplate;

    @Mock
    private EventValidationService validationService;

    private EventIngestionService eventIngestionService;

    private UUID eventId;
    private UUID tenantId;
    private UUID shipmentId;

    @BeforeEach
    void setUp() {
        eventIngestionService = new EventIngestionService(
                trackingEventRepository,
                processedEventRepository,
                kafkaTemplate,
                validationService,
                new SimpleMeterRegistry()
        );

        eventId = UUID.randomUUID();
        tenantId = UUID.randomUUID();
        shipmentId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Should ingest event successfully")
    void ingestEvent_Success() {
        // Given
        IngestRequest request = createIngestRequest();
        when(processedEventRepository.existsByEventId(eventId)).thenReturn(false);
        when(trackingEventRepository.save(any(TrackingEventEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(kafkaTemplate.send(any(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));

        // When
        IngestResponse response = eventIngestionService.ingestEvent(request, "127.0.0.1", UUID.randomUUID());

        // Then
        assertThat(response.status()).isEqualTo("ACCEPTED");
        assertThat(response.eventId()).isEqualTo(eventId);
        verify(trackingEventRepository).save(any(TrackingEventEntity.class));
        verify(processedEventRepository).save(any(ProcessedEvent.class));
        verify(kafkaTemplate).send(any(), any(), any(TrackingEvent.class));
    }

    @Test
    @DisplayName("Should detect and reject duplicate event")
    void ingestEvent_Duplicate() {
        // Given
        IngestRequest request = createIngestRequest();
        when(processedEventRepository.existsByEventId(eventId)).thenReturn(true);

        // When
        IngestResponse response = eventIngestionService.ingestEvent(request, "127.0.0.1", UUID.randomUUID());

        // Then
        assertThat(response.status()).isEqualTo("DUPLICATE");
        verify(trackingEventRepository, never()).save(any());
        verify(kafkaTemplate, never()).send(any(), any(), any());
    }

    @Test
    @DisplayName("Should store event with correct fields")
    void ingestEvent_CorrectEntityMapping() {
        // Given
        IngestRequest request = createIngestRequestWithLocation();
        when(processedEventRepository.existsByEventId(eventId)).thenReturn(false);
        when(kafkaTemplate.send(any(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));

        ArgumentCaptor<TrackingEventEntity> entityCaptor = ArgumentCaptor.forClass(TrackingEventEntity.class);
        when(trackingEventRepository.save(entityCaptor.capture()))
                .thenAnswer(inv -> inv.getArgument(0));

        // When
        eventIngestionService.ingestEvent(request, "192.168.1.1", UUID.randomUUID());

        // Then
        TrackingEventEntity savedEntity = entityCaptor.getValue();
        assertThat(savedEntity.getEventId()).isEqualTo(eventId);
        assertThat(savedEntity.getTenantId()).isEqualTo(tenantId);
        assertThat(savedEntity.getShipmentId()).isEqualTo(shipmentId);
        assertThat(savedEntity.getEventType()).isEqualTo("IN_TRANSIT");
        assertThat(savedEntity.getSource()).isEqualTo("GPS_DEVICE");
        assertThat(savedEntity.getLocationLat()).isEqualTo(12.9716);
        assertThat(savedEntity.getLocationLon()).isEqualTo(77.5946);
        assertThat(savedEntity.getLocationHubCode()).isEqualTo("BLR-HUB-01");
        assertThat(savedEntity.getSourceIp()).isEqualTo("192.168.1.1");
    }

    @Test
    @DisplayName("Should publish validated event to Kafka")
    void ingestEvent_PublishesToKafka() {
        // Given
        IngestRequest request = createIngestRequest();
        when(processedEventRepository.existsByEventId(eventId)).thenReturn(false);
        when(trackingEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ArgumentCaptor<TrackingEvent> eventCaptor = ArgumentCaptor.forClass(TrackingEvent.class);
        when(kafkaTemplate.send(any(), any(), eventCaptor.capture()))
                .thenReturn(CompletableFuture.completedFuture(null));

        // When
        eventIngestionService.ingestEvent(request, "127.0.0.1", UUID.randomUUID());

        // Then
        TrackingEvent publishedEvent = eventCaptor.getValue();
        assertThat(publishedEvent.eventId()).isEqualTo(eventId);
        assertThat(publishedEvent.tenantId()).isEqualTo(tenantId);
        assertThat(publishedEvent.shipmentId()).isEqualTo(shipmentId);
        assertThat(publishedEvent.eventType()).isEqualTo("IN_TRANSIT");
    }

    @Test
    @DisplayName("Should check if event is processed")
    void isEventProcessed_True() {
        // Given
        when(processedEventRepository.existsByEventId(eventId)).thenReturn(true);

        // When
        boolean result = eventIngestionService.isEventProcessed(eventId);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should return false for unprocessed event")
    void isEventProcessed_False() {
        // Given
        when(processedEventRepository.existsByEventId(eventId)).thenReturn(false);

        // When
        boolean result = eventIngestionService.isEventProcessed(eventId);

        // Then
        assertThat(result).isFalse();
    }

    // Helper methods

    private IngestRequest createIngestRequest() {
        return new IngestRequest(
                eventId,
                tenantId,
                shipmentId,
                "IN_TRANSIT",
                Instant.now().minusSeconds(60),
                "GPS_DEVICE",
                null,
                Map.of("speedKmph", 62),
                null
        );
    }

    private IngestRequest createIngestRequestWithLocation() {
        return new IngestRequest(
                eventId,
                tenantId,
                shipmentId,
                "IN_TRANSIT",
                Instant.now().minusSeconds(60),
                "GPS_DEVICE",
                new Location(12.9716, 77.5946, "BLR-HUB-01"),
                Map.of("speedKmph", 62),
                new Metadata(UUID.randomUUID(), Instant.now())
        );
    }
}
