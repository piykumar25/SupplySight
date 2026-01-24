package com.supplysight.ingestion.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.supplysight.common.kafka.KafkaTopics;
import com.supplysight.common.security.JwtTokenProvider;
import com.supplysight.ingestion.dto.EventIngestionDto.*;
import com.supplysight.ingestion.entity.ProcessedEvent;
import com.supplysight.ingestion.entity.TrackingEventEntity;
import com.supplysight.ingestion.repository.ProcessedEventRepository;
import com.supplysight.ingestion.repository.TrackingEventRepository;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Event Ingestion Service using Testcontainers.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@Transactional
class EventIngestionIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("supplysight_test")
            .withUsername("test")
            .withPassword("test");

    @Container
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.5.0")
    );

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TrackingEventRepository trackingEventRepository;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private String accessToken;
    private UUID tenantId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        userId = UUID.randomUUID();
        accessToken = jwtTokenProvider.generateAccessToken(
                userId, tenantId, "testuser", Set.of("OPS_USER")
        );
    }

    @Test
    @DisplayName("Should ingest event successfully and persist to database")
    void ingestEvent_Success_PersistsToDatabase() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID shipmentId = UUID.randomUUID();

        IngestRequest request = new IngestRequest(
                eventId,
                tenantId,
                shipmentId,
                "IN_TRANSIT",
                Instant.now().minusSeconds(60),
                "GPS_DEVICE",
                new Location(12.9716, 77.5946, "BLR-HUB-01"),
                Map.of("speedKmph", 62),
                null
        );

        mockMvc.perform(post("/api/v1/events/ingest")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.data.eventId").value(eventId.toString()));

        // Verify event persisted
        Optional<TrackingEventEntity> savedEvent = trackingEventRepository.findByEventId(eventId);
        assertThat(savedEvent).isPresent();
        assertThat(savedEvent.get().getShipmentId()).isEqualTo(shipmentId);
        assertThat(savedEvent.get().getEventType()).isEqualTo("IN_TRANSIT");

        // Verify processed event tracked
        assertThat(processedEventRepository.existsByEventId(eventId)).isTrue();
    }

    @Test
    @DisplayName("Should reject duplicate event with idempotent response")
    void ingestEvent_Duplicate_ReturnsOk() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID shipmentId = UUID.randomUUID();

        IngestRequest request = new IngestRequest(
                eventId,
                tenantId,
                shipmentId,
                "IN_TRANSIT",
                Instant.now().minusSeconds(60),
                "GPS_DEVICE",
                null,
                null,
                null
        );

        // First ingestion - should succeed
        mockMvc.perform(post("/api/v1/events/ingest")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"));

        // Second ingestion - should return DUPLICATE
        mockMvc.perform(post("/api/v1/events/ingest")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DUPLICATE"));

        // Verify only one event persisted
        long count = trackingEventRepository.countByTenantIdAndShipmentId(tenantId, shipmentId);
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("Should reject event with invalid eventType")
    void ingestEvent_InvalidEventType_ReturnsBadRequest() throws Exception {
        IngestRequest request = new IngestRequest(
                UUID.randomUUID(),
                tenantId,
                UUID.randomUUID(),
                "INVALID_TYPE",
                Instant.now().minusSeconds(60),
                "GPS_DEVICE",
                null,
                null,
                null
        );

        mockMvc.perform(post("/api/v1/events/ingest")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.status").value("REJECTED"));
    }

    @Test
    @DisplayName("Should reject request without authentication")
    void ingestEvent_NoAuth_ReturnsUnauthorized() throws Exception {
        IngestRequest request = new IngestRequest(
                UUID.randomUUID(),
                tenantId,
                UUID.randomUUID(),
                "IN_TRANSIT",
                Instant.now().minusSeconds(60),
                "GPS_DEVICE",
                null,
                null,
                null
        );

        mockMvc.perform(post("/api/v1/events/ingest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should check event status correctly")
    void checkEventStatus_ProcessedEvent() throws Exception {
        UUID eventId = UUID.randomUUID();

        // Create processed event
        ProcessedEvent processed = new ProcessedEvent(eventId, tenantId, ProcessedEvent.EventSource.REST);
        processedEventRepository.save(processed);

        mockMvc.perform(get("/api/v1/events/status/" + eventId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PROCESSED"));
    }

    @Test
    @DisplayName("Should return NOT_FOUND for unprocessed event")
    void checkEventStatus_UnprocessedEvent() throws Exception {
        UUID eventId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/events/status/" + eventId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("Should ingest batch of events")
    void ingestBatch_Success() throws Exception {
        List<IngestRequest> events = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            events.add(new IngestRequest(
                    UUID.randomUUID(),
                    tenantId,
                    UUID.randomUUID(),
                    "IN_TRANSIT",
                    Instant.now().minusSeconds(60 + i),
                    "GPS_DEVICE",
                    null,
                    null,
                    null
            ));
        }

        BatchIngestRequest request = new BatchIngestRequest(events);

        mockMvc.perform(post("/api/v1/events/ingest/batch")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.accepted").value(3))
                .andExpect(jsonPath("$.data.duplicates").value(0))
                .andExpect(jsonPath("$.data.rejected").value(0));
    }
}
