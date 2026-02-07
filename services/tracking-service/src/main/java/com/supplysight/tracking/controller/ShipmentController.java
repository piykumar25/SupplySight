package com.supplysight.tracking.controller;

import com.supplysight.tracking.domain.Shipment;
import com.supplysight.tracking.domain.TrackingEvent;
import com.supplysight.tracking.service.TrackingService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/shipments")
public class ShipmentController {

    private final TrackingService trackingService;

    public ShipmentController(TrackingService trackingService) {
        this.trackingService = trackingService;
    }

    @PostMapping
    public ResponseEntity<Shipment> createShipment(@RequestBody Shipment shipment) {
        return ResponseEntity.ok(trackingService.createShipment(shipment));
    }

    @GetMapping
    public ResponseEntity<List<Shipment>> getAllShipments() {
        return ResponseEntity.ok(trackingService.getAllShipments());
    }

    @GetMapping("/{trackingNumber}")
    public ResponseEntity<Shipment> getShipment(@PathVariable String trackingNumber) {
        return trackingService
                .getShipment(trackingNumber)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{trackingNumber}/events")
    public ResponseEntity<TrackingEvent> addEvent(
            @PathVariable String trackingNumber, @RequestBody TrackingEvent event) {
        return ResponseEntity.ok(trackingService.addEvent(trackingNumber, event));
    }

    @GetMapping("/{trackingNumber}/events")
    public ResponseEntity<List<TrackingEvent>> getEvents(@PathVariable String trackingNumber) {
        return ResponseEntity.ok(trackingService.getEvents(trackingNumber));
    }
}
