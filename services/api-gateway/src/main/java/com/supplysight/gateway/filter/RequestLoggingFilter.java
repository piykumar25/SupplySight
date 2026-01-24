package com.supplysight.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Simple request logging filter for debugging and monitoring.
 */
@Component
public class RequestLoggingFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        long startTime = System.currentTimeMillis();

        String correlationId = exchange.getAttribute(CorrelationIdFilter.CORRELATION_ID_HEADER);
        String tenantId = exchange.getAttribute("tenantId");

        log.info("Incoming request: {} {} | tenant={} | correlation={}",
                request.getMethod(),
                request.getPath().value(),
                tenantId != null ? tenantId : "anonymous",
                correlationId);

        return chain.filter(exchange)
                .doFinally(signal -> {
                    long duration = System.currentTimeMillis() - startTime;
                    int statusCode = exchange.getResponse().getStatusCode() != null
                            ? exchange.getResponse().getStatusCode().value()
                            : 0;

                    log.info("Completed request: {} {} | status={} | duration={}ms | correlation={}",
                            request.getMethod(),
                            request.getPath().value(),
                            statusCode,
                            duration,
                            correlationId);
                });
    }

    @Override
    public int getOrder() {
        // Run after correlation ID filter
        return -150;
    }
}
