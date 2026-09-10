package com.distrimarket.commons.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "transferencias_stock")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferenciaStock extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_transferencia")
    private Long idTransferencia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_deposito_origen", nullable = false)
    private Deposito depositoOrigen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_deposito_destino", nullable = false)
    private Deposito depositoDestino;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_empleado", nullable = false)
    private Empleado empleado;

    @Column(name = "observacion", length = 255)
    private String observacion;

    @Column(name = "estado", length = 20)
    @Builder.Default
    private String estado = "COMPLETADO";

    @Builder.Default
    @OneToMany(mappedBy = "transferenciaStock", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<TransferenciaStockDetalle> detalles = new ArrayList<>();

    public void addDetalle(TransferenciaStockDetalle detalle) {
        detalles.add(detalle);
        detalle.setTransferenciaStock(this);
    }

    public void removeDetalle(TransferenciaStockDetalle detalle) {
        detalles.remove(detalle);
        detalle.setTransferenciaStock(null);
    }
}