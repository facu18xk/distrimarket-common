package com.distrimarket.commons.security;

import com.distrimarket.commons.enums.RolUsuario;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Reglas de seguridad de un microservicio, leídas de {@code application.yml}.
 *
 * @param publicPaths rutas sin token (documentación, health, error)
 * @param rules       reglas por URL; el orden importa
 */
@ConfigurationProperties(prefix = "security")
public record ServiceSecurityProperties(List<String> publicPaths, List<UrlRule> rules) {

    public ServiceSecurityProperties {
        publicPaths = publicPaths == null ? List.of() : List.copyOf(publicPaths);
        rules = rules == null ? List.of() : List.copyOf(rules);
    }

    /**
     * @param pattern ruta Ant, por ejemplo {@code /**}
     * @param methods métodos HTTP a los que aplica; vacío = todos
     * @param roles   roles permitidos
     */
    public record UrlRule(String pattern, List<String> methods, List<RolUsuario> roles) {
        public UrlRule {
            methods = methods == null ? List.of() : List.copyOf(methods);
            roles = roles == null ? List.of() : List.copyOf(roles);
        }
    }
}
