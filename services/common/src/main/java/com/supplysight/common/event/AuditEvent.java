package com.supplysight.common.event;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Audit event for tracking all significant actions in the system.
 */
public record AuditEvent(
    UUID eventId,
    UUID tenantId,
    UUID userId,
    String username,
    String action,
    String resourceType,
    String resourceId,
    Instant timestamp,
    String sourceIp,
    String userAgent,
    Map<String, Object> details,
    String correlationId
) {
    /**
     * Audit action types.
     */
    public static final class Actions {
        // Authentication
        public static final String LOGIN_SUCCESS = "LOGIN_SUCCESS";
        public static final String LOGIN_FAILED = "LOGIN_FAILED";
        public static final String LOGOUT = "LOGOUT";
        public static final String TOKEN_REFRESH = "TOKEN_REFRESH";
        
        // User management
        public static final String USER_CREATED = "USER_CREATED";
        public static final String USER_UPDATED = "USER_UPDATED";
        public static final String USER_DELETED = "USER_DELETED";
        public static final String PASSWORD_CHANGED = "PASSWORD_CHANGED";
        
        // Tenant management
        public static final String TENANT_CREATED = "TENANT_CREATED";
        public static final String TENANT_UPDATED = "TENANT_UPDATED";
        public static final String TENANT_DELETED = "TENANT_DELETED";
        
        // Shipment operations
        public static final String SHIPMENT_CREATED = "SHIPMENT_CREATED";
        public static final String SHIPMENT_UPDATED = "SHIPMENT_UPDATED";
        public static final String SHIPMENT_DELETED = "SHIPMENT_DELETED";
        
        // Event ingestion
        public static final String EVENT_INGESTED = "EVENT_INGESTED";
        public static final String EVENT_VALIDATED = "EVENT_VALIDATED";
        public static final String EVENT_REJECTED = "EVENT_REJECTED";
        
        // Predictions
        public static final String PREDICTION_GENERATED = "PREDICTION_GENERATED";
        public static final String ALERT_RAISED = "ALERT_RAISED";
        
        // Admin actions
        public static final String CONFIG_CHANGED = "CONFIG_CHANGED";
        public static final String EXPORT_REQUESTED = "EXPORT_REQUESTED";
        
        private Actions() {}
    }

    /**
     * Resource types for audit logging.
     */
    public static final class ResourceTypes {
        public static final String USER = "USER";
        public static final String TENANT = "TENANT";
        public static final String SHIPMENT = "SHIPMENT";
        public static final String TRACKING_EVENT = "TRACKING_EVENT";
        public static final String PREDICTION = "PREDICTION";
        public static final String ALERT = "ALERT";
        public static final String CONFIGURATION = "CONFIGURATION";
        
        private ResourceTypes() {}
    }

    public static AuditEvent of(UUID tenantId, UUID userId, String username, String action, 
                                 String resourceType, String resourceId, Map<String, Object> details) {
        return new AuditEvent(
            UUID.randomUUID(),
            tenantId,
            userId,
            username,
            action,
            resourceType,
            resourceId,
            Instant.now(),
            null,
            null,
            details,
            null
        );
    }
}
