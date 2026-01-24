package com.supplysight.tracking.service;

import com.supplysight.tracking.domain.Shipment;
import com.supplysight.tracking.domain.TrackingEvent;
import com.supplysight.tracking.repository.ShipmentRepository;
import com.supplysight.tracking.repository.TrackingEventRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class TrackingService {

    private final ShipmentRepository shipmentRepository;
    private final TrackingEventRepository eventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public TrackingService(ShipmentRepository shipmentRepository,
            TrackingEventRepository eventRepository,
            KafkaTemplate<String, Object> kafkaTemplate) {
        this.shipmentRepository = shipmentRepository;
        this.eventRepository = eventRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    public Shipment createShipment(Shipment shipment) {
        Shipment saved = shipmentRepository.save(shipment);
        // kafkaTemplate.send("tracking.shipments", saved);
        return saved;
    }

    public Optional<Shipment> getShipment(String trackingNumber) {
        return shipmentRepository.findByTrackingNumber(trackingNumber);
    }

    public List<Shipment> getAllShipments() {
        return shipmentRepository.findAll();
    }

    @Transactional
    public TrackingEvent addEvent(String trackingNumber, TrackingEvent event) {
        Shipment shipment = shipmentRepository.findByTrackingNumber(trackingNumber)
                .orElseThrow(() -> new RuntimeException("Shipment not found"));

        event.setTrackingNumber(trackingNumber);
        TrackingEvent savedEvent = eventRepository.save(event);

        // Update shipment status
        shipment.setCurrentStatus(event.getStatus());
        shipmentRepository.save(shipment);

        // Publish to Kafka
        kafkaTemplate.send("tracking.events.raw", savedEvent);

        return savedEvent;
    }

    public List<TrackingEvent> getEvents(String trackingNumber) {
        return eventRepository.findByTrackingNumberOrderByTimestampDesc(trackingNumber);
    }
}
