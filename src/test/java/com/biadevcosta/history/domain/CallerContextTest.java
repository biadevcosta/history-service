package com.biadevcosta.history.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CallerContextTest {

    @Test
    void patientCaller_alwaysGetsOwnPatientId_ignoringRequested() {
        CallerContext caller = new CallerContext("PATIENT", "pat-1");

        assertThat(caller.effectivePatientId("pat-2")).isEqualTo("pat-1");
        assertThat(caller.effectivePatientId(null)).isEqualTo("pat-1");
    }

    @Test
    void doctorCaller_usesRequestedPatientId() {
        CallerContext caller = new CallerContext("DOCTOR", null);

        assertThat(caller.effectivePatientId("pat-2")).isEqualTo("pat-2");
    }

    @Test
    void nurseCaller_usesRequestedPatientId() {
        CallerContext caller = new CallerContext("NURSE", null);

        assertThat(caller.effectivePatientId("pat-3")).isEqualTo("pat-3");
    }
}
