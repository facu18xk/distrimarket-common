package com.distrimarket.commons.entity;

import com.distrimarket.commons.enums.RolUsuario;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Rol extends BaseEntity {

    // Los roles válidos son los del enum; la tabla se carga con ellos en la migración 005.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 50)
    private RolUsuario nombre;
}
