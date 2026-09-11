package com.distrimarket.commons.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "marcas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Marca extends BaseEntity {

    @Column(name = "nombre", nullable = false, unique = true, length = 80)
    private String nombre;
}