package com.biadevcosta.history.domain;

/** Lifecycle of an appointment, as reported by scheduling-service's events. */
public enum AppointmentStatus {
    SCHEDULED,
    COMPLETED,
    CANCELLED
}
