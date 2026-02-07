package com.supplysight.generator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Sample Data Generator - Generates realistic shipment and event data for testing. */
@SpringBootApplication(scanBasePackages = {"com.supplysight.generator", "com.supplysight.common"})
public class SampleDataGeneratorApplication {

    public static void main(String[] args) {
        SpringApplication.run(SampleDataGeneratorApplication.class, args);
    }
}
