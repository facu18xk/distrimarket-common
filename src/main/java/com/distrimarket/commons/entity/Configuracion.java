package com.distrimarket.commons.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "configuraciones")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Configuracion extends BaseEntity {

    @Column(nullable = false, unique = true, length = 60)
    private String clave;

    @Column(nullable = false, length = 255)
    private String valor;

    @Column(length = 200)
    private String descripcion;
}
