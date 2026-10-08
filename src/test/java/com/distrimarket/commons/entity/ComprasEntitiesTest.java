package com.distrimarket.commons.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class ComprasEntitiesTest {

    @Test
    @DisplayName("ProveedorProducto debe inicializarse con @SuperBuilder y respetar valores por defecto")
    void testProveedorProductoBuilder() {
        Proveedor proveedor = Proveedor.builder()
                .id(10L)
                .nombreFantasia("Distribuidora Central")
                .build();

        Producto producto = Producto.builder()
                .id(101L)
                .nombre("Arroz Premium 1kg")
                .build();

        ProveedorProducto acuerdo = ProveedorProducto.builder()
                .id(1L)
                .proveedor(proveedor)
                .producto(producto)
                .precioCostoAcordado(new BigDecimal("4500.00"))
                .codigoProveedor("PROV-ARR-01")
                .build();

        assertEquals(1L, acuerdo.getId());
        assertEquals("Distribuidora Central", acuerdo.getProveedor().getNombreFantasia());
        assertEquals("Arroz Premium 1kg", acuerdo.getProducto().getNombre());
        assertEquals(new BigDecimal("4500.00"), acuerdo.getPrecioCostoAcordado());
        assertEquals("PROV-ARR-01", acuerdo.getCodigoProveedor());
        assertTrue(acuerdo.getActivo(), "El campo activo debe ser true por defecto");
    }

    @Test
    @DisplayName("OrdenCompra debe asociar detalles bidireccionalmente y calcular subtotales y total general")
    void testOrdenCompraTotalesYDetalles() {
        Proveedor proveedor = Proveedor.builder().id(5L).nombreFantasia("Proveedor X").build();
        Empleado empleado = Empleado.builder().id(3L).cargo("Comprador").build();

        OrdenCompra orden = OrdenCompra.builder()
                .id(50L)
                .proveedor(proveedor)
                .empleado(empleado)
                .observacion("Pedido de reposición")
                .build();

        assertEquals("PENDIENTE", orden.getEstado(), "Estado inicial debe ser PENDIENTE");
        assertEquals(BigDecimal.ZERO, orden.getTotal());

        Producto p1 = Producto.builder().id(201L).nombre("Harina 1kg").build();
        OrdenCompraDetalle d1 = OrdenCompraDetalle.builder()
                .producto(p1)
                .cantidad(20)
                .precioUnitario(new BigDecimal("3000.00"))
                .build();

        Producto p2 = Producto.builder().id(202L).nombre("Azúcar 1kg").build();
        OrdenCompraDetalle d2 = OrdenCompraDetalle.builder()
                .producto(p2)
                .cantidad(10)
                .precioUnitario(new BigDecimal("4500.00"))
                .build();

        orden.agregarDetalle(d1);
        orden.agregarDetalle(d2);

        // 20 * 3000 = 60000
        assertEquals(new BigDecimal("60000.00"), d1.getSubtotal());
        // 10 * 4500 = 45000
        assertEquals(new BigDecimal("45000.00"), d2.getSubtotal());
        // Total = 60000 + 45000 = 105000
        assertEquals(new BigDecimal("105000.00"), orden.getTotal());

        assertSame(orden, d1.getOrdenCompra());
        assertSame(orden, d2.getOrdenCompra());
        assertEquals(2, orden.getDetalles().size());
    }

    @Test
    @DisplayName("FacturaCompra debe soportar vínculo a OrdenCompra, estado, condición de compra y herencia de Comprobante")
    void testFacturaCompraConOrdenYAtributos() {
        Deposito deposito = Deposito.builder().id(1L).nombre("Depósito Central").build();
        MedioPago medioPago = MedioPago.builder().id(2L).nombre("Transferencia Bancaria").build();
        Proveedor proveedor = Proveedor.builder().id(5L).nombreFantasia("Proveedor Lácteo").build();

        OrdenCompra orden = OrdenCompra.builder()
                .id(20L)
                .proveedor(proveedor)
                .estado("APROBADA")
                .build();

        FacturaCompra factura = FacturaCompra.builder()
                .id(100L)
                .numeroFactura("001-002-0004567")
                .timbrado("16789012")
                .fechaEmision(LocalDate.of(2026, 9, 30))
                .deposito(deposito)
                .medioPago(medioPago)
                .proveedor(proveedor)
                .ordenCompra(orden)
                .condicionCompra("CREDITO")
                .build();

        // Atributos de Comprobante heredados
        assertEquals("001-002-0004567", factura.getNumeroFactura());
        assertEquals(deposito, factura.getDeposito());
        assertEquals(medioPago, factura.getMedioPago());
        assertEquals(LocalDate.of(2026, 9, 30), factura.getFechaEmision());

        // Atributos específicos de FacturaCompra
        assertEquals("16789012", factura.getTimbrado());
        assertEquals(proveedor, factura.getProveedor());
        assertEquals(orden, factura.getOrdenCompra());
        assertEquals("ACTIVA", factura.getEstado(), "Estado por defecto debe ser ACTIVA");
        assertEquals("CREDITO", factura.getCondicionCompra());

        // Recálculo con detalles
        FacturaCompraDetalle det = FacturaCompraDetalle.builder()
                .producto(Producto.builder().id(11L).nombre("Queso Paraguay 1kg").build())
                .cantidad(5)
                .precioUnitario(new BigDecimal("22000.00"))
                .porcentajeIva(new BigDecimal("10.00"))
                .build();

        factura.agregarDetalle(det);

        // Subtotal = 5 * 22000 = 110000.00
        assertEquals(new BigDecimal("110000.00"), factura.getTotalGeneral());
        // IVA 10% incluido = 110000 / 11 = 10000.00
        assertEquals(new BigDecimal("10000.00"), factura.getTotalIva());
        assertSame(factura, det.getFacturaCompra());
    }
}
