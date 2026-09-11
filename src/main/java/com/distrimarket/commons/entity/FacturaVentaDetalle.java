package com.distrimarket.commons.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "facturas_ventas_detalle")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FacturaVentaDetalle extends ComprobanteDetalle {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_factura_venta", nullable = false)
    private FacturaVenta facturaVenta;
}