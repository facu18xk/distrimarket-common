package com.distrimarket.commons.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "timbrado")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Timbrado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_timbrado")
    private Long id;

    @Column(name = "numero_timbrado", nullable = false, unique = true, length = 20)
    private String numeroTimbrado;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_vencimiento", nullable = false)
    private LocalDate fechaVencimiento;

    @Column(name = "punto_expedicion", nullable = false, length = 3)
    @Builder.Default
    private String puntoExpedicion = "001";

    @Column(nullable = false, length = 3)
    @Builder.Default
    private String sucursal = "001";

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;
}