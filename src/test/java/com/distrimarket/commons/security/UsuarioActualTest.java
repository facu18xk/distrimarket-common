package com.distrimarket.commons.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class UsuarioActualTest {

    /** Simula el token de un usuario con su depósito asignado (o sin depósito si es null). */
    static void sesion(Long idDeposito, String... roles) {
        Jwt.Builder jwt = Jwt.withTokenValue("t").header("alg", "HS256").subject("prueba").claim(JwtClaims.EMPLEADO_ID, 7);
        if (idDeposito != null) {
            jwt.claim(JwtClaims.DEPOSITO_ID, idDeposito);
        }
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt.build(),
                Arrays.stream(roles).map(r -> new SimpleGrantedAuthority("ROLE_" + r)).toList()));
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void depositeroSiempreOperaEnSuDeposito() {
        sesion(1L, "DEPOSITERO");

        assertThat(UsuarioActual.depositoParaOperar(2L)).isEqualTo(1L);
        assertThat(UsuarioActual.depositoParaOperar(null)).isEqualTo(1L);
        assertThat(UsuarioActual.idEmpleado()).contains(7L);
        assertThat(UsuarioActual.esAdmin()).isFalse();
    }

    @Test
    void adminUsaElDepositoPedido() {
        sesion(1L, "ADMIN");

        assertThat(UsuarioActual.depositoParaOperar(2L)).isEqualTo(2L);
        assertThat(UsuarioActual.depositoParaOperar(null)).isEqualTo(1L);
    }

    @Test
    void sinDepositoAsignadoUsaElPedido() {
        sesion(null, "VENDEDOR");

        assertThat(UsuarioActual.idDeposito()).isEmpty();
        assertThat(UsuarioActual.depositoParaOperar(3L)).isEqualTo(3L);
    }

    @Test
    void sinSesionNoHayUsuario() {
        assertThat(UsuarioActual.username()).isEmpty();
        assertThat(UsuarioActual.depositoParaOperar(3L)).isEqualTo(3L);
    }
}
