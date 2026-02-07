package com.supplysight.tracking.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.supplysight.common.security.TenantContext;
import com.supplysight.tracking.domain.Shipment;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@TestPropertySource(
        properties = {
            "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
            "spring.datasource.driver-class-name=org.h2.Driver",
            "spring.jpa.hibernate.ddl-auto=create-drop",
            "spring.jpa.properties.hibernate.default_schema=",
            "spring.flyway.enabled=false"
        })
class ShipmentRepositoryTest {

    @Configuration
    @EnableAutoConfiguration
    @EntityScan(basePackages = {"com.supplysight.tracking.domain", "com.supplysight.common.entity"})
    @EnableJpaRepositories(basePackages = "com.supplysight.tracking.repository")
    static class TestConfig {}

    @Autowired private ShipmentRepository shipmentRepository;

    private final UUID tenantA = UUID.randomUUID();
    private final UUID tenantB = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        // Set context to Tenant A
        TenantContext.set(new TenantContext.TenantInfo(tenantA, userId, "user", Set.of("USER")));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        shipmentRepository.deleteAll();
    }

    @Test
    void shouldSaveShipmentWithTenantId() {
        Shipment shipment = new Shipment();
        shipment.setTrackingNumber("TRK-001");
        shipment.setOrigin("New York");
        shipment.setDestination("London");
        shipment.setCurrentStatus("CREATED");
        shipment.setEstimatedDelivery(LocalDateTime.now().plusDays(5));

        Shipment saved = shipmentRepository.save(shipment);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getTenantId()).isEqualTo(tenantA);
    }

    @Test
    void shouldFindShipmentForCorrectTenant() {
        // Save shipment for Tenant A
        Shipment shipment = new Shipment();
        shipment.setTrackingNumber("TRK-002");
        shipment.setOrigin("Paris");
        shipment.setDestination("Berlin");
        shipmentRepository.save(shipment);

        // Verify finding by tenant ID and tracking number
        Optional<Shipment> found =
                shipmentRepository.findByTenantIdAndTrackingNumber(tenantA, "TRK-002");
        assertThat(found).isPresent();
        assertThat(found.get().getTrackingNumber()).isEqualTo("TRK-002");
    }

    @Test
    void shouldNotFindShipmentForDifferentTenant() {
        // Save shipment for Tenant A
        Shipment shipment = new Shipment();
        shipment.setTrackingNumber("TRK-003");
        shipment.setOrigin("Tokyo");
        shipment.setDestination("Osaka");
        shipmentRepository.save(shipment);

        // Switch context to Tenant B
        TenantContext.set(new TenantContext.TenantInfo(tenantB, userId, "user", Set.of("USER")));

        // Verify finding by tenant ID (Tenant B) and tracking number returns empty
        Optional<Shipment> found =
                shipmentRepository.findByTenantIdAndTrackingNumber(tenantB, "TRK-003");
        assertThat(found).isEmpty();

        // Even if we query with Tenant A ID explicitly (if we were bypassing security),
        // the repository method enforces querying by the passed ID.
        // But let's verify finding by Tenant B doesn't return Tenant A's data.
    }
}
