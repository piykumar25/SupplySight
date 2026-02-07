package com.supplysight.prediction;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.supplysight.common.event.TrackingEvent;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.serializer.JsonDeserializer;

public class KafkaSerializationTest {

    @Test
    public void testDeserialization() {
        // Create an ObjectMapper (simulating default one used by JsonDeserializer if
        // not injected)
        // Without jackson-datatype-jsr310 on classpath, this should fail for Instant
        ObjectMapper mapper = new ObjectMapper();

        // This test simulates what happens inside JsonDeserializer when it creates its
        // own ObjectMapper
        // If the module is not on the classpath, findAndRegisterModules() won't find
        // it.
        mapper.findAndRegisterModules();

        JsonDeserializer<TrackingEvent> deserializer =
                new JsonDeserializer<>(TrackingEvent.class, mapper);

        UUID eventId = UUID.randomUUID();
        String json =
                String.format(
                        "{"
                                + "\"eventId\": \"%s\","
                                + "\"tenantId\": \"%s\","
                                + "\"shipmentId\": \"%s\","
                                + "\"eventType\": \"LOCATION_UPDATE\","
                                + "\"eventTime\": \"%s\","
                                + // Instant format
                                "\"source\": \"GPS\""
                                + "}",
                        eventId, UUID.randomUUID(), UUID.randomUUID(), Instant.now().toString());

        TrackingEvent event =
                deserializer.deserialize("topic", json.getBytes(StandardCharsets.UTF_8));

        assertEquals(eventId, event.eventId());
    }
}
