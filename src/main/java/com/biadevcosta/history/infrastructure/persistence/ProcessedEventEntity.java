package com.biadevcosta.history.infrastructure.persistence;

import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

/**
 * Row in {@code processed_events}. Insert-only: {@link #isNew()} is always {@code true} and a
 * repeated insert (PK clash) is swallowed by {@code JdbcProcessedEventStore} as "already handled".
 */
@Table("processed_events")
public class ProcessedEventEntity implements Persistable<String> {

    @Id
    private String eventId;
    private LocalDateTime processedAt;

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }

    @Override
    public String getId() {
        return eventId;
    }

    @Override
    public boolean isNew() {
        return true;
    }
}
