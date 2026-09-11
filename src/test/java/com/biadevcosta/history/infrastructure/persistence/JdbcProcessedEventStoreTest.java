package com.biadevcosta.history.infrastructure.persistence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JdbcProcessedEventStoreTest {

    @Mock
    ProcessedEventJdbcRepository jdbc;

    private JdbcProcessedEventStore store() {
        return new JdbcProcessedEventStore(jdbc);
    }

    @Test
    void exists_delegatesToExistsById() {
        when(jdbc.existsById("evt-1")).thenReturn(true);

        assertThat(store().exists("evt-1")).isTrue();
    }

    @Test
    void markProcessed_savesTheGivenEventAndTimestamp() {
        LocalDateTime now = LocalDateTime.of(2030, 1, 1, 12, 0);

        store().markProcessed("evt-1", now);

        ArgumentCaptor<ProcessedEventEntity> captor = ArgumentCaptor.forClass(ProcessedEventEntity.class);
        verify(jdbc).save(captor.capture());
        assertThat(captor.getValue().getEventId()).isEqualTo("evt-1");
        assertThat(captor.getValue().getProcessedAt()).isEqualTo(now);
    }

    @Test
    void markProcessed_swallowsDuplicateKey() {
        when(jdbc.save(any())).thenThrow(new DuplicateKeyException("PK clash"));

        assertThatCode(() -> store().markProcessed("evt-1", LocalDateTime.now()))
                .doesNotThrowAnyException();
    }
}
