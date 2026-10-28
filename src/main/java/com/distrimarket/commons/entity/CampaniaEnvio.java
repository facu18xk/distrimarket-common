package com.distrimarket.commons.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

/** Correo de una promoción a un cliente: evita enviarle dos veces la misma y deja el error si falló. */
@Entity
@Table(name = "campania_envios",
        uniqueConstraints = @UniqueConstraint(name = "uk_campania_envios_campania_cliente", columnNames = {"id_campania", "id_cliente"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class CampaniaEnvio extends BaseEntity {

    @Column(name = "id_campania", nullable = false)
    private Long idCampania;

    @Column(name = "id_cliente", nullable = false)
    private Long idCliente;

    @Column(nullable = false, length = 100)
    private String correo;

    @Column(nullable = false)
    @Builder.Default
    private Boolean enviado = false;

    @Column(length = 500)
    private String error;
}
