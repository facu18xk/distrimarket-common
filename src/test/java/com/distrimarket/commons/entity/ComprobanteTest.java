package com.distrimarket.commons.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ComprobanteTest {

    @Test
    @DisplayName("Debe calcular correctamente el IVA dinámico al 10% (equivalente a subtotal / 11)")
    void testIvaDiezPorCiento() {
        FacturaCompraDetalle detalle = FacturaCompraDetalle.builder().build();
        detalle.setCantidad(1);
        detalle.setPrecioUnitario(new BigDecimal("110000.00"));
        detalle.setPorcentajeIva(new BigDecimal("10.00"));
        detalle.calcularSubtotal();

        assertEquals(new BigDecimal("110000.00"), detalle.getSubtotal());
        assertEquals(new BigDecimal("10000.00"), detalle.getMontoIva());
    }

    @Test
    @DisplayName("Debe calcular correctamente el IVA dinámico al 5% (equivalente a subtotal / 21)")
    void testIvaCincoPorCiento() {
        FacturaCompraDetalle detalle = FacturaCompraDetalle.builder().build();
        detalle.setCantidad(1);
        detalle.setPrecioUnitario(new BigDecimal("21000.00"));
        detalle.setPorcentajeIva(new BigDecimal("5.00"));
        detalle.calcularSubtotal();

        assertEquals(new BigDecimal("21000.00"), detalle.getSubtotal());
        assertEquals(new BigDecimal("1000.00"), detalle.getMontoIva());
    }

    @Test
    @DisplayName("Debe calcular 0 para productos exentos (IVA 0%)")
    void testIvaExento() {
        FacturaCompraDetalle detalle = FacturaCompraDetalle.builder().build();
        detalle.setCantidad(2);
        detalle.setPrecioUnitario(new BigDecimal("50000.00"));
        detalle.setPorcentajeIva(BigDecimal.ZERO);
        detalle.calcularSubtotal();

        assertEquals(new BigDecimal("100000.00"), detalle.getSubtotal());
        assertEquals(BigDecimal.ZERO, detalle.getMontoIva());
    }

    @Test
    @DisplayName("Debe soportar porcentajes de IVA totalmente configurables (ej. 8%) sin hardcodeo")
    void testIvaConfigurableOchoPorCiento() {
        FacturaCompraDetalle detalle = FacturaCompraDetalle.builder().build();
        detalle.setCantidad(1);
        detalle.setPrecioUnitario(new BigDecimal("1080.00"));
        detalle.setPorcentajeIva(new BigDecimal("8.00"));
        detalle.calcularSubtotal();

        // 1080 * 8 / 108 = 80.00
        assertEquals(new BigDecimal("80.00"), detalle.getMontoIva());
    }

    @Test
    @DisplayName("FacturaCompra debe recalcular totales e IVA acumulado con múltiples detalles")
    void testFacturaCompraRecalcularTotales() {
        FacturaCompra factura = FacturaCompra.builder()
                .timbrado("12345678")
                .build();
        factura.setNumeroFactura("001-001-0001234");
        factura.setFechaEmision(LocalDate.now());

        FacturaCompraDetalle d1 = FacturaCompraDetalle.builder().build();
        d1.setCantidad(1);
        d1.setPrecioUnitario(new BigDecimal("110000.00"));
        d1.setPorcentajeIva(new BigDecimal("10.00"));

        FacturaCompraDetalle d2 = FacturaCompraDetalle.builder().build();
        d2.setCantidad(1);
        d2.setPrecioUnitario(new BigDecimal("21000.00"));
        d2.setPorcentajeIva(new BigDecimal("5.00"));

        factura.agregarDetalle(d1);
        factura.agregarDetalle(d2);

        assertEquals(new BigDecimal("131000.00"), factura.getTotalGeneral());
        assertEquals(new BigDecimal("11000.00"), factura.getTotalIva());
        assertSame(factura, d1.getFacturaCompra());
        assertSame(factura, d2.getFacturaCompra());
    }

    @Test
    @DisplayName("Verificar instancias y getters/setters de entidades de seguridad y módulos adicionales")
    void testNuevasEntidades() {
        Rol rol = Rol.builder()
                .nombre("ROLE_ADMIN")
                .build();
        assertEquals("ROLE_ADMIN", rol.getNombre());

        Persona persona = Persona.builder()
                .nombreCompleto("Juan Perez")
                .ci("1234567")
                .build();

        Usuario usuario = Usuario.builder()
                .username("admin")
                .password("bcrypt_hash")
                .rol(rol)
                .persona(persona)
                .activo(true)
                .build();
        assertTrue(usuario.getActivo());
        assertEquals("admin", usuario.getUsername());

        Configuracion config = Configuracion.builder()
                .clave("IVA_DEFECTO")
                .valor("10")
                .descripcion("Porcentaje de IVA por defecto")
                .build();
        assertEquals("10", config.getValor());

        CampaniaPromocion campania = CampaniaPromocion.builder()
                .titulo("Descuento Aniversario")
                .cuerpoHtml("<h1>50% OFF</h1>")
                .enviado(false)
                .build();
        assertFalse(campania.getEnviado());

        LocalDateTime ahora = LocalDateTime.now();
        ContactoMensaje mensaje = ContactoMensaje.builder()
                .nombreRemitente("Cliente")
                .correoRemitente("cliente@mail.com")
                .asunto("Consulta")
                .mensaje("Mensaje")
                .atendido(false)
                .build();
        mensaje.setFechaEnvio(ahora);
        assertEquals(ahora, mensaje.getFechaEnvio());
        assertFalse(mensaje.getAtendido());
    }
}
