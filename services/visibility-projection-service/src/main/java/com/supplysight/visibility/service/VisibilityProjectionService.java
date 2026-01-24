package com.supplysight.visibility.service;

import com.supplysight.common.event.TrackingEvent;
import com.supplysight.visibility.entity.ShipmentCurrentState;
import com.supplysight.visibility.entity.ShipmentTimeline;
import com.supplysight.visibility.repository.ShipmentCurrentStateRepository;
import com.supplysight.visibility.repository.ShipmentTimelineRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Service for projecting tracking events into materialized views.
 * Handles out-of-order events by using eventTime for ordering.
 */
@Service
public class VisibilityProjectionService {

    private static final Logger log = LoggerFactory.getLogger(VisibilityProjectionService.class);

    private final ShipmentCurrentStateRepository currentStateRepository;
    private final ShipmentTimelineRepository timelineRepository;
    private final Counter eventsProjectedCounter;
    private final Counter stateUpdatesCounter;
    private final Counter outOfOrderEventsCounter;

    public VisibilityProjectionService(
            ShipmentCurrentStateRepository currentStateRepository,
            ShipmentTimelineRepository timelineRepository,
            MeterRegistry meterRegistry
    ) {
        this.currentStateRepository = currentStateRepository;
        this.timelineRepository = timelineRepository;

        this.eventsProjectedCounter = Counter.builder("visibility.events.projected")
                .description("Number of events projected to views")
                .register(meterRegistry);
        this.stateUpdatesCounter = Counter.builder("visibility.state.updates")
                .description("Number of current state updates")
                .register(meterRegistry);
        this.outOfOrderEventsCounter = Counter.builder("visibility.events.out_of_order")
                .description("Number of out-of-order events received")
                .register(meterRegistry);
    }

    /**
     * Project a validated tracking event.
     * Updates current state and appends to timeline.
     */
    @Transactional
    @CacheEvict(value = "shipment-current-state", key = "#event.tenantId() + ':' + #event.shipmentId()")
    public void projectEvent(TrackingEvent event) {
        log.debug("Projecting event {} for shipment {}", event.eventId(), event.shipmentId());

        // Add to timeline (always, even for out-of-order events)
        if (!timelineRepository.existsByEventId(event.eventId())) {
            addToTimeline(event);
        } else {
            log.debug("Event {} already exists in timeline, skipping", event.eventId());
        }

        // Update current state (only if this event is newer)
        updateCurrentState(event);

        eventsProjectedCounter.increment();
        log.info("Projected event {} for shipment {}", event.eventId(), event.shipmentId());
    }

    private void addToTimeline(TrackingEvent event) {
        ShipmentTimeline timeline = new ShipmentTimeline();
        timeline.setTenantId(event.tenantId());
        timeline.setShipmentId(event.shipmentId());
        timeline.setEventId(event.eventId());
        timeline.setEventType(event.eventType());
        timeline.setEventTime(event.eventTime());
        timeline.setSource(event.source());

        if (event.location() != null) {
            timeline.setLocationLat(event.location().lat());
            timeline.setLocationLon(event.location().lon());
            timeline.setLocationHubCode(event.location().hubCode());
        }

        timeline.setPayload(event.payload());

        timelineRepository.save(timeline);
        log.debug("Added event {} to timeline for shipment {}", event.eventId(), event.shipmentId());
    }

    private void updateCurrentState(TrackingEvent event) {
        Optional<ShipmentCurrentState> existingOpt = currentStateRepository
                .findByTenantIdAndShipmentId(event.tenantId(), event.shipmentId());

        if (existingOpt.isPresent()) {
            ShipmentCurrentState existing = existingOpt.get();
            
            // Check if this event is newer (handle out-of-order)
            if (!existing.shouldUpdateFrom(event.eventTime())) {
                log.debug("Out-of-order event {} for shipment {} (eventTime: {}, lastEventTime: {})",
                        event.eventId(), event.shipmentId(), event.eventTime(), existing.getLastEventTime());
                outOfOrderEventsCounter.increment();
                
                // Still increment event count
                existing.incrementEventCount();
                currentStateRepository.save(existing);
                return;
            }

            // Update existing state
            updateExistingState(existing, event);
            currentStateRepository.save(existing);
            stateUpdatesCounter.increment();
        } else {
            // Create new state
            ShipmentCurrentState newState = createNewState(event);
            currentStateRepository.save(newState);
            stateUpdatesCounter.increment();
        }
    }

    private void updateExistingState(ShipmentCurrentState state, TrackingEvent event) {
        state.setStatus(mapEventTypeToStatus(event.eventType()));
        state.setLastEventId(event.eventId());
        state.setLastEventTime(event.eventTime());
        state.setLastEventType(event.eventType());
        state.incrementEventCount();

        if (event.location() != null) {
            state.setLocationLat(event.location().lat());
            state.setLocationLon(event.location().lon());
            state.setLocationHubCode(event.location().hubCode());

            // Set origin on first location update if not set
            if (state.getOriginLat() == null && event.location().lat() != null) {
                state.setOriginLat(event.location().lat());
                state.setOriginLon(event.location().lon());
            }
        }
    }

    private ShipmentCurrentState createNewState(TrackingEvent event) {
        ShipmentCurrentState state = new ShipmentCurrentState();
        state.setTenantId(event.tenantId());
        state.setShipmentId(event.shipmentId());
        state.setStatus(mapEventTypeToStatus(event.eventType()));
        state.setLastEventId(event.eventId());
        state.setLastEventTime(event.eventTime());
        state.setLastEventType(event.eventType());
        state.setEventCount(1);

        if (event.location() != null) {
            state.setLocationLat(event.location().lat());
            state.setLocationLon(event.location().lon());
            state.setLocationHubCode(event.location().hubCode());
            state.setOriginLat(event.location().lat());
            state.setOriginLon(event.location().lon());
        }

        return state;
    }

    /**
     * Map event type to shipment status.
     */
    private String mapEventTypeToStatus(String eventType) {
        return switch (eventType) {
            case "CREATED" -> "CREATED";
            case "PICKED_UP" -> "PICKED_UP";
            case "IN_TRANSIT" -> "IN_TRANSIT";
            case "AT_HUB" -> "AT_HUB";
            case "OUT_FOR_DELIVERY" -> "OUT_FOR_DELIVERY";
            case "DELIVERED" -> "DELIVERED";
            case "DELAYED" -> "DELAYED";
            case "EXCEPTION" -> "EXCEPTION";
            case "RETURNED" -> "RETURNED";
            case "CANCELLED" -> "CANCELLED";
            default -> eventType;
        };
    }
}
