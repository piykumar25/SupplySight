package com.supplysight.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

/** Gateway configuration for rate limiting key resolvers. */
@Configuration
public class GatewayConfig {

    /**
     * Rate limiter key resolver based on client IP address. Used for public endpoints like login.
     */
    @Bean
    @Primary
    public KeyResolver ipKeyResolver() {
        return exchange ->
                Mono.just(
                        exchange.getRequest().getRemoteAddress() != null
                                ? exchange.getRequest()
                                        .getRemoteAddress()
                                        .getAddress()
                                        .getHostAddress()
                                : "unknown");
    }

    /**
     * Rate limiter key resolver based on tenant ID from JWT claims. Falls back to IP address if
     * tenant ID is not available.
     */
    @Bean
    public KeyResolver tenantKeyResolver() {
        return exchange -> {
            String tenantId = exchange.getAttribute("tenantId");
            if (tenantId != null) {
                return Mono.just(tenantId);
            }
            // Fallback to IP if tenant ID not available
            return Mono.just(
                    exchange.getRequest().getRemoteAddress() != null
                            ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                            : "unknown");
        };
    }
}
