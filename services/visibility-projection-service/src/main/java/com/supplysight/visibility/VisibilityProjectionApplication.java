package com.supplysight.visibility;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * Visibility Projection Service - Maintains materialized views and provides real-time visibility APIs.
 */
@SpringBootApplication(scanBasePackages = {"com.supplysight.visibility", "com.supplysight.common"})
@EnableJpaAuditing
@EnableKafka
@EnableCaching
public class VisibilityProjectionApplication {

    public static void main(String[] args) {
        SpringApplication.run(VisibilityProjectionApplication.class, args);
    }
}
