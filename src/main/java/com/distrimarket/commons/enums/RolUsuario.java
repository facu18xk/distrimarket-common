package com.distrimarket.commons.enums;

/**
 * Roles del sistema. Se guardan por nombre en la tabla {@code roles} y viajan en el token JWT
 * (claim {@code roles}); Spring Security los usa con el prefijo {@code ROLE_}.
 * Un usuario puede tener varios (por ejemplo ADMIN y VENDEDOR a la vez).
 */
public enum RolUsuario {
    /** Administra usuarios, roles y catálogos; accede a todos los módulos. */
    ADMIN,
    /** Módulo de ventas: clientes y facturas de venta. */
    VENDEDOR,
    /** Módulo de inventario: productos, existencias, ajustes y transferencias. */
    DEPOSITERO,
    /** Módulo de compras: proveedores, órdenes y facturas de compra. */
    COMPRADOR
}
