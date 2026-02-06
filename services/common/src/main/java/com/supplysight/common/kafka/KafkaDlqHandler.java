package com.supplysight.common.kafka;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;

/**
 * Dead Letter Queue (DLQ) Handler for Kafka message processing failures.
 * 
 * When a message fails processing after all retries, it's sent to a DLQ topic
 * with additional error metadata for later analysis and reprocessing.
 */
@Component
public class KafkaDlqHandler {

    private static final Logger log = LoggerFactory.getLogger(KafkaDlqHandler.class);

    private static final String HEADER_ORIGINAL_TOPIC = "x-original-topic";
    private static final String HEADER_ORIGINAL_PARTITION = "x-original-partition";
    private static final String HEADER_ORIGINAL_OFFSET = "x-original-offset";
    private static final String HEADER_ORIGINAL_TIMESTAMP = "x-original-timestamp";
    private static final String HEADER_ERROR_MESSAGE = "x-error-message";
    private static final String HEADER_ERROR_STACKTRACE = "x-error-stacktrace";
    private static final String HEADER_RETRY_COUNT = "x-retry-count";
    private static final String HEADER_DLQ_TIMESTAMP = "x-dlq-timestamp";
    private static final String HEADER_CORRELATION_ID = "x-correlation-id";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.dlq.topic-suffix:.dlq}")
    private String dlqTopicSuffix;

    @Value("${kafka.dlq.enabled:true}")
    private boolean dlqEnabled;

    public KafkaDlqHandler(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Send a failed message to the Dead Letter Queue.
     * 
     * @param record     The original consumer record that failed
     * @param exception  The exception that caused the failure
     * @param retryCount Number of retry attempts made
     */
    public void sendToDlq(ConsumerRecord<String, ?> record, Exception exception, int retryCount) {
        if (!dlqEnabled) {
            log.warn("DLQ is disabled. Failed message from topic={} partition={} offset={} will be lost",
                    record.topic(), record.partition(), record.offset());
            return;
        }

        String dlqTopic = record.topic() + dlqTopicSuffix;
        String correlationId = extractHeader(record, "correlationId")
                .orElse(MDC.get("correlationId"));

        try {
            ProducerRecord<String, Object> dlqRecord = new ProducerRecord<>(
                    dlqTopic,
                    record.partition(),
                    record.key(),
                    record.value());

            // Add original message metadata
            addHeader(dlqRecord, HEADER_ORIGINAL_TOPIC, record.topic());
            addHeader(dlqRecord, HEADER_ORIGINAL_PARTITION, String.valueOf(record.partition()));
            addHeader(dlqRecord, HEADER_ORIGINAL_OFFSET, String.valueOf(record.offset()));
            addHeader(dlqRecord, HEADER_ORIGINAL_TIMESTAMP, String.valueOf(record.timestamp()));
            addHeader(dlqRecord, HEADER_DLQ_TIMESTAMP, Instant.now().toString());
            addHeader(dlqRecord, HEADER_RETRY_COUNT, String.valueOf(retryCount));

            // Add error information
            addHeader(dlqRecord, HEADER_ERROR_MESSAGE, truncate(exception.getMessage(), 500));
            addHeader(dlqRecord, HEADER_ERROR_STACKTRACE, truncate(getStackTrace(exception), 2000));

            // Preserve correlation ID
            if (correlationId != null) {
                addHeader(dlqRecord, HEADER_CORRELATION_ID, correlationId);
            }

            // Copy original headers
            record.headers().forEach(header -> {
                if (!header.key().startsWith("x-")) {
                    dlqRecord.headers().add(header);
                }
            });

            kafkaTemplate.send(dlqRecord).whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to send message to DLQ topic={}: {}", dlqTopic, ex.getMessage());
                } else {
                    log.info("Message sent to DLQ topic={} partition={} offset={}",
                            result.getRecordMetadata().topic(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                }
            });

        } catch (Exception e) {
            log.error("Failed to send to DLQ topic={}: {}", dlqTopic, e.getMessage(), e);
        }
    }

    /**
     * Creates a CommonErrorHandler that sends failed messages to DLQ.
     * Compatible with Spring Kafka 3.x.
     */
    public CommonErrorHandler createDlqErrorHandler() {
        return new CommonErrorHandler() {
            @Override
            public boolean handleOne(Exception exception, ConsumerRecord<?, ?> record,
                    Consumer<?, ?> consumer, MessageListenerContainer container) {
                @SuppressWarnings("unchecked")
                ConsumerRecord<String, ?> typedRecord = (ConsumerRecord<String, ?>) record;
                int retryCount = extractRetryCount(typedRecord);
                sendToDlq(typedRecord, exception, retryCount + 1);
                return true; // Mark as handled
            }
        };
    }

    /**
     * Extract retry count from a consumer record.
     */
    public int extractRetryCount(ConsumerRecord<String, ?> record) {
        return extractHeader(record, HEADER_RETRY_COUNT)
                .map(Integer::parseInt)
                .orElse(0);
    }

    private void addHeader(ProducerRecord<String, Object> record, String key, String value) {
        if (value != null) {
            record.headers().add(key, value.getBytes(StandardCharsets.UTF_8));
        }
    }

    private Optional<String> extractHeader(ConsumerRecord<String, ?> record, String key) {
        Header header = record.headers().lastHeader(key);
        if (header != null && header.value() != null) {
            return Optional.of(new String(header.value(), StandardCharsets.UTF_8));
        }
        return Optional.empty();
    }

    private String getStackTrace(Exception exception) {
        StringBuilder sb = new StringBuilder();
        sb.append(exception.getClass().getName()).append(": ").append(exception.getMessage()).append("\n");
        for (StackTraceElement element : exception.getStackTrace()) {
            if (sb.length() > 1500)
                break;
            sb.append("\tat ").append(element.toString()).append("\n");
        }
        return sb.toString();
    }

    private String truncate(String str, int maxLength) {
        if (str == null)
            return "";
        return str.length() > maxLength ? str.substring(0, maxLength) + "..." : str;
    }
}
