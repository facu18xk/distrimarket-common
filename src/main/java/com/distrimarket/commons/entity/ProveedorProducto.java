package com.distrimarket.commons.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Table(
        name = "proveedores_productos",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_proveedor_producto", columnNames = {"id_proveedor", "id_producto"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ProveedorProducto extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_proveedor", nullable = false)
    private Proveedor proveedor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_producto", nullable = false)
    private Producto producto;

    @Column(name = "precio_costo_acordado", precision = 14, scale = 2, nullable = false)
    private BigDecimal precioCostoAcordado;

    @Column(name = "codigo_proveedor", length = 60)
    private String codigoProveedor;

    @Column(name = "activo", nullable = false)
    @Builder.Default
    private Boolean activo = true;
}
