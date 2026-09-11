package com.biadevcosta.history.support;

import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/** Mints signed RS256 tokens for tests, mirroring the claims identity-service issues. */
public class TestTokens {

    private final JwtEncoder encoder;

    public TestTokens(JwtEncoder encoder) {
        this.encoder = encoder;
    }

    public String forUser(String userId, String role) {
        return forPatient(userId, role, null);
    }

    public String forPatient(String userId, String role, String patientId) {
        Instant now = Instant.now();
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer("hospital-identity")
                .subject(userId)
                .claim("role", role)
                .issuedAt(now)
                .expiresAt(now.plus(10, ChronoUnit.MINUTES));
        if (patientId != null) {
            claims.claim("patientId", patientId);
        }
        return encoder.encode(JwtEncoderParameters.from(claims.build())).getTokenValue();
    }
}
