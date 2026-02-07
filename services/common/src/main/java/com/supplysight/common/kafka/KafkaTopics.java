package com.supplysight.common.kafka;

/** Centralized Kafka topic definitions for the platform. */
public final class KafkaTopics {

    /** Raw tracking events from external sources. */
    public static final String TRACKING_EVENTS_RAW = "tracking.events.raw";

    /** Validated and enriched tracking events. */
    public static final String TRACKING_EVENTS_VALIDATED = "tracking.events.validated";

    /** Prediction results (ETA, delay probability, anomalies). */
    public static final String TRACKING_PREDICTIONS = "tracking.predictions";

    /** Alerts for significant events (delay risk, anomalies). */
    public static final String TRACKING_ALERTS = "tracking.alerts";

    /** Audit events for compliance and traceability. */
    public static final String TRACKING_AUDIT = "tracking.audit";

    private KafkaTopics() {}
}
