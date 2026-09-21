package com.distrimarket.commons.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "facturas_ventas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class FacturaVenta extends Comprobante {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cliente", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_empleado", nullable = false)
    private Empleado empleado;

    // Relación al Timbrado propio de la empresa
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_timbrado", nullable = false)
    private Timbrado timbrado;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String estado = "EMITIDA";

    @OneToMany(mappedBy = "facturaVenta", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<FacturaVentaDetalle> detalles = new ArrayList<>();

    // Manejo bidireccional y recálculo automático
    public void agregarDetalle(FacturaVentaDetalle detalle) {
        detalles.add(detalle);
        detalle.setFacturaVenta(this);
        recalcularTotales();
    }

    public void recalcularTotales() {
        this.setTotalGeneral(detalles.stream()
                .map(d -> { d.calcularSubtotal(); return d.getSubtotal(); })
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add));

        this.setTotalIva(detalles.stream()
                .map(ComprobanteDetalle::getMontoIva)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add));
    }
}