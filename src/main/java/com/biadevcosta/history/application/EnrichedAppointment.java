package com.biadevcosta.history.application;

import com.biadevcosta.history.domain.AppointmentStatus;

import java.time.LocalDateTime;

/** Query output: an {@code AppointmentRecord} with patient/doctor ids resolved to display names. */
public record EnrichedAppointment(String appointmentId, String patientName, String doctorName,
                                  LocalDateTime scheduledAt, AppointmentStatus status) {
}
