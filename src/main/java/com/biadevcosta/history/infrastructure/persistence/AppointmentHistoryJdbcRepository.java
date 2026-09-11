package com.biadevcosta.history.infrastructure.persistence;

import org.springframework.data.repository.CrudRepository;

import java.util.List;

/**
 * Spring Data JDBC repository over {@link AppointmentHistoryEntity}. The entity carries a
 * {@code @Version} field, so {@code save()} inserts when it is null and updates otherwise (the id
 * is the appointment id, not database-generated).
 */
public interface AppointmentHistoryJdbcRepository extends CrudRepository<AppointmentHistoryEntity, String> {

    List<AppointmentHistoryEntity> findByPatientId(String patientId);
}
