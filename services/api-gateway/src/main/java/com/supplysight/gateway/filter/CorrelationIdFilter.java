package com.supplysight.gateway.filter;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Global filter to inject correlation ID for distributed tracing. If a correlation ID is present in
 * the request, it's propagated; otherwise, a new one is generated.
 */
@Component
public class CorrelationIdFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(CorrelationIdFilter.class);

    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String MDC_CORRELATION_KEY = "correlationId";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String correlationId = exchange.getRequest().getHeaders().getFirst(CORRELATION_ID_HEADER);

        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
            log.debug("Generated new correlation ID: {}", correlationId);
        } else {
            log.debug("Using existing correlation ID: {}", correlationId);
        }

        // Store in exchange attributes for other filters
        exchange.getAttributes().put(CORRELATION_ID_HEADER, correlationId);

        // Add to request headers for downstream services
        ServerHttpRequest mutatedRequest =
                exchange.getRequest().mutate().header(CORRELATION_ID_HEADER, correlationId).build();

        // Add to response headers for client tracking
        ServerHttpResponse response = exchange.getResponse();
        final String finalCorrelationId = correlationId;
        response.beforeCommit(
                () -> {
                    response.getHeaders().add(CORRELATION_ID_HEADER, finalCorrelationId);
                    return Mono.empty();
                });

        // Set MDC for logging context
        String mdcCorrelationId = correlationId;
        return chain.filter(exchange.mutate().request(mutatedRequest).build())
                .contextWrite(
                        ctx -> {
                            MDC.put(MDC_CORRELATION_KEY, mdcCorrelationId);
                            return ctx;
                        })
                .doFinally(signal -> MDC.remove(MDC_CORRELATION_KEY));
    }

    @Override
    public int getOrder() {
        // Run before JWT filter
        return -200;
    }
}
