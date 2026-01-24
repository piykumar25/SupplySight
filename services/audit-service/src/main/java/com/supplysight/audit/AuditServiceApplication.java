package com.supplysight.audit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Audit Service - Stores and queries audit trail for compliance.
 * Consumes audit events from Kafka and provides query APIs.
 */
@SpringBootApplication(scanBasePackages = {"com.supplysight.audit", "com.supplysight.common"})
public class AuditServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuditServiceApplication.class, args);
    }
}
