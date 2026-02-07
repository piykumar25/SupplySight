package com.supplysight.identity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/** Identity Service - Authentication, authorization, and tenant/user management. */
@SpringBootApplication(scanBasePackages = {"com.supplysight.identity", "com.supplysight.common"})
@EnableJpaAuditing
public class IdentityServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(IdentityServiceApplication.class, args);
    }
}
