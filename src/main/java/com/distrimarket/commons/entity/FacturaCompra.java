package com.distrimarket.commons.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "facturas_compras",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_proveedor_factura", columnNames = {"id_proveedor", "numero_factura", "timbrado"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FacturaCompra extends Comprobante {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_proveedor", nullable = false)
    private Proveedor proveedor;

    @Column(nullable = false, length = 20)
    private String timbrado;

    @OneToMany(mappedBy = "facturaCompra", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<FacturaCompraDetalle> detalles = new ArrayList<>();

    // Manejo bidireccional y recálculo automático
    public void agregarDetalle(FacturaCompraDetalle detalle) {
        detalles.add(detalle);
        detalle.setFacturaCompra(this);
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
