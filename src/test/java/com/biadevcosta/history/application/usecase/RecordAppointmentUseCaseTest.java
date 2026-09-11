package com.biadevcosta.history.application.usecase;

import com.biadevcosta.history.application.command.RecordEventCommand;
import com.biadevcosta.history.application.port.HistoryRepository;
import com.biadevcosta.history.application.port.ProcessedEventStore;
import com.biadevcosta.history.domain.AppointmentRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecordAppointmentUseCaseTest {

    @Mock
    HistoryRepository historyRepository;
    @Mock
    ProcessedEventStore processedEvents;

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2030-01-01T12:00:00Z"), ZoneOffset.UTC);

    private RecordAppointmentUseCase useCase() {
        return new RecordAppointmentUseCase(historyRepository, processedEvents, CLOCK);
    }

    private static RecordEventCommand command(String type) {
        return new RecordEventCommand(type, "evt-1", "apt-1", "pat-1", "doc-1",
                LocalDateTime.of(2030, 6, 1, 10, 0), "SCHEDULED");
    }

    @Test
    void alreadyProcessedEvent_isIdempotent_noUpsert() {
        when(processedEvents.exists("evt-1")).thenReturn(true);

        useCase().apply(command("AppointmentCreated"));

        verify(historyRepository, never()).upsert(any());
        verify(processedEvents, never()).markProcessed(any(), any());
    }

    @Test
    void createdEvent_upsertsRecord_thenMarksProcessed() {
        when(processedEvents.exists("evt-1")).thenReturn(false);

        useCase().apply(command("AppointmentCreated"));

        InOrder order = inOrder(processedEvents, historyRepository);
        order.verify(processedEvents).exists("evt-1");
        order.verify(historyRepository).upsert(any());
        order.verify(processedEvents).markProcessed("evt-1", LocalDateTime.now(CLOCK));
    }

    @Test
    void updatedEvent_alsoUpserts_sameAsCreated() {
        when(processedEvents.exists("evt-1")).thenReturn(false);

        useCase().apply(command("AppointmentUpdated"));

        ArgumentCaptor<AppointmentRecord> captor = ArgumentCaptor.forClass(AppointmentRecord.class);
        verify(historyRepository).upsert(captor.capture());
        assertThat(captor.getValue().appointmentId()).isEqualTo("apt-1");
        assertThat(captor.getValue().patientId()).isEqualTo("pat-1");
    }
}
