package com.supplysight.common.observability;

import ch.qos.logback.classic.PatternLayout;
import net.logstash.logback.encoder.LogstashEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Logging configuration for structured JSON logging. */
@Configuration
public class LoggingConfig {

    private static final Logger log = LoggerFactory.getLogger(LoggingConfig.class);

    /**
     * Configure structured JSON logging for production. In development, use console logging with
     * correlation ID.
     */
    @Bean
    public LogstashEncoder logstashEncoder() {
        LogstashEncoder encoder = new LogstashEncoder();
        encoder.setIncludeContext(true);
        encoder.setIncludeMdc(true);
        encoder.setIncludeCallerData(true);
        encoder.setCustomFields("{\"application\":\"supplysight\"}");
        return encoder;
    }

    /** Pattern layout for console logging with correlation ID. */
    @Bean
    public PatternLayout patternLayout() {
        PatternLayout layout = new PatternLayout();
        layout.setPattern(
                "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level [%X{correlationId}] [%X{tenantId}] %logger{36} - %msg%n");
        return layout;
    }
}
