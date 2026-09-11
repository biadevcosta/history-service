package com.biadevcosta.history.application.port;

import com.biadevcosta.history.domain.AppointmentRecord;

import java.util.List;

/** The read model itself: current state per appointment, queryable by patient. */
public interface HistoryRepository {

    /** Inserts a brand-new appointment or overwrites the row for an existing {@code appointmentId}. */
    void upsert(AppointmentRecord record);

    List<AppointmentRecord> findByPatient(String patientId);
}
