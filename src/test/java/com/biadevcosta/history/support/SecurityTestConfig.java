package com.biadevcosta.history.support;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;

/**
 * Test security wiring: an in-memory RSA keypair signs tokens ({@link JwtEncoder}) and the
 * matching public key validates them. The {@code @Primary} decoder replaces the one built from
 * {@code public.pem}, so tests exercise the real security filter chain end to end.
 */
@TestConfiguration(proxyBeanMethods = false)
public class SecurityTestConfig {

    private static final KeyPair KEY_PAIR = generate();

    private static KeyPair generate() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    @Bean
    @Primary
    JwtDecoder testJwtDecoder() {
        return NimbusJwtDecoder.withPublicKey((RSAPublicKey) KEY_PAIR.getPublic()).build();
    }

    @Bean
    JwtEncoder testJwtEncoder() {
        RSAKey jwk = new RSAKey.Builder((RSAPublicKey) KEY_PAIR.getPublic())
                .privateKey(KEY_PAIR.getPrivate())
                .keyID("test-key")
                .build();
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(jwk)));
    }

    @Bean
    TestTokens testTokens(JwtEncoder encoder) {
        return new TestTokens(encoder);
    }
}
