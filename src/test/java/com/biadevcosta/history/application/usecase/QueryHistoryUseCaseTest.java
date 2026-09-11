package com.biadevcosta.history.application.usecase;

import com.biadevcosta.history.application.EnrichedAppointment;
import com.biadevcosta.history.application.port.HistoryRepository;
import com.biadevcosta.history.application.port.UserDirectory;
import com.biadevcosta.history.domain.AppointmentRecord;
import com.biadevcosta.history.domain.AppointmentStatus;
import com.biadevcosta.history.domain.CallerContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QueryHistoryUseCaseTest {

    @Mock
    HistoryRepository historyRepository;
    @Mock
    UserDirectory userDirectory;

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2030-06-01T00:00:00Z"), ZoneOffset.UTC);

    private QueryHistoryUseCase useCase() {
        return new QueryHistoryUseCase(historyRepository, userDirectory, CLOCK);
    }

    private static AppointmentRecord record(String id, AppointmentStatus status, LocalDateTime scheduledAt) {
        return new AppointmentRecord(id, "pat-1", "doc-1", scheduledAt, status);
    }

    @Test
    void patientCaller_ignoresRequestedId_usesOwn() {
        CallerContext caller = new CallerContext("PATIENT", "pat-1");
        when(historyRepository.findByPatient("pat-1")).thenReturn(List.of());

        useCase().history(caller, "someone-else");

        org.mockito.Mockito.verify(historyRepository).findByPatient("pat-1");
    }

    @Test
    void doctorCaller_usesRequestedId() {
        CallerContext caller = new CallerContext("DOCTOR", null);
        when(historyRepository.findByPatient("pat-9")).thenReturn(List.of());

        useCase().history(caller, "pat-9");

        org.mockito.Mockito.verify(historyRepository).findByPatient("pat-9");
    }

    @Test
    void future_filtersOutPastAndNonScheduled() {
        CallerContext caller = new CallerContext("DOCTOR", null);
        when(historyRepository.findByPatient("pat-1")).thenReturn(List.of(
                record("apt-future", AppointmentStatus.SCHEDULED, LocalDateTime.of(2030, 7, 1, 9, 0)),
                record("apt-past", AppointmentStatus.SCHEDULED, LocalDateTime.of(2030, 1, 1, 9, 0)),
                record("apt-completed", AppointmentStatus.COMPLETED, LocalDateTime.of(2030, 8, 1, 9, 0))));
        when(userDirectory.findUserName(org.mockito.ArgumentMatchers.any())).thenReturn(Optional.of("Name"));

        List<EnrichedAppointment> result = useCase().future(caller, "pat-1");

        assertThat(result).extracting(EnrichedAppointment::appointmentId).containsExactly("apt-future");
    }

    @Test
    void enrich_resolvesPatientAndDoctorNames() {
        CallerContext caller = new CallerContext("DOCTOR", null);
        when(historyRepository.findByPatient("pat-1")).thenReturn(List.of(
                record("apt-1", AppointmentStatus.SCHEDULED, LocalDateTime.of(2030, 7, 1, 9, 0))));
        when(userDirectory.findUserName("pat-1")).thenReturn(Optional.of("John Doe"));
        when(userDirectory.findUserName("doc-1")).thenReturn(Optional.of("Dr. Smith"));

        List<EnrichedAppointment> result = useCase().history(caller, "pat-1");

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().patientName()).isEqualTo("John Doe");
        assertThat(result.getFirst().doctorName()).isEqualTo("Dr. Smith");
    }

    @Test
    void enrich_missingName_fallsBackToUnknown() {
        CallerContext caller = new CallerContext("DOCTOR", null);
        when(historyRepository.findByPatient("pat-1")).thenReturn(List.of(
                record("apt-1", AppointmentStatus.SCHEDULED, LocalDateTime.of(2030, 7, 1, 9, 0))));
        when(userDirectory.findUserName("pat-1")).thenReturn(Optional.empty());
        when(userDirectory.findUserName("doc-1")).thenReturn(Optional.empty());

        List<EnrichedAppointment> result = useCase().history(caller, "pat-1");

        assertThat(result.getFirst().patientName()).isEqualTo("Unknown");
        assertThat(result.getFirst().doctorName()).isEqualTo("Unknown");
    }
}
