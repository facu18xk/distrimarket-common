package com.distrimarket.commons.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;

@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public abstract class ComprobanteDetalle extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_producto", nullable = false)
    private Producto producto;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(name = "precio_unitario", nullable = false)
    private BigDecimal precioUnitario;

    @Column(nullable = false)
    @Builder.Default
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "porcentaje_iva", nullable = false)
    @Builder.Default
    private BigDecimal porcentajeIva = new BigDecimal("10.00");

    // Lógica POO compartida para compras y ventas
    public void calcularSubtotal() {
        if (precioUnitario != null && cantidad != null) {
            this.subtotal = precioUnitario.multiply(BigDecimal.valueOf(cantidad));
        }
    }

    public BigDecimal getMontoIva() {
        if (subtotal == null || subtotal.compareTo(BigDecimal.ZERO) == 0 ||
            porcentajeIva == null || porcentajeIva.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        // Fórmula universal de IVA incluido: subtotal * porcentajeIva / (100 + porcentajeIva)
        // Dinámica y configurable: 10% -> subtotal/11, 5% -> subtotal/21, o cualquier otra tasa
        BigDecimal divisor = BigDecimal.valueOf(100).add(porcentajeIva);
        return subtotal.multiply(porcentajeIva).divide(divisor, 2, RoundingMode.HALF_UP);
    }
}