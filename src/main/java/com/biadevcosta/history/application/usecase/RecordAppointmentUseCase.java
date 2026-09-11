package com.biadevcosta.history.application.usecase;

import com.biadevcosta.history.application.command.RecordEventCommand;
import com.biadevcosta.history.application.port.HistoryRepository;
import com.biadevcosta.history.application.port.ProcessedEventStore;
import com.biadevcosta.history.domain.AppointmentRecord;
import com.biadevcosta.history.domain.AppointmentStatus;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Ingests one appointment event into the read model. Idempotent on {@code eventId}: a redelivered
 * event is a no-op. {@code Created} and {@code Updated} are handled the same way — both upsert
 * keyed by {@code appointmentId}, since the row only ever holds the appointment's current state.
 */
public class RecordAppointmentUseCase {

    private final HistoryRepository historyRepository;
    private final ProcessedEventStore processedEvents;
    private final Clock clock;

    public RecordAppointmentUseCase(HistoryRepository historyRepository,
                                    ProcessedEventStore processedEvents, Clock clock) {
        this.historyRepository = historyRepository;
        this.processedEvents = processedEvents;
        this.clock = clock;
    }

    public void apply(RecordEventCommand command) {
        if (processedEvents.exists(command.eventId())) {
            return;
        }
        AppointmentRecord record = new AppointmentRecord(
                command.appointmentId(), command.patientId(), command.doctorId(),
                command.scheduledAt(), AppointmentStatus.valueOf(command.status()));
        historyRepository.upsert(record);
        processedEvents.markProcessed(command.eventId(), LocalDateTime.now(clock));
    }
}
