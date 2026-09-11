package com.biadevcosta.history.infrastructure.client;

import com.biadevcosta.history.application.port.UserDirectory;
import com.biadevcosta.history.infrastructure.config.IdentityClientProperties;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.Optional;

/**
 * {@link UserDirectory} backed by {@code identity-service} over HTTP. Results are cached
 * ({@code user-names}) so a history query for many appointments doesn't hammer identity per row.
 */
@Component
public class IdentityHttpUserDirectory implements UserDirectory {

    private final RestClient restClient;

    public IdentityHttpUserDirectory(RestClient.Builder builder, IdentityClientProperties properties) {
        this.restClient = builder.baseUrl(properties.baseUrl()).build();
    }

    @Override
    @Cacheable("user-names")
    public Optional<String> findUserName(String userId) {
        try {
            IdentityUser user = restClient.get()
                    .uri("/users/{id}", userId)
                    .retrieve()
                    .body(IdentityUser.class);
            return Optional.ofNullable(user).map(IdentityUser::name);
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        }
    }

    /** Shape of identity-service's {@code GET /users/{id}} response (only what we need). */
    private record IdentityUser(String id, String name, String email, String role) {
    }
}
