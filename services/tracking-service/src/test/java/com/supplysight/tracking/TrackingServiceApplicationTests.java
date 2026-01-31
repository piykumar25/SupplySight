package com.supplysight.tracking;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration"
})
class TrackingServiceApplicationTests {

    @org.springframework.boot.test.mock.mockito.MockBean
    private com.supplysight.tracking.repository.ShipmentRepository shipmentRepository;

    @org.springframework.boot.test.mock.mockito.MockBean
    private com.supplysight.tracking.repository.TrackingEventRepository trackingEventRepository;

    @org.springframework.boot.test.mock.mockito.MockBean
    private org.springframework.kafka.core.KafkaTemplate<String, Object> kafkaTemplate;

    @Test
    void contextLoads() {
    }

}
