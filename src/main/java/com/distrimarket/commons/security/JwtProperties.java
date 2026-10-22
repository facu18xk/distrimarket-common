package com.distrimarket.commons.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.charset.StandardCharsets;

/**
 * Configuración compartida para validar tokens: la misma clave HS256 y el mismo emisor
 * que usa el microservicio de auth.
 *
 * @param secret  clave HMAC; HS256 exige al menos 256 bits (32 bytes)
 * @param issuer  valor esperado del claim {@code iss}
 */
@ConfigurationProperties(prefix = "security.jwt")
public record JwtProperties(String secret, String issuer) {

    private static final int MIN_SECRET_BYTES = 32;

    public JwtProperties {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "security.jwt.secret debe tener al menos " + MIN_SECRET_BYTES + " caracteres.");
        }
        if (issuer == null || issuer.isBlank()) {
            throw new IllegalStateException("security.jwt.issuer es obligatorio.");
        }
    }
}
