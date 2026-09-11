package com.biadevcosta.history.infrastructure.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Turns on method-level caching (used by {@code IdentityHttpUserDirectory}). The cache itself
 * (Caffeine, name {@code user-names}, size/TTL) is configured in {@code application.yaml}.
 */
@Configuration
@EnableCaching
public class CacheConfig {
}
