package com.supplysight.audit.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

import java.util.Map;
import java.util.UUID;

/**
 * Audit log entity - immutable record of system events.
 */
@Entity
@Table(name = "audit_logs", schema = "audit")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true)
    private UUID eventId;

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "username")
    private String username;

    @Column(name = "action", nullable = false, length = 100)
    private String action;

    @Column(name = "resource_type", nullable = false, length = 50)
    private String resourceType;

    @Column(name = "resource_id")
    private String resourceId;

    @Column(name = "event_time", nullable = false)
    private Instant eventTime;

    @Column(name = "source_ip", length = 45)
    private String sourceIp;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "details", columnDefinition = "jsonb")
    private Map<String, Object> details;

    @Column(name = "correlation_id", length = 50)
    private String correlationId;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (eventTime == null) {
            eventTime = Instant.now();
        }
    }
}
