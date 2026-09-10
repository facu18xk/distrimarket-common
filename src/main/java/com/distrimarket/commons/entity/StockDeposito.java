package com.distrimarket.commons.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "stock_deposito",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_deposito_producto", columnNames = {"id_deposito", "id_producto"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockDeposito extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_stock")
    private Long idStock;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_deposito", nullable = false)
    private Deposito deposito;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_producto", nullable = false)
    private Producto producto;

    @Column(name = "cantidad", nullable = false)
    @Builder.Default
    private Integer cantidad = 0;
}