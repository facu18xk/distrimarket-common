package com.distrimarket.commons.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * Decodificador JWT compartido por los microservicios que habilitan seguridad.
 * Verifica firma HS256, vencimiento y emisor, y convierte el claim {@code roles}
 * en autoridades {@code ROLE_ADMIN}, {@code ROLE_VENDEDOR}, etc.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({JwtDecoder.class, JwtAuthenticationConverter.class})
@ConditionalOnProperty(prefix = "security", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(JwtProperties.class)
public class JwtDecoderConfig {

    private static final String ROLE_PREFIX = "ROLE_";

    private final SecretKey key;
    private final String issuer;

    public JwtDecoderConfig(JwtProperties properties) {
        this.key = new SecretKeySpec(
                properties.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        this.issuer = properties.issuer();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuer));
        return decoder;
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter roles = new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName(JwtClaims.ROLES);
        roles.setAuthorityPrefix(ROLE_PREFIX);
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(roles);
        return converter;
    }
}
