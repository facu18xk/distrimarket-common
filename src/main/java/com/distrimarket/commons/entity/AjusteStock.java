package com.distrimarket.commons.entity;

import com.distrimarket.commons.enums.TipoAjuste;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ajuste_stock")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class AjusteStock extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_deposito", nullable = false)
    private Deposito deposito;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_empleado", nullable = false)
    private Empleado empleado;

    @Column(name = "motivo", nullable = false, length = 200)
    private String motivo;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_ajuste", nullable = false, length = 20)
    private TipoAjuste tipoAjuste;

    @Builder.Default
    @OneToMany(mappedBy = "ajusteStock", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<AjusteStockDetalle> detalles = new ArrayList<>();

    // Métodos POO para sincronizar la relación bidireccional
    public void addDetalle(AjusteStockDetalle detalle) {
        detalles.add(detalle);
        detalle.setAjusteStock(this);
    }

    public void removeDetalle(AjusteStockDetalle detalle) {
        detalles.remove(detalle);
        detalle.setAjusteStock(null);
    }
}