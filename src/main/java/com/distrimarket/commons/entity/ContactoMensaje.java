package com.distrimarket.commons.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@Table(name = "contacto_mensajes")
@AttributeOverride(name = "fechaCreacion", column = @Column(name = "fecha_envio", updatable = false))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ContactoMensaje extends BaseEntity {

    @Column(name = "nombre_remitente", nullable = false, length = 100)
    private String nombreRemitente;

    @Column(name = "correo_remitente", nullable = false, length = 100)
    private String correoRemitente;

    @Column(nullable = false, length = 150)
    private String asunto;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String mensaje;

    @Column(nullable = false)
    @Builder.Default
    private Boolean atendido = false;

    // Métodos de acceso semánticos para fecha_envio mapeada en BaseEntity
    public LocalDateTime getFechaEnvio() {
        return getFechaCreacion();
    }

    public void setFechaEnvio(LocalDateTime fechaEnvio) {
        setFechaCreacion(fechaEnvio);
    }
}
