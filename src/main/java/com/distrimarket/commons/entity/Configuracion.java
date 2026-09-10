package com.distrimarket.commons.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "configuraciones")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Configuracion extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_config")
    private Long id;

    @Column(nullable = false, unique = true, length = 60)
    private String clave;

    @Column(nullable = false, length = 255)
    private String valor;

    @Column(length = 200)
    private String descripcion;
}
