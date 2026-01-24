package com.supplysight.visibility.service;

import com.supplysight.common.exception.ResourceNotFoundException;
import com.supplysight.visibility.dto.ShipmentDto.*;
import com.supplysight.visibility.entity.ShipmentCurrentState;
import com.supplysight.visibility.entity.ShipmentTimeline;
import com.supplysight.visibility.repository.ShipmentCurrentStateRepository;
import com.supplysight.visibility.repository.ShipmentTimelineRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for querying shipment visibility data.
 * Provides cached read access to materialized views.
 */
@Service
@Transactional(readOnly = true)
public class ShipmentQueryService {

    private static final Logger log = LoggerFactory.getLogger(ShipmentQueryService.class);

    private final ShipmentCurrentStateRepository currentStateRepository;
    private final ShipmentTimelineRepository timelineRepository;

    public ShipmentQueryService(
            ShipmentCurrentStateRepository currentStateRepository,
            ShipmentTimelineRepository timelineRepository
    ) {
        this.currentStateRepository = currentStateRepository;
        this.timelineRepository = timelineRepository;
    }

    /**
     * Get current state for a shipment.
     */
    @Cacheable(value = "shipment-current-state", key = "#tenantId + ':' + #shipmentId")
    public CurrentStateResponse getShipmentCurrentState(UUID tenantId, UUID shipmentId) {
        log.debug("Fetching current state for shipment {} (tenant {})", shipmentId, tenantId);

        ShipmentCurrentState state = currentStateRepository
                .findByTenantIdAndShipmentId(tenantId, shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment", shipmentId));

        return toCurrentStateResponse(state);
    }

    /**
     * Get timeline for a shipment.
     */
    public TimelineResponse getShipmentTimeline(UUID tenantId, UUID shipmentId) {
        log.debug("Fetching timeline for shipment {} (tenant {})", shipmentId, tenantId);

        // Verify shipment exists
        if (!currentStateRepository.existsByTenantIdAndShipmentId(tenantId, shipmentId)) {
            throw new ResourceNotFoundException("Shipment", shipmentId);
        }

        List<ShipmentTimeline> events = timelineRepository
                .findByTenantIdAndShipmentIdOrderByEventTime(tenantId, shipmentId);

        List<TimelineEvent> timelineEvents = events.stream()
                .map(this::toTimelineEvent)
                .collect(Collectors.toList());

        return new TimelineResponse(shipmentId, tenantId, timelineEvents.size(), timelineEvents);
    }

    /**
     * List shipments for a tenant with optional filtering.
     */
    public Page<ShipmentSummary> listShipments(UUID tenantId, ShipmentQuery query) {
        log.debug("Listing shipments for tenant {} with query: {}", tenantId, query);

        Sort sort = query.sortDir() != null && query.sortDir().equalsIgnoreCase("asc")
                ? Sort.by(query.sortBy() != null ? query.sortBy() : "updatedAt").ascending()
                : Sort.by(query.sortBy() != null ? query.sortBy() : "updatedAt").descending();

        PageRequest pageRequest = PageRequest.of(
                query.page(),
                Math.min(query.size(), 100),
                sort
        );

        Page<ShipmentCurrentState> page;
        if (query.status() != null && !query.status().isBlank()) {
            page = currentStateRepository.findByTenantIdAndStatus(tenantId, query.status(), pageRequest);
        } else {
            page = currentStateRepository.findByTenantId(tenantId, pageRequest);
        }

        return page.map(this::toShipmentSummary);
    }

    /**
     * Get dashboard statistics.
     */
    public DashboardStats getDashboardStats(UUID tenantId) {
        log.debug("Fetching dashboard stats for tenant {}", tenantId);

        List<Object[]> statusCounts = currentStateRepository.countByTenantIdGroupByStatus(tenantId);

        long total = 0;
        long inTransit = 0;
        long delivered = 0;
        long delayed = 0;
        List<StatusStats> breakdown = new ArrayList<>();

        for (Object[] row : statusCounts) {
            String status = (String) row[0];
            long count = (Long) row[1];
            total += count;
            breakdown.add(new StatusStats(status, count));

            switch (status) {
                case "IN_TRANSIT", "OUT_FOR_DELIVERY" -> inTransit += count;
                case "DELIVERED" -> delivered += count;
                case "DELAYED", "EXCEPTION" -> delayed += count;
            }
        }

        return new DashboardStats(total, inTransit, delivered, delayed, breakdown);
    }

    /**
     * Get active shipments (in transit).
     */
    public List<CurrentStateResponse> getActiveShipments(UUID tenantId) {
        log.debug("Fetching active shipments for tenant {}", tenantId);

        return currentStateRepository.findActiveShipments(tenantId).stream()
                .map(this::toCurrentStateResponse)
                .collect(Collectors.toList());
    }

    // Mapping methods

    private CurrentStateResponse toCurrentStateResponse(ShipmentCurrentState state) {
        Location lastLocation = null;
        if (state.getLocationLat() != null) {
            lastLocation = new Location(
                    state.getLocationLat(),
                    state.getLocationLon(),
                    state.getLocationHubCode()
            );
        }

        Location origin = null;
        if (state.getOriginLat() != null) {
            origin = new Location(state.getOriginLat(), state.getOriginLon(), null);
        }

        Location destination = null;
        if (state.getDestinationLat() != null) {
            destination = new Location(state.getDestinationLat(), state.getDestinationLon(), null);
        }

        return new CurrentStateResponse(
                state.getShipmentId(),
                state.getTenantId(),
                state.getStatus(),
                state.getLastEventId(),
                state.getLastEventTime(),
                state.getLastEventType(),
                lastLocation,
                origin,
                destination,
                state.getEta(),
                state.getDelayProbability(),
                state.getEventCount(),
                state.getCreatedAt(),
                state.getUpdatedAt()
        );
    }

    private ShipmentSummary toShipmentSummary(ShipmentCurrentState state) {
        Location lastLocation = null;
        if (state.getLocationLat() != null) {
            lastLocation = new Location(
                    state.getLocationLat(),
                    state.getLocationLon(),
                    state.getLocationHubCode()
            );
        }

        return new ShipmentSummary(
                state.getShipmentId(),
                state.getStatus(),
                state.getLastEventType(),
                state.getLastEventTime(),
                lastLocation,
                state.getEta()
        );
    }

    private TimelineEvent toTimelineEvent(ShipmentTimeline timeline) {
        Location location = null;
        if (timeline.getLocationLat() != null) {
            location = new Location(
                    timeline.getLocationLat(),
                    timeline.getLocationLon(),
                    timeline.getLocationHubCode()
            );
        }

        return new TimelineEvent(
                timeline.getEventId(),
                timeline.getEventType(),
                timeline.getEventTime(),
                timeline.getSource(),
                location,
                timeline.getPayload()
        );
    }
}
