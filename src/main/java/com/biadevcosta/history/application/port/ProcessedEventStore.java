package com.biadevcosta.history.application.port;

import java.time.LocalDateTime;

/** Idempotency ledger for inbound Kafka events, keyed by {@code eventId}. */
public interface ProcessedEventStore {

    boolean exists(String eventId);

    void markProcessed(String eventId, LocalDateTime processedAt);
}
