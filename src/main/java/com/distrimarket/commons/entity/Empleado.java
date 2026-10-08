package com.distrimarket.commons.entity;

import com.distrimarket.commons.model.SoftDeletable;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;

@Entity
@Table(name = "empleados")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@SQLRestriction("activo = true")
public class Empleado extends BaseEntity implements SoftDeletable {

    @Column(name = "activo", nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @OneToOne(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "id_persona", nullable = false, unique = true)
    private Persona persona;

    @Column(nullable = false, length = 60)
    private String cargo;

    @Column(name = "fecha_ingreso", nullable = false)
    private LocalDate fechaIngreso;

    @Column(name = "estado", nullable = false)
    @Builder.Default
    private Boolean estado = true;
}