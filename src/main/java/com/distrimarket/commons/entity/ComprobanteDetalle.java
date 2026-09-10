package com.distrimarket.commons.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;

@MappedSuperclass
@Getter
@Setter
public abstract class ComprobanteDetalle {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_producto", nullable = false)
    private Producto producto;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(name = "precio_unitario", nullable = false)
    private BigDecimal precioUnitario;

    @Column(nullable = false)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "porcentaje_iva", nullable = false)
    private BigDecimal porcentajeIva = new BigDecimal("10.00");

    // Lógica POO compartida para compras y ventas
    public void calcularSubtotal() {
        if (precioUnitario != null && cantidad != null) {
            this.subtotal = precioUnitario.multiply(BigDecimal.valueOf(cantidad));
        }
    }

    public BigDecimal getMontoIva() {
        if (subtotal == null || subtotal.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;

        if (porcentajeIva.compareTo(new BigDecimal("10.00")) == 0) {
            return subtotal.divide(new BigDecimal("11"), 2, RoundingMode.HALF_UP);
        } else if (porcentajeIva.compareTo(new BigDecimal("5.00")) == 0) {
            return subtotal.divide(new BigDecimal("21"), 2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }
}