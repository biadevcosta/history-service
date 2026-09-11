package com.biadevcosta.history.domain;

/**
 * Identity of whoever is calling a query, taken from the JWT by the controller and passed into
 * the use case as a plain parameter — the core never reads {@code SecurityContextHolder} directly.
 */
public record CallerContext(String role, String patientId) {

    /**
     * A PATIENT can only ever see their own history: the token's {@code patientId} wins regardless
     * of what was asked for. DOCTOR/NURSE use whatever patient id was requested.
     */
    public String effectivePatientId(String requestedPatientId) {
        return "PATIENT".equals(role) ? patientId : requestedPatientId;
    }
}
