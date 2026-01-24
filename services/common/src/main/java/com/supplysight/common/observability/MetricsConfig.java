package com.supplysight.common.observability;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Common metrics configuration for all services.
 */
@Configuration
public class MetricsConfig {

    /**
     * Timer for measuring API request latency.
     */
    @Bean
    public Timer apiRequestTimer(MeterRegistry meterRegistry) {
        return Timer.builder("api.request.duration")
                .description("API request duration")
                .register(meterRegistry);
    }

    /**
     * Counter for tracking API requests by status.
     */
    @Bean
    public io.micrometer.core.instrument.Counter apiRequestCounter(MeterRegistry meterRegistry) {
        return io.micrometer.core.instrument.Counter.builder("api.request.total")
                .description("Total API requests")
                .register(meterRegistry);
    }

    /**
     * Counter for tracking Kafka messages consumed.
     */
    @Bean
    public io.micrometer.core.instrument.Counter kafkaMessageCounter(MeterRegistry meterRegistry) {
        return io.micrometer.core.instrument.Counter.builder("kafka.messages.consumed")
                .description("Kafka messages consumed")
                .register(meterRegistry);
    }

    /**
     * Counter for tracking Kafka messages produced.
     */
    @Bean
    public io.micrometer.core.instrument.Counter kafkaMessageProducedCounter(MeterRegistry meterRegistry) {
        return io.micrometer.core.instrument.Counter.builder("kafka.messages.produced")
                .description("Kafka messages produced")
                .register(meterRegistry);
    }

    /**
     * Gauge for tracking active shipments.
     */
    @Bean
    public io.micrometer.core.instrument.Gauge activeShipmentsGauge(MeterRegistry meterRegistry) {
        return io.micrometer.core.instrument.Gauge.builder("shipments.active", () -> 0)
                .description("Number of active shipments")
                .register(meterRegistry);
    }
}
