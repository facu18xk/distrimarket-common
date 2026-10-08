package com.distrimarket.commons.entity;

import com.distrimarket.commons.model.SoftDeletable;
import com.distrimarket.commons.enums.TipoPersona;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "personas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@SQLRestriction("activo = true")
public class Persona extends BaseEntity implements SoftDeletable {

    @Column(name = "activo", nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_persona", nullable = false, length = 20)
    private TipoPersona tipoPersona; // Identifica si es Física o Jurídica

    @Column(unique = true, length = 20)
    private String ci; // Solo llenado si es FISICA

    @Column(unique = true, length = 20)
    private String ruc; // Llenado siempre en JURIDICA, opcional en FISICA

    @Column(nullable = false, length = 150)
    private String nombreCompleto; // Razón Social o Nombres + Apellidos

    @Column(length = 30)
    private String telefono;

    @Column(length = 100)
    private String correo;

    @Column(length = 200)
    private String direccion;

    // Método para validación interna
    @PrePersist
    @PreUpdate
    public void normalizarYValidar() {
        if (this.ci != null && this.ci.isBlank()) {
            this.ci = null;
        }
        if (this.ruc != null && this.ruc.isBlank()) {
            this.ruc = null;
        }

        if (tipoPersona == TipoPersona.JURIDICA && (ruc == null || ruc.isBlank())) {
            throw new IllegalArgumentException("Una persona jurídica debe tener RUC.");
        }
        if (tipoPersona == TipoPersona.FISICA && (ci == null || ci.isBlank())) {
            throw new IllegalArgumentException("Una persona física debe tener CI.");
        }
    }
}