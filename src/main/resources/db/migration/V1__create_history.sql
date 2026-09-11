CREATE TABLE appointment_history (
    appointment_id VARCHAR(36)  NOT NULL PRIMARY KEY,
    version        BIGINT       NULL,
    patient_id     VARCHAR(36)  NOT NULL,
    doctor_id      VARCHAR(36)  NOT NULL,
    scheduled_at   DATETIME     NOT NULL,
    status         VARCHAR(20)  NOT NULL
);
CREATE INDEX idx_history_patient ON appointment_history (patient_id);

CREATE TABLE processed_events (
    event_id     VARCHAR(36) NOT NULL PRIMARY KEY,
    processed_at DATETIME    NOT NULL
);
