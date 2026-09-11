package com.biadevcosta.history.infrastructure.messaging;

import java.time.LocalDateTime;

/**
 * Local copy of scheduling-service's Kafka payload for topic {@code appointment-events}. The
 * consumer is configured to always deserialize into this type (ignores {@code __TypeId__}), so the
 * two services can evolve independently.
 */
public record AppointmentEventMessage(String type,
                                      String eventId,
                                      String appointmentId,
                                      String patientId,
                                      String doctorId,
                                      LocalDateTime scheduledAt,
                                      String status) {
}
