package com.supplysight.prediction;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * Prediction Engine Service - Computes ETA, delay risk, and anomaly detection.
 */
@SpringBootApplication(scanBasePackages = {"com.supplysight.prediction", "com.supplysight.common"})
@EnableJpaAuditing
@EnableKafka
public class PredictionEngineApplication {

    public static void main(String[] args) {
        SpringApplication.run(PredictionEngineApplication.class, args);
    }
}
