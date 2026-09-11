package com.biadevcosta.history.infrastructure.persistence;

import com.biadevcosta.history.domain.AppointmentRecord;
import com.biadevcosta.history.domain.AppointmentStatus;

/** Translates between the {@link AppointmentRecord} domain object and its persistence row. */
final class AppointmentHistoryMapper {

    private AppointmentHistoryMapper() {
    }

    static AppointmentHistoryEntity toNewEntity(AppointmentRecord record) {
        AppointmentHistoryEntity entity = new AppointmentHistoryEntity();
        copyInto(record, entity);
        return entity;
    }

    /** Copies the record's state onto an existing row, keeping its {@code version}. */
    static void copyInto(AppointmentRecord record, AppointmentHistoryEntity entity) {
        entity.setAppointmentId(record.appointmentId());
        entity.setPatientId(record.patientId());
        entity.setDoctorId(record.doctorId());
        entity.setScheduledAt(record.scheduledAt());
        entity.setStatus(record.status().name());
    }

    static AppointmentRecord toDomain(AppointmentHistoryEntity entity) {
        return new AppointmentRecord(
                entity.getAppointmentId(), entity.getPatientId(), entity.getDoctorId(),
                entity.getScheduledAt(), AppointmentStatus.valueOf(entity.getStatus()));
    }
}
