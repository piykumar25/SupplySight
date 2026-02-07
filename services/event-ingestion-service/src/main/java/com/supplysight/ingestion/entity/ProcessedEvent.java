package com.supplysight.ingestion.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/** Tracks processed event IDs for idempotency/deduplication. */
@Entity
@Table(name = "processed_events", schema = "tracking")
public class ProcessedEvent {

    @Id
    @Column(name = "event_id", updatable = false, nullable = false)
    private UUID eventId;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "processed_at", nullable = false, updatable = false)
    private Instant processedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ProcessingStatus status = ProcessingStatus.PROCESSED;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 50)
    private EventSource source = EventSource.REST;

    public ProcessedEvent() {
        this.processedAt = Instant.now();
    }

    public ProcessedEvent(UUID eventId, UUID tenantId, EventSource source) {
        this.eventId = eventId;
        this.tenantId = tenantId;
        this.source = source;
        this.processedAt = Instant.now();
        this.status = ProcessingStatus.PROCESSED;
    }

    @PrePersist
    protected void onCreate() {
        if (this.processedAt == null) {
            this.processedAt = Instant.now();
        }
    }

    // Getters and Setters
    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(Instant processedAt) {
        this.processedAt = processedAt;
    }

    public ProcessingStatus getStatus() {
        return status;
    }

    public void setStatus(ProcessingStatus status) {
        this.status = status;
    }

    public EventSource getSource() {
        return source;
    }

    public void setSource(EventSource source) {
        this.source = source;
    }

    public enum ProcessingStatus {
        PROCESSED,
        FAILED,
        DUPLICATE
    }

    public enum EventSource {
        REST,
        KAFKA_RAW
    }
}
