package com.biadevcosta.history.infrastructure.messaging;

import com.biadevcosta.history.application.command.RecordEventCommand;
import com.biadevcosta.history.application.usecase.RecordAppointmentUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AppointmentEventListenerTest {

    @Mock
    RecordAppointmentUseCase useCase;

    @Test
    void onEvent_mapsMessageOntoCommand_andRunsTheUseCase() {
        LocalDateTime when = LocalDateTime.of(2030, 12, 1, 10, 30);

        new AppointmentEventListener(useCase).onEvent(new AppointmentEventMessage(
                "AppointmentCreated", "evt-1", "apt-1", "pat-1", "doc-1", when, "SCHEDULED"));

        ArgumentCaptor<RecordEventCommand> command = ArgumentCaptor.forClass(RecordEventCommand.class);
        verify(useCase).apply(command.capture());
        assertThat(command.getValue().type()).isEqualTo("AppointmentCreated");
        assertThat(command.getValue().eventId()).isEqualTo("evt-1");
        assertThat(command.getValue().appointmentId()).isEqualTo("apt-1");
        assertThat(command.getValue().patientId()).isEqualTo("pat-1");
        assertThat(command.getValue().doctorId()).isEqualTo("doc-1");
        assertThat(command.getValue().scheduledAt()).isEqualTo(when);
        assertThat(command.getValue().status()).isEqualTo("SCHEDULED");
    }
}
