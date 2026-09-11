package com.distrimarket.commons.entity;


import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "medios_pago")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedioPago extends BaseEntity{

    @Column(nullable = false, unique = true, length = 50)
    private String nombre;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;
}
