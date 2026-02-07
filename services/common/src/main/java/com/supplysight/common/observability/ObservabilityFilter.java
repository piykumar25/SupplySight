package com.supplysight.common.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

/**
 * HTTP filter for observability metrics and logging context. Captures request/response size,
 * latency, and propagates correlation IDs.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ObservabilityFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(ObservabilityFilter.class);

    public static final String X_CORRELATION_ID = "X-Correlation-ID";
    public static final String X_REQUEST_ID = "X-Request-ID";

    private final TracingConfig.TracingSpanCustomizer spanCustomizer;
    private final Timer requestTimer;
    private final Counter requestCounter;
    private final Counter errorCounter;

    public ObservabilityFilter(
            TracingConfig.TracingSpanCustomizer spanCustomizer, MeterRegistry meterRegistry) {
        this.spanCustomizer = spanCustomizer;

        this.requestTimer =
                Timer.builder("http.server.requests.duration")
                        .description("HTTP server request duration with detailed tags")
                        .register(meterRegistry);

        this.requestCounter =
                Counter.builder("http.server.requests.total")
                        .description("Total HTTP requests")
                        .register(meterRegistry);

        this.errorCounter =
                Counter.builder("http.server.requests.errors")
                        .description("HTTP request errors (5xx)")
                        .register(meterRegistry);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (!(request instanceof HttpServletRequest httpRequest)
                || !(response instanceof HttpServletResponse httpResponse)) {
            chain.doFilter(request, response);
            return;
        }

        long startTime = System.nanoTime();

        // Wrap request/response for size calculation
        ContentCachingRequestWrapper wrappedRequest =
                request instanceof ContentCachingRequestWrapper
                        ? (ContentCachingRequestWrapper) request
                        : new ContentCachingRequestWrapper(httpRequest);

        ContentCachingResponseWrapper wrappedResponse =
                response instanceof ContentCachingResponseWrapper
                        ? (ContentCachingResponseWrapper) response
                        : new ContentCachingResponseWrapper(httpResponse);

        // Extract or generate correlation ID
        String correlationId = httpRequest.getHeader(X_CORRELATION_ID);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = httpRequest.getHeader(X_REQUEST_ID);
        }
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        // Set up tracing context
        spanCustomizer.addCorrelationId(correlationId);
        spanCustomizer.addRequestContext(httpRequest.getMethod(), httpRequest.getRequestURI());

        // Add correlation ID to response header
        wrappedResponse.setHeader(X_CORRELATION_ID, correlationId);

        try {
            chain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            long duration = System.nanoTime() - startTime;

            int status = wrappedResponse.getStatus();
            String method = httpRequest.getMethod();
            String uri = getCleanUri(httpRequest.getRequestURI());

            // Calculate sizes
            int requestSize =
                    wrappedRequest.getContentLength() > 0
                            ? wrappedRequest.getContentLength()
                            : wrappedRequest.getContentAsByteArray().length;
            int responseSize = wrappedResponse.getContentSize();

            // Log with context
            MDC.put("requestSize", String.valueOf(requestSize));
            MDC.put("responseSize", String.valueOf(responseSize));
            MDC.put("latencyMs", String.valueOf(TimeUnit.NANOSECONDS.toMillis(duration)));
            MDC.put("httpStatus", String.valueOf(status));

            // Record metrics
            requestCounter.increment();
            requestTimer.record(duration, TimeUnit.NANOSECONDS);

            if (status >= 500) {
                errorCounter.increment();
            }

            // Log request completion
            if (log.isDebugEnabled() || status >= 400) {
                log.info(
                        "HTTP {} {} - {} ({}ms, req={}B, res={}B)",
                        method,
                        uri,
                        status,
                        TimeUnit.NANOSECONDS.toMillis(duration),
                        requestSize,
                        responseSize);
            }

            // Copy content to response
            wrappedResponse.copyBodyToResponse();

            // Clear MDC
            spanCustomizer.clearContext();
            MDC.remove("requestSize");
            MDC.remove("responseSize");
            MDC.remove("latencyMs");
            MDC.remove("httpStatus");
        }
    }

    /** Normalize URI for metrics (remove UUIDs and numeric IDs). */
    private String getCleanUri(String uri) {
        if (uri == null) return "unknown";
        // Replace UUIDs with placeholder
        uri =
                uri.replaceAll(
                        "[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}", "{id}");
        // Replace numeric IDs with placeholder
        uri = uri.replaceAll("/\\d+(/|$)", "/{id}$1");
        return uri;
    }
}
