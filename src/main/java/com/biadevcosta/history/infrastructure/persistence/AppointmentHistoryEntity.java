package com.biadevcosta.history.infrastructure.persistence;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

/** Row in {@code appointment_history}, keyed by {@code appointmentId}. Mapped by {@link AppointmentHistoryMapper}. */
@Table("appointment_history")
public class AppointmentHistoryEntity {

    @Id
    private String appointmentId;

    /** Null on a brand-new row, so Spring Data JDBC issues an INSERT; set afterwards -> UPDATE. */
    @Version
    private Long version;

    private String patientId;
    private String doctorId;
    private LocalDateTime scheduledAt;
    private String status;

    public String getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(String appointmentId) {
        this.appointmentId = appointmentId;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(String doctorId) {
        this.doctorId = doctorId;
    }

    public LocalDateTime getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(LocalDateTime scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
