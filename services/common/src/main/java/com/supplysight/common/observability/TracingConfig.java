package com.supplysight.common.observability;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.slf4j.MDC;
import org.springframework.boot.actuate.autoconfigure.tracing.MicrometerTracingAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.Nullable;

/**
 * Distributed tracing configuration using Micrometer Tracing with OpenTelemetry. Provides trace
 * propagation with correlationId and tenantId context.
 */
@Configuration
@AutoConfigureAfter(MicrometerTracingAutoConfiguration.class)
public class TracingConfig {

    public static final String CORRELATION_ID = "correlationId";
    public static final String TENANT_ID = "tenantId";
    public static final String USER_ID = "userId";
    public static final String REQUEST_PATH = "requestPath";
    public static final String REQUEST_METHOD = "requestMethod";

    /** Custom span handler to add tenant context to spans. */
    @Bean
    public TracingSpanCustomizer tracingSpanCustomizer(@Nullable Tracer tracer) {
        return new TracingSpanCustomizer(tracer);
    }

    /** Helper class to add custom tags to spans. */
    public static class TracingSpanCustomizer {

        private final Tracer tracer;

        public TracingSpanCustomizer(@Nullable Tracer tracer) {
            this.tracer = tracer;
        }

        /** Add tenant context to the current span. */
        public void addTenantContext(String tenantId, String userId) {
            if (tracer == null) return;

            Span currentSpan = tracer.currentSpan();
            if (currentSpan != null) {
                currentSpan.tag(TENANT_ID, tenantId != null ? tenantId : "unknown");
                currentSpan.tag(USER_ID, userId != null ? userId : "unknown");
            }

            // Also add to MDC for logging
            if (tenantId != null) {
                MDC.put(TENANT_ID, tenantId);
            }
            if (userId != null) {
                MDC.put(USER_ID, userId);
            }
        }

        /** Add correlation ID to the current span and MDC. */
        public void addCorrelationId(String correlationId) {
            if (correlationId == null) return;

            MDC.put(CORRELATION_ID, correlationId);

            if (tracer == null) return;
            Span currentSpan = tracer.currentSpan();
            if (currentSpan != null) {
                currentSpan.tag(CORRELATION_ID, correlationId);
            }
        }

        /** Add request context to the current span. */
        public void addRequestContext(String method, String path) {
            if (tracer == null) return;

            Span currentSpan = tracer.currentSpan();
            if (currentSpan != null) {
                currentSpan.tag(REQUEST_METHOD, method);
                currentSpan.tag(REQUEST_PATH, path);
            }

            MDC.put(REQUEST_METHOD, method);
            MDC.put(REQUEST_PATH, path);
        }

        /** Get the current trace ID (for correlation). */
        public String getCurrentTraceId() {
            if (tracer == null) return null;
            Span currentSpan = tracer.currentSpan();
            return currentSpan != null ? currentSpan.context().traceId() : null;
        }

        /** Clear MDC context. */
        public void clearContext() {
            MDC.remove(CORRELATION_ID);
            MDC.remove(TENANT_ID);
            MDC.remove(USER_ID);
            MDC.remove(REQUEST_METHOD);
            MDC.remove(REQUEST_PATH);
        }
    }
}
