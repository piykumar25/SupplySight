package com.supplysight.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supplysight.common.event.AuditEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Global filter for logging all API access to Kafka for audit trail.
 * Captures request/response details and publishes audit events.
 */
@Component
public class AccessLoggingFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(AccessLoggingFilter.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String auditTopic;
    private final boolean auditEnabled;

    public AccessLoggingFilter(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            @Value("${gateway.audit.topic:tracking.audit}") String auditTopic,
            @Value("${gateway.audit.enabled:true}") boolean auditEnabled
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.auditTopic = auditTopic;
        this.auditEnabled = auditEnabled;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!auditEnabled) {
            return chain.filter(exchange);
        }

        long startTime = System.currentTimeMillis();
        ServerHttpRequest request = exchange.getRequest();

        return chain.filter(exchange)
                .doFinally(signal -> {
                    try {
                        long duration = System.currentTimeMillis() - startTime;
                        HttpStatus status = exchange.getResponse().getStatusCode() != null
                                ? HttpStatus.resolve(exchange.getResponse().getStatusCode().value())
                                : HttpStatus.OK;

                        publishAuditEvent(exchange, request, status, duration);
                    } catch (Exception e) {
                        log.error("Failed to publish audit event", e);
                    }
                });
    }

    private void publishAuditEvent(ServerWebExchange exchange, ServerHttpRequest request,
                                    HttpStatus status, long duration) {
        String tenantIdStr = exchange.getAttribute("tenantId");
        String userIdStr = exchange.getAttribute("userId");
        String username = exchange.getAttribute("username");
        String correlationId = exchange.getAttribute(CorrelationIdFilter.CORRELATION_ID_HEADER);

        UUID tenantId = tenantIdStr != null ? parseUuid(tenantIdStr) : null;
        UUID userId = userIdStr != null ? parseUuid(userIdStr) : null;

        // Build audit details
        Map<String, Object> details = new HashMap<>();
        details.put("method", request.getMethod().toString());
        details.put("path", request.getPath().value());
        details.put("queryParams", request.getQueryParams().toSingleValueMap());
        details.put("statusCode", status != null ? status.value() : 0);
        details.put("durationMs", duration);

        // Determine action based on HTTP method and path
        String action = determineAction(request.getMethod().toString(), request.getPath().value());

        String sourceIp = request.getRemoteAddress() != null
                ? request.getRemoteAddress().getAddress().getHostAddress()
                : "unknown";

        String userAgent = request.getHeaders().getFirst("User-Agent");

        AuditEvent auditEvent = new AuditEvent(
                UUID.randomUUID(),
                tenantId,
                userId,
                username,
                action,
                "API_REQUEST",
                request.getPath().value(),
                Instant.now(),
                sourceIp,
                userAgent,
                details,
                correlationId
        );

        try {
            String eventJson = objectMapper.writeValueAsString(auditEvent);
            String key = tenantId != null ? tenantId.toString() : "system";
            
            kafkaTemplate.send(auditTopic, key, eventJson)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish audit event: {}", ex.getMessage());
                        } else {
                            log.debug("Published audit event: {} to partition {}", 
                                    action, result.getRecordMetadata().partition());
                        }
                    });
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize audit event", e);
        }
    }

    private String determineAction(String method, String path) {
        // Determine action type based on endpoint
        if (path.contains("/login")) {
            return "API_LOGIN_ATTEMPT";
        } else if (path.contains("/logout")) {
            return "API_LOGOUT";
        } else if (path.contains("/refresh")) {
            return "API_TOKEN_REFRESH";
        } else if (path.contains("/events/ingest")) {
            return "API_EVENT_INGEST";
        } else if (path.contains("/shipments")) {
            return "API_SHIPMENT_" + method;
        } else if (path.contains("/predictions")) {
            return "API_PREDICTION_" + method;
        } else if (path.contains("/alerts")) {
            return "API_ALERT_" + method;
        } else if (path.contains("/users")) {
            return "API_USER_" + method;
        } else if (path.contains("/tenants")) {
            return "API_TENANT_" + method;
        } else if (path.contains("/audit")) {
            return "API_AUDIT_" + method;
        } else {
            return "API_" + method;
        }
    }

    private UUID parseUuid(String str) {
        try {
            return UUID.fromString(str);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public int getOrder() {
        // Run after authentication but before routing
        return -50;
    }
}
