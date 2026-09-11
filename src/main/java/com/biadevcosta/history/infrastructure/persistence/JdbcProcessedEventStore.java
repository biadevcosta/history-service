package com.biadevcosta.history.infrastructure.persistence;

import com.biadevcosta.history.application.port.ProcessedEventStore;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.relational.core.conversion.DbActionExecutionException;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

/** {@link ProcessedEventStore} backed by Spring Data JDBC (table {@code processed_events}). */
@Repository
public class JdbcProcessedEventStore implements ProcessedEventStore {

    private final ProcessedEventJdbcRepository jdbc;

    public JdbcProcessedEventStore(ProcessedEventJdbcRepository jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean exists(String eventId) {
        return jdbc.existsById(eventId);
    }

    @Override
    public void markProcessed(String eventId, LocalDateTime processedAt) {
        ProcessedEventEntity entity = new ProcessedEventEntity();
        entity.setEventId(eventId);
        entity.setProcessedAt(processedAt);
        try {
            jdbc.save(entity);
        } catch (DataIntegrityViolationException | DbActionExecutionException e) {
            // a concurrent handler already recorded this event — idempotent, nothing to do
        }
    }
}
