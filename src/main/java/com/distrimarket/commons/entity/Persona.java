package com.distrimarket.commons.entity;

import com.distrimarket.commons.enums.TipoPersona;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "personas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Persona extends BaseEntity {

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
    public void validarDocumentos() {
        if (tipoPersona == TipoPersona.JURIDICA && (ruc == null || ruc.isEmpty())) {
            throw new IllegalArgumentException("Una persona jurídica debe tener RUC.");
        }
        if (tipoPersona == TipoPersona.FISICA && (ci == null || ci.isEmpty())) {
            throw new IllegalArgumentException("Una persona física debe tener CI.");
        }
    }
}