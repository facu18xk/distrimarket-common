package com.distrimarket.commons.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "marcas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Marca extends BaseEntity {

    @Column(name = "nombre", nullable = false, unique = true, length = 80)
    private String nombre;
}