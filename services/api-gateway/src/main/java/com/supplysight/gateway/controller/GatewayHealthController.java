package com.supplysight.gateway.controller;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/** Health and status controller for the API Gateway. */
@RestController
public class GatewayHealthController {

    @GetMapping("/")
    public Mono<ResponseEntity<Map<String, Object>>> root() {
        Map<String, Object> response = new HashMap<>();
        response.put("service", "SupplySight API Gateway");
        response.put("status", "UP");
        response.put("timestamp", Instant.now().toString());
        response.put("version", "1.0.0");
        return Mono.just(ResponseEntity.ok(response));
    }

    @GetMapping("/gateway/health")
    public Mono<ResponseEntity<Map<String, Object>>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", Instant.now().toString());
        return Mono.just(ResponseEntity.ok(response));
    }
}
