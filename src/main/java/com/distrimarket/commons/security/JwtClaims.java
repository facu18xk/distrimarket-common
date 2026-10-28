package com.distrimarket.commons.security;

/**
 * Nombres de los datos que viajan dentro del token JWT. El emisor (auth) y todos los
 * microservicios que validan tokens comparten estas constantes.
 */
public final class JwtClaims {

    public static final String ROLES = "roles";
    public static final String USER_ID = "uid";
    public static final String PERSONA_ID = "idPersona";
    public static final String EMPLEADO_ID = "idEmpleado";
    /** Depósito asignado al empleado (si tiene): vendedor, comprador y depositero operan sobre él. */
    public static final String DEPOSITO_ID = "idDeposito";

    private JwtClaims() {
    }
}
