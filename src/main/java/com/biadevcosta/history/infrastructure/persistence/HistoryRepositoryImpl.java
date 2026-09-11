package com.biadevcosta.history.infrastructure.persistence;

import com.biadevcosta.history.application.port.HistoryRepository;
import com.biadevcosta.history.domain.AppointmentRecord;
import org.springframework.stereotype.Repository;

import java.util.List;

/** {@link HistoryRepository} backed by Spring Data JDBC. */
@Repository
public class HistoryRepositoryImpl implements HistoryRepository {

    private final AppointmentHistoryJdbcRepository jdbc;

    public HistoryRepositoryImpl(AppointmentHistoryJdbcRepository jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void upsert(AppointmentRecord record) {
        AppointmentHistoryEntity entity = jdbc.findById(record.appointmentId())
                .map(existing -> {
                    AppointmentHistoryMapper.copyInto(record, existing); // keep @Version -> UPDATE
                    return existing;
                })
                .orElseGet(() -> AppointmentHistoryMapper.toNewEntity(record)); // version null -> INSERT
        jdbc.save(entity);
    }

    @Override
    public List<AppointmentRecord> findByPatient(String patientId) {
        return jdbc.findByPatientId(patientId).stream().map(AppointmentHistoryMapper::toDomain).toList();
    }
}
