package com.distrimarket.commons.entity;

import com.distrimarket.commons.model.SoftDeletable;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "marcas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@SQLRestriction("activo = true")
public class Marca extends BaseEntity implements SoftDeletable {

    @Column(name = "activo", nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @Column(name = "nombre", nullable = false, unique = true, length = 80)
    private String nombre;
}