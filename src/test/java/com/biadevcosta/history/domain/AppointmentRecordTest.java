package com.biadevcosta.history.domain;

import com.biadevcosta.history.domain.exception.InvalidHistoryRecordException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AppointmentRecordTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2030, 1, 1, 12, 0);

    private static AppointmentRecord record(AppointmentStatus status, LocalDateTime scheduledAt) {
        return new AppointmentRecord("apt-1", "pat-1", "doc-1", scheduledAt, status);
    }

    @Test
    void scheduledInTheFuture_isFuture() {
        assertThat(record(AppointmentStatus.SCHEDULED, NOW.plusDays(1)).isFuture(NOW)).isTrue();
    }

    @Test
    void scheduledInThePast_isNotFuture() {
        assertThat(record(AppointmentStatus.SCHEDULED, NOW.minusDays(1)).isFuture(NOW)).isFalse();
    }

    @Test
    void completedEvenIfDateAhead_isNotFuture() {
        assertThat(record(AppointmentStatus.COMPLETED, NOW.plusDays(1)).isFuture(NOW)).isFalse();
    }

    @Test
    void cancelledEvenIfDateAhead_isNotFuture() {
        assertThat(record(AppointmentStatus.CANCELLED, NOW.plusDays(1)).isFuture(NOW)).isFalse();
    }

    @Test
    void blankPatientId_isRejected() {
        assertThatThrownBy(() -> new AppointmentRecord("apt-1", " ", "doc-1", NOW, AppointmentStatus.SCHEDULED))
                .isInstanceOf(InvalidHistoryRecordException.class);
    }

    @Test
    void nullScheduledAt_isRejected() {
        assertThatThrownBy(() -> new AppointmentRecord("apt-1", "pat-1", "doc-1", null, AppointmentStatus.SCHEDULED))
                .isInstanceOf(InvalidHistoryRecordException.class);
    }

    @Test
    void nullStatus_isRejected() {
        assertThatThrownBy(() -> new AppointmentRecord("apt-1", "pat-1", "doc-1", NOW, null))
                .isInstanceOf(InvalidHistoryRecordException.class);
    }
}
