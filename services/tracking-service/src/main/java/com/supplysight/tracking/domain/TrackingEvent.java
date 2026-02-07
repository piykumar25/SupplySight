package com.supplysight.tracking.domain;

import com.supplysight.common.entity.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "shipment_updates")
public class TrackingEvent extends TenantAwareEntity {

    @Column(nullable = false)
    private String trackingNumber;

    @Column(nullable = false)
    private String status;

    private String location;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    private String description;

    public TrackingEvent() {
        super();
    }

    // Getters and Setters

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
