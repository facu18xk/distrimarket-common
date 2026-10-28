package com.distrimarket.commons.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;

/**
 * El usuario del pedido en curso, leído del token que dejó Spring Security en el contexto
 * (el "objeto principal"). Sirve a cualquier capa del microservicio sin pasarlo por parámetro.
 */
public final class UsuarioActual {

    private UsuarioActual() {
    }

    private static Optional<Jwt> jwt() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getPrincipal() instanceof Jwt jwt ? Optional.of(jwt) : Optional.empty();
    }

    private static Optional<Long> numero(String claim) {
        return jwt().map(j -> j.getClaim(claim)).map(valor -> valor instanceof Number n ? n.longValue() : Long.valueOf(valor.toString()));
    }

    public static Optional<String> username() {
        return jwt().map(Jwt::getSubject);
    }

    public static Optional<Long> idEmpleado() {
        return numero(JwtClaims.EMPLEADO_ID);
    }

    /** Depósito asignado al empleado; vacío si no tiene (por ejemplo, un ADMIN). */
    public static Optional<Long> idDeposito() {
        return numero(JwtClaims.DEPOSITO_ID);
    }

    public static boolean tieneRol(String rol) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_" + rol));
    }

    public static boolean esAdmin() {
        return tieneRol("ADMIN");
    }

    /**
     * Depósito con el que se opera: un ADMIN usa el que pidió; cualquier otro, siempre el suyo
     * (si tiene uno asignado), aunque haya pedido otro. Así no se puede mover stock de otro depósito.
     */
    public static Long depositoParaOperar(Long pedido) {
        if (esAdmin()) {
            return pedido != null ? pedido : idDeposito().orElse(null);
        }
        return idDeposito().orElse(pedido);
    }
}
