package com.distrimarket.commons.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Cadena de filtros stateless común a los microservicios. Se activa solo con
 * {@code security.enabled=true}; cada servicio declara sus rutas públicas y roles
 * en application.yml. No usa sesiones ni formularios: cada pedido trae su token.
 */
@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
@ConditionalOnClass({SecurityFilterChain.class, JwtDecoder.class})
@ConditionalOnProperty(prefix = "security", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(ServiceSecurityProperties.class)
public class ResourceServerSecurityConfig {

    @Bean
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            ServiceSecurityProperties properties,
            JwtAuthenticationConverter jwtAuthenticationConverter,
            SecurityErrorHandler errorHandler) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> {
                    properties.publicPaths().forEach(path -> auth.requestMatchers(path).permitAll());
                    auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();
                    for (ServiceSecurityProperties.UrlRule rule : properties.rules()) {
                        String[] roles = rule.roles().stream().map(Enum::name).toArray(String[]::new);
                        if (rule.methods().isEmpty()) {
                            var matcher = auth.requestMatchers(rule.pattern());
                            if (roles.length == 0) {
                                matcher.authenticated();
                            } else {
                                matcher.hasAnyRole(roles);
                            }
                        } else {
                            for (String method : rule.methods()) {
                                var matcher = auth.requestMatchers(
                                        HttpMethod.valueOf(method.toUpperCase()), rule.pattern());
                                if (roles.length == 0) {
                                    matcher.authenticated();
                                } else {
                                    matcher.hasAnyRole(roles);
                                }
                            }
                        }
                    }
                    auth.anyRequest().authenticated();
                })
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                        .authenticationEntryPoint(errorHandler)
                        .accessDeniedHandler(errorHandler))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(errorHandler)
                        .accessDeniedHandler(errorHandler));
        return http.build();
    }

    @Bean
    @ConditionalOnMissingBean(SecurityErrorHandler.class)
    public SecurityErrorHandler securityErrorHandler(ObjectMapper objectMapper) {
        return new SecurityErrorHandler(objectMapper);
    }

}
