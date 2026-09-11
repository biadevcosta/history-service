package com.biadevcosta.history.domain;

import com.biadevcosta.history.domain.exception.InvalidHistoryRecordException;

import java.time.LocalDateTime;

/**
 * Read-model row: the current state of one appointment, as last reported by the events coming
 * from scheduling-service. This class has no framework dependency.
 */
public record AppointmentRecord(String appointmentId, String patientId, String doctorId,
                                LocalDateTime scheduledAt, AppointmentStatus status) {

    public AppointmentRecord {
        if (isBlank(appointmentId) || isBlank(patientId) || isBlank(doctorId)) {
            throw new InvalidHistoryRecordException("appointmentId, patientId and doctorId are required");
        }
        if (scheduledAt == null) {
            throw new InvalidHistoryRecordException("scheduledAt is required");
        }
        if (status == null) {
            throw new InvalidHistoryRecordException("status is required");
        }
    }

    /** {@code true} when the appointment is still scheduled and its date is ahead of {@code now}. */
    public boolean isFuture(LocalDateTime now) {
        return status == AppointmentStatus.SCHEDULED && scheduledAt.isAfter(now);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
