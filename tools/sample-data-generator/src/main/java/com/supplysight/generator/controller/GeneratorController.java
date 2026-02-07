package com.supplysight.generator.controller;

import com.supplysight.generator.service.DataGeneratorService;
import com.supplysight.generator.service.DataIngestionService;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** REST controller for generating sample data. */
@RestController
@RequestMapping("/api/v1/generate")
public class GeneratorController {

    @Autowired private DataGeneratorService dataGeneratorService;

    @Autowired private DataIngestionService dataIngestionService;

    @PostMapping("/shipments/{count}")
    public ResponseEntity<Map<String, Object>> generateShipments(
            @PathVariable int count, @RequestParam(required = false) UUID tenantId) {

        if (tenantId == null) {
            tenantId = UUID.randomUUID();
        }

        List<DataGeneratorService.ShipmentData> shipments = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            shipments.add(dataGeneratorService.generateShipment(tenantId));
        }

        // Ingest shipments and events asynchronously
        CompletableFuture.runAsync(
                () -> {
                    for (DataGeneratorService.ShipmentData shipment : shipments) {
                        try {
                            dataIngestionService.ingestShipment(shipment);
                            List<DataGeneratorService.EventData> events =
                                    dataGeneratorService.generateEventsForShipment(shipment);
                            for (DataGeneratorService.EventData event : events) {
                                dataIngestionService.ingestEvent(event);
                                Thread.sleep(100); // Small delay between events
                            }
                        } catch (Exception e) {
                            System.err.println("Failed to ingest shipment: " + e.getMessage());
                        }
                    }
                });

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("tenantId", tenantId);
        response.put("shipmentsGenerated", count);
        response.put("message", "Shipments are being generated and ingested asynchronously");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/events/{shipmentId}")
    public ResponseEntity<Map<String, Object>> generateEventsForShipment(
            @PathVariable UUID shipmentId,
            @RequestParam UUID tenantId,
            @RequestParam(defaultValue = "10") int count) {

        // This would require fetching shipment data first
        // For now, generate random events
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Event generation for specific shipment not yet implemented");
        return ResponseEntity.ok(response);
    }
}
