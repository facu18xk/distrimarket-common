package com.distrimarket.commons.entity;

import com.distrimarket.commons.model.SoftDeletable;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLRestriction;

import java.util.HashSet;
import java.util.Set;

/**
 * Credenciales de acceso. El usuario pertenece a una persona (que puede ser un empleado)
 * y puede tener varios roles. La contraseña se guarda cifrada (BCrypt), nunca en texto plano.
 */
@Entity
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@SQLRestriction("activo = true")
public class Usuario extends BaseEntity implements SoftDeletable {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_persona", nullable = false, unique = true)
    private Persona persona;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "usuario_roles",
            joinColumns = @JoinColumn(name = "id_usuario"),
            inverseJoinColumns = @JoinColumn(name = "id_rol"))
    @Builder.Default
    private Set<Rol> roles = new HashSet<>();

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;
}
