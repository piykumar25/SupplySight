package com.supplysight.prediction.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Prediction result for a shipment.
 */
@Entity
@Table(name = "shipment_predictions", schema = "prediction",
    uniqueConstraints = @UniqueConstraint(name = "uk_prediction_shipment_event", 
                                          columnNames = {"shipment_id", "event_id"}))
public class ShipmentPrediction {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "shipment_id", nullable = false, updatable = false)
    private UUID shipmentId;

    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @Column(name = "eta")
    private Instant eta;

    @Column(name = "eta_confidence")
    private Double etaConfidence;

    @Column(name = "delay_probability", nullable = false)
    private Double delayProbability;

    @Enumerated(EnumType.STRING)
    @Column(name = "delay_risk", nullable = false, length = 20)
    private DelayRisk delayRisk;

    @Column(name = "anomaly_detected", nullable = false)
    private boolean anomalyDetected = false;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "anomaly_flags", columnDefinition = "jsonb")
    private List<String> anomalyFlags;

    @Column(name = "distance_remaining_km")
    private Double distanceRemainingKm;

    @Column(name = "average_speed_kmph")
    private Double averageSpeedKmph;

    @Column(name = "dwell_time_hours")
    private Double dwellTimeHours;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "factors", columnDefinition = "jsonb")
    private Map<String, Object> factors;

    @Column(name = "model_version", length = 50)
    private String modelVersion = "heuristic-v1";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public ShipmentPrediction() {
        this.id = UUID.randomUUID();
        this.createdAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
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

    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public Instant getEta() {
        return eta;
    }

    public void setEta(Instant eta) {
        this.eta = eta;
    }

    public Double getEtaConfidence() {
        return etaConfidence;
    }

    public void setEtaConfidence(Double etaConfidence) {
        this.etaConfidence = etaConfidence;
    }

    public Double getDelayProbability() {
        return delayProbability;
    }

    public void setDelayProbability(Double delayProbability) {
        this.delayProbability = delayProbability;
    }

    public DelayRisk getDelayRisk() {
        return delayRisk;
    }

    public void setDelayRisk(DelayRisk delayRisk) {
        this.delayRisk = delayRisk;
    }

    public boolean isAnomalyDetected() {
        return anomalyDetected;
    }

    public void setAnomalyDetected(boolean anomalyDetected) {
        this.anomalyDetected = anomalyDetected;
    }

    public List<String> getAnomalyFlags() {
        return anomalyFlags;
    }

    public void setAnomalyFlags(List<String> anomalyFlags) {
        this.anomalyFlags = anomalyFlags;
    }

    public Double getDistanceRemainingKm() {
        return distanceRemainingKm;
    }

    public void setDistanceRemainingKm(Double distanceRemainingKm) {
        this.distanceRemainingKm = distanceRemainingKm;
    }

    public Double getAverageSpeedKmph() {
        return averageSpeedKmph;
    }

    public void setAverageSpeedKmph(Double averageSpeedKmph) {
        this.averageSpeedKmph = averageSpeedKmph;
    }

    public Double getDwellTimeHours() {
        return dwellTimeHours;
    }

    public void setDwellTimeHours(Double dwellTimeHours) {
        this.dwellTimeHours = dwellTimeHours;
    }

    public Map<String, Object> getFactors() {
        return factors;
    }

    public void setFactors(Map<String, Object> factors) {
        this.factors = factors;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public void setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public enum DelayRisk {
        LOW,
        MEDIUM,
        HIGH
    }
}
