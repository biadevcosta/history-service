package com.biadevcosta.history.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

/** Binds {@code security.jwt.*} from {@code application.yaml}. */
@ConfigurationProperties(prefix = "security.jwt")
public record JwtProperties(Resource publicKey, String issuer) {
}
