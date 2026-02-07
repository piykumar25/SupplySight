package com.supplysight.tracking;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
        properties = {
            "spring.datasource.url=jdbc:h2:mem:testdb",
            "spring.datasource.driver-class-name=org.h2.Driver",
            "spring.jpa.hibernate.ddl-auto=create-drop",
            "spring.flyway.enabled=false",
            "spring.jpa.open-in-view=false"
        })
class TrackingServiceApplicationTests {

    @org.springframework.boot.test.mock.mockito.MockBean
    private com.supplysight.tracking.repository.ShipmentRepository shipmentRepository;

    @org.springframework.boot.test.mock.mockito.MockBean
    private com.supplysight.tracking.repository.TrackingEventRepository trackingEventRepository;

    @org.springframework.boot.test.mock.mockito.MockBean
    private org.springframework.kafka.core.KafkaTemplate<String, Object> kafkaTemplate;

    @Test
    void contextLoads() {}
}
