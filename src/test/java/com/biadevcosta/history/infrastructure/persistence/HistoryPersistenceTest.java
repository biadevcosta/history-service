package com.biadevcosta.history.infrastructure.persistence;

import com.biadevcosta.history.domain.AppointmentRecord;
import com.biadevcosta.history.domain.AppointmentStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HistoryPersistenceTest {

    @Mock
    AppointmentHistoryJdbcRepository jdbc;

    private static AppointmentRecord sample(String id) {
        return new AppointmentRecord(id, "pat-1", "doc-1",
                LocalDateTime.of(2030, 1, 1, 10, 0), AppointmentStatus.SCHEDULED);
    }

    @Test
    void mapper_roundTripsThroughEntity() {
        AppointmentRecord original = sample("apt-1");

        AppointmentHistoryEntity entity = AppointmentHistoryMapper.toNewEntity(original);
        AppointmentRecord back = AppointmentHistoryMapper.toDomain(entity);

        assertThat(entity.getStatus()).isEqualTo("SCHEDULED");
        assertThat(back).isEqualTo(original);
    }

    @Test
    void upsert_newAppointment_insertsWithNullVersion() {
        when(jdbc.findById("apt-1")).thenReturn(Optional.empty());
        when(jdbc.save(any())).thenAnswer(i -> i.getArgument(0));
        var repository = new HistoryRepositoryImpl(jdbc);

        repository.upsert(sample("apt-1"));

        ArgumentCaptor<AppointmentHistoryEntity> captor = ArgumentCaptor.forClass(AppointmentHistoryEntity.class);
        verify(jdbc).save(captor.capture());
        assertThat(captor.getValue().getVersion()).isNull();
        assertThat(captor.getValue().getAppointmentId()).isEqualTo("apt-1");
    }

    @Test
    void upsert_existingAppointment_keepsVersionForUpdate() {
        AppointmentHistoryEntity existing = AppointmentHistoryMapper.toNewEntity(sample("apt-1"));
        existing.setVersion(3L);
        when(jdbc.findById("apt-1")).thenReturn(Optional.of(existing));
        when(jdbc.save(any())).thenAnswer(i -> i.getArgument(0));
        var repository = new HistoryRepositoryImpl(jdbc);

        AppointmentRecord updated = new AppointmentRecord("apt-1", "pat-1", "doc-1",
                LocalDateTime.of(2030, 1, 1, 10, 0), AppointmentStatus.CANCELLED);
        repository.upsert(updated);

        ArgumentCaptor<AppointmentHistoryEntity> captor = ArgumentCaptor.forClass(AppointmentHistoryEntity.class);
        verify(jdbc).save(captor.capture());
        assertThat(captor.getValue().getVersion()).isEqualTo(3L);
        assertThat(captor.getValue().getStatus()).isEqualTo("CANCELLED");
    }

    @Test
    void findByPatient_mapsEntitiesToDomain() {
        when(jdbc.findByPatientId("pat-1")).thenReturn(List.of(AppointmentHistoryMapper.toNewEntity(sample("apt-1"))));
        var repository = new HistoryRepositoryImpl(jdbc);

        List<AppointmentRecord> found = repository.findByPatient("pat-1");

        assertThat(found).hasSize(1);
        assertThat(found.getFirst().patientId()).isEqualTo("pat-1");
    }
}
