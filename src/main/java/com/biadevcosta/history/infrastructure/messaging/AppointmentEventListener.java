package com.biadevcosta.history.infrastructure.messaging;

import com.biadevcosta.history.application.command.RecordEventCommand;
import com.biadevcosta.history.application.usecase.RecordAppointmentUseCase;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Inbound adapter: maps an {@code appointment-events} record onto a {@link RecordEventCommand} and
 * runs the use case. Swapping Kafka for another bus means a new adapter here, nothing else — the
 * use case never sees a {@code ConsumerRecord} or this message type.
 */
@Component
public class AppointmentEventListener {

    private final RecordAppointmentUseCase useCase;

    public AppointmentEventListener(RecordAppointmentUseCase useCase) {
        this.useCase = useCase;
    }

    @KafkaListener(topics = "${app.kafka.topic}")
    public void onEvent(AppointmentEventMessage message) {
        useCase.apply(new RecordEventCommand(
                message.type(), message.eventId(), message.appointmentId(),
                message.patientId(), message.doctorId(), message.scheduledAt(), message.status()));
    }
}
