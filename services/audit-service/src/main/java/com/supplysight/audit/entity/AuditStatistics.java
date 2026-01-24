package com.supplysight.audit.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Pre-aggregated audit statistics for reporting.
 */
@Entity
@Table(name = "audit_statistics", schema = "audit",
        uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "stat_date", "action"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditStatistics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "stat_date", nullable = false)
    private LocalDate statDate;

    @Column(name = "action", nullable = false, length = 100)
    private String action;

    @Column(name = "count")
    @Builder.Default
    private Long count = 0L;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
