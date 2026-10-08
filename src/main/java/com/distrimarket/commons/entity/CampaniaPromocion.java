package com.distrimarket.commons.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@Table(name = "campanias_promocion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class CampaniaPromocion extends BaseEntity {

    @Column(nullable = false, length = 120)
    private String titulo;

    @Column(name = "cuerpo_html", nullable = false, columnDefinition = "TEXT")
    private String cuerpoHtml;

    @Column(name = "fecha_envio")
    private LocalDateTime fechaEnvio;

    @Column(nullable = false)
    @Builder.Default
    private Boolean enviado = false;
}
