package com.biadevcosta.history.application.command;

import java.time.LocalDateTime;

/**
 * Primitives-only command built by the Kafka listener from an {@code AppointmentEventMessage}.
 * The use case never sees the Kafka message type.
 */
public record RecordEventCommand(String type, String eventId, String appointmentId,
                                 String patientId, String doctorId,
                                 LocalDateTime scheduledAt, String status) {
}
