package com.supplysight.ingestion;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.kafka.annotation.EnableKafka;

/** Event Ingestion Service - Validates, deduplicates, and publishes tracking events. */
@SpringBootApplication(scanBasePackages = {"com.supplysight.ingestion", "com.supplysight.common"})
@EnableJpaAuditing
@EnableKafka
public class EventIngestionApplication {

    public static void main(String[] args) {
        SpringApplication.run(EventIngestionApplication.class, args);
    }
}
