package com.distrimarket.commons.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "facturas_compras_detalle")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FacturaCompraDetalle extends ComprobanteDetalle {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_factura_compra", nullable = false)
    private FacturaCompra facturaCompra;
}
