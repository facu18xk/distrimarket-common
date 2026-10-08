package com.distrimarket.commons.entity;


import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "medios_pago")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class MedioPago extends BaseEntity{

    @Column(nullable = false, unique = true, length = 50)
    private String nombre;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;
}
