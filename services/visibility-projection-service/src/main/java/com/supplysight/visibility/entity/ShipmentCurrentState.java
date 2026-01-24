package com.supplysight.visibility.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Materialized view of current shipment state.
 * Updated on each new event, handles out-of-order by eventTime.
 */
@Entity
@Table(name = "shipment_current_state", schema = "visibility",
    uniqueConstraints = @UniqueConstraint(name = "uk_shipment_current_state_tenant_shipment", 
                                          columnNames = {"tenant_id", "shipment_id"}))
public class ShipmentCurrentState {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "shipment_id", nullable = false, updatable = false)
    private UUID shipmentId;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "last_event_id", nullable = false)
    private UUID lastEventId;

    @Column(name = "last_event_time", nullable = false)
    private Instant lastEventTime;

    @Column(name = "last_event_type", nullable = false, length = 50)
    private String lastEventType;

    @Column(name = "location_lat")
    private Double locationLat;

    @Column(name = "location_lon")
    private Double locationLon;

    @Column(name = "location_hub_code", length = 50)
    private String locationHubCode;

    @Column(name = "origin_lat")
    private Double originLat;

    @Column(name = "origin_lon")
    private Double originLon;

    @Column(name = "destination_lat")
    private Double destinationLat;

    @Column(name = "destination_lon")
    private Double destinationLon;

    @Column(name = "eta")
    private Instant eta;

    @Column(name = "delay_probability")
    private Double delayProbability;

    @Column(name = "event_count", nullable = false)
    private Integer eventCount = 1;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version")
    private Long version;

    public ShipmentCurrentState() {
        this.id = UUID.randomUUID();
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    /**
     * Check if this event is newer than current state (for out-of-order handling).
     */
    public boolean shouldUpdateFrom(Instant eventTime) {
        return this.lastEventTime == null || eventTime.isAfter(this.lastEventTime);
    }

    public void incrementEventCount() {
        this.eventCount++;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public UUID getShipmentId() {
        return shipmentId;
    }

    public void setShipmentId(UUID shipmentId) {
        this.shipmentId = shipmentId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public UUID getLastEventId() {
        return lastEventId;
    }

    public void setLastEventId(UUID lastEventId) {
        this.lastEventId = lastEventId;
    }

    public Instant getLastEventTime() {
        return lastEventTime;
    }

    public void setLastEventTime(Instant lastEventTime) {
        this.lastEventTime = lastEventTime;
    }

    public String getLastEventType() {
        return lastEventType;
    }

    public void setLastEventType(String lastEventType) {
        this.lastEventType = lastEventType;
    }

    public Double getLocationLat() {
        return locationLat;
    }

    public void setLocationLat(Double locationLat) {
        this.locationLat = locationLat;
    }

    public Double getLocationLon() {
        return locationLon;
    }

    public void setLocationLon(Double locationLon) {
        this.locationLon = locationLon;
    }

    public String getLocationHubCode() {
        return locationHubCode;
    }

    public void setLocationHubCode(String locationHubCode) {
        this.locationHubCode = locationHubCode;
    }

    public Double getOriginLat() {
        return originLat;
    }

    public void setOriginLat(Double originLat) {
        this.originLat = originLat;
    }

    public Double getOriginLon() {
        return originLon;
    }

    public void setOriginLon(Double originLon) {
        this.originLon = originLon;
    }

    public Double getDestinationLat() {
        return destinationLat;
    }

    public void setDestinationLat(Double destinationLat) {
        this.destinationLat = destinationLat;
    }

    public Double getDestinationLon() {
        return destinationLon;
    }

    public void setDestinationLon(Double destinationLon) {
        this.destinationLon = destinationLon;
    }

    public Instant getEta() {
        return eta;
    }

    public void setEta(Instant eta) {
        this.eta = eta;
    }

    public Double getDelayProbability() {
        return delayProbability;
    }

    public void setDelayProbability(Double delayProbability) {
        this.delayProbability = delayProbability;
    }

    public Integer getEventCount() {
        return eventCount;
    }

    public void setEventCount(Integer eventCount) {
        this.eventCount = eventCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
