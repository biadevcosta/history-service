package com.biadevcosta.history.application.port;

import java.util.Optional;

/**
 * Resolves a user's display name by id. Absence (unknown id, lookup failure) is {@code empty()} —
 * the use case, not the adapter, decides the "Unknown" fallback.
 */
public interface UserDirectory {

    Optional<String> findUserName(String userId);
}
