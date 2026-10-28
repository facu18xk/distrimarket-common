package com.distrimarket.commons.entity;

import com.distrimarket.commons.model.SoftDeletable;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Promoción sobre uno o más productos. Un job de ventas la envía por correo a los clientes que
 * compraron esos productos en los últimos {@code diasHistorial} días, una sola vez por cliente.
 */
@Entity
@Table(name = "campanias_promocion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class CampaniaPromocion extends BaseEntity implements SoftDeletable {

    @Column(nullable = false, length = 120)
    private String titulo;

    /** Texto del correo (HTML simple). */
    @Column(name = "cuerpo_html", nullable = false, columnDefinition = "TEXT")
    private String cuerpoHtml;

    @Column(name = "porcentaje_descuento", precision = 5, scale = 2)
    private BigDecimal porcentajeDescuento;

    @Column(name = "fecha_inicio")
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDate fechaFin;

    /** Desde cuándo se buscan compras de los productos (en días hacia atrás). */
    @Column(name = "dias_historial", nullable = false)
    @Builder.Default
    private Integer diasHistorial = 180;

    /** Momento programado de envío: el job la toma cuando llega esta fecha. */
    @Column(name = "fecha_envio")
    private LocalDateTime fechaEnvio;

    @Column(nullable = false)
    @Builder.Default
    private Boolean enviado = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    /** Productos en promoción (por id: el producto pertenece a inventario). */
    @ElementCollection
    @CollectionTable(name = "campania_productos", joinColumns = @JoinColumn(name = "id_campania"))
    @Column(name = "id_producto")
    @Builder.Default
    private Set<Long> idsProducto = new HashSet<>();
}
