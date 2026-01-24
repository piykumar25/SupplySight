package com.supplysight.visibility.service;

import com.supplysight.common.event.TrackingEvent;
import com.supplysight.visibility.entity.ShipmentCurrentState;
import com.supplysight.visibility.entity.ShipmentTimeline;
import com.supplysight.visibility.repository.ShipmentCurrentStateRepository;
import com.supplysight.visibility.repository.ShipmentTimelineRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for VisibilityProjectionService.
 */
@ExtendWith(MockitoExtension.class)
class VisibilityProjectionServiceTest {

    @Mock
    private ShipmentCurrentStateRepository currentStateRepository;

    @Mock
    private ShipmentTimelineRepository timelineRepository;

    private VisibilityProjectionService projectionService;

    private UUID tenantId;
    private UUID shipmentId;
    private UUID eventId;

    @BeforeEach
    void setUp() {
        projectionService = new VisibilityProjectionService(
                currentStateRepository,
                timelineRepository,
                new SimpleMeterRegistry()
        );

        tenantId = UUID.randomUUID();
        shipmentId = UUID.randomUUID();
        eventId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Should create new current state for first event")
    void projectEvent_FirstEvent_CreatesNewState() {
        // Given
        TrackingEvent event = createEvent(Instant.now());
        when(timelineRepository.existsByEventId(eventId)).thenReturn(false);
        when(currentStateRepository.findByTenantIdAndShipmentId(tenantId, shipmentId))
                .thenReturn(Optional.empty());

        // When
        projectionService.projectEvent(event);

        // Then
        verify(timelineRepository).save(any(ShipmentTimeline.class));
        
        ArgumentCaptor<ShipmentCurrentState> stateCaptor = ArgumentCaptor.forClass(ShipmentCurrentState.class);
        verify(currentStateRepository).save(stateCaptor.capture());
        
        ShipmentCurrentState savedState = stateCaptor.getValue();
        assertThat(savedState.getShipmentId()).isEqualTo(shipmentId);
        assertThat(savedState.getTenantId()).isEqualTo(tenantId);
        assertThat(savedState.getStatus()).isEqualTo("IN_TRANSIT");
        assertThat(savedState.getEventCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should update existing state for newer event")
    void projectEvent_NewerEvent_UpdatesState() {
        // Given
        Instant oldTime = Instant.now().minusSeconds(60);
        Instant newTime = Instant.now();

        ShipmentCurrentState existingState = new ShipmentCurrentState();
        existingState.setTenantId(tenantId);
        existingState.setShipmentId(shipmentId);
        existingState.setLastEventTime(oldTime);
        existingState.setStatus("PICKED_UP");
        existingState.setEventCount(1);

        TrackingEvent event = createEvent(newTime);
        when(timelineRepository.existsByEventId(eventId)).thenReturn(false);
        when(currentStateRepository.findByTenantIdAndShipmentId(tenantId, shipmentId))
                .thenReturn(Optional.of(existingState));

        // When
        projectionService.projectEvent(event);

        // Then
        ArgumentCaptor<ShipmentCurrentState> stateCaptor = ArgumentCaptor.forClass(ShipmentCurrentState.class);
        verify(currentStateRepository).save(stateCaptor.capture());

        ShipmentCurrentState savedState = stateCaptor.getValue();
        assertThat(savedState.getStatus()).isEqualTo("IN_TRANSIT");
        assertThat(savedState.getLastEventTime()).isEqualTo(newTime);
        assertThat(savedState.getEventCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should handle out-of-order event correctly")
    void projectEvent_OutOfOrderEvent_DoesNotUpdateStatus() {
        // Given
        Instant newerTime = Instant.now();
        Instant olderTime = Instant.now().minusSeconds(60);

        ShipmentCurrentState existingState = new ShipmentCurrentState();
        existingState.setTenantId(tenantId);
        existingState.setShipmentId(shipmentId);
        existingState.setLastEventTime(newerTime);
        existingState.setStatus("DELIVERED");
        existingState.setLastEventType("DELIVERED");
        existingState.setEventCount(5);

        TrackingEvent oldEvent = createEvent(olderTime);
        when(timelineRepository.existsByEventId(eventId)).thenReturn(false);
        when(currentStateRepository.findByTenantIdAndShipmentId(tenantId, shipmentId))
                .thenReturn(Optional.of(existingState));

        // When
        projectionService.projectEvent(oldEvent);

        // Then
        ArgumentCaptor<ShipmentCurrentState> stateCaptor = ArgumentCaptor.forClass(ShipmentCurrentState.class);
        verify(currentStateRepository).save(stateCaptor.capture());

        ShipmentCurrentState savedState = stateCaptor.getValue();
        // Status should NOT be updated (still DELIVERED)
        assertThat(savedState.getStatus()).isEqualTo("DELIVERED");
        assertThat(savedState.getLastEventTime()).isEqualTo(newerTime);
        // But event count should still increment
        assertThat(savedState.getEventCount()).isEqualTo(6);
    }

    @Test
    @DisplayName("Should add event to timeline")
    void projectEvent_AddsToTimeline() {
        // Given
        TrackingEvent event = createEventWithLocation(Instant.now());
        when(timelineRepository.existsByEventId(eventId)).thenReturn(false);
        when(currentStateRepository.findByTenantIdAndShipmentId(tenantId, shipmentId))
                .thenReturn(Optional.empty());

        // When
        projectionService.projectEvent(event);

        // Then
        ArgumentCaptor<ShipmentTimeline> timelineCaptor = ArgumentCaptor.forClass(ShipmentTimeline.class);
        verify(timelineRepository).save(timelineCaptor.capture());

        ShipmentTimeline savedTimeline = timelineCaptor.getValue();
        assertThat(savedTimeline.getEventId()).isEqualTo(eventId);
        assertThat(savedTimeline.getShipmentId()).isEqualTo(shipmentId);
        assertThat(savedTimeline.getEventType()).isEqualTo("IN_TRANSIT");
        assertThat(savedTimeline.getLocationLat()).isEqualTo(12.9716);
    }

    @Test
    @DisplayName("Should skip duplicate event in timeline")
    void projectEvent_DuplicateEvent_SkipsTimeline() {
        // Given
        TrackingEvent event = createEvent(Instant.now());
        when(timelineRepository.existsByEventId(eventId)).thenReturn(true);
        when(currentStateRepository.findByTenantIdAndShipmentId(tenantId, shipmentId))
                .thenReturn(Optional.empty());

        // When
        projectionService.projectEvent(event);

        // Then
        verify(timelineRepository, never()).save(any(ShipmentTimeline.class));
        // But should still update current state
        verify(currentStateRepository).save(any(ShipmentCurrentState.class));
    }

    // Helper methods

    private TrackingEvent createEvent(Instant eventTime) {
        return new TrackingEvent(
                eventId,
                tenantId,
                shipmentId,
                "IN_TRANSIT",
                eventTime,
                "GPS_DEVICE",
                null,
                null,
                null
        );
    }

    private TrackingEvent createEventWithLocation(Instant eventTime) {
        return new TrackingEvent(
                eventId,
                tenantId,
                shipmentId,
                "IN_TRANSIT",
                eventTime,
                "GPS_DEVICE",
                new TrackingEvent.Location(12.9716, 77.5946, "BLR-HUB-01"),
                null,
                null
        );
    }
}
