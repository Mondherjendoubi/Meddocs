package com.meddocs.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Type-safe binding for the {@code meddocs.jwt.*} properties.
 *
 * @param secret       HMAC signing secret; must be >= 32 bytes for HS256.
 * @param expirationMs token lifetime in milliseconds (relaxed binding: {@code expiration-ms}).
 */
@ConfigurationProperties("meddocs.jwt")
public record JwtProperties(String secret, long expirationMs) {
}
