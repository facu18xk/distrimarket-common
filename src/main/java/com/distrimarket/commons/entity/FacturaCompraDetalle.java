package com.distrimarket.commons.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "facturas_compras_detalle")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class FacturaCompraDetalle extends ComprobanteDetalle {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_factura_compra", nullable = false)
    private FacturaCompra facturaCompra;
}
