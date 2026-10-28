package com.distrimarket.commons.service;

import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Búsqueda por texto libre (el parámetro {@code q} de los listados): cada palabra tiene que
 * aparecer en alguno de los campos indicados, sin importar mayúsculas. Los campos pueden ser de
 * relaciones ("persona.nombreCompleto", "producto.nombre"): se unen con LEFT JOIN.
 */
public final class BusquedaTexto {

    private BusquedaTexto() {
    }

    public static <E> Specification<E> en(String q, String... campos) {
        return (root, query, builder) -> {
            if (q == null || q.isBlank() || campos.length == 0) {
                return null;
            }
            List<Predicate> porPalabra = new ArrayList<>();
            for (String palabra : q.trim().toLowerCase(Locale.ROOT).split("\\s+")) {
                String patron = "%" + palabra + "%";
                List<Predicate> enAlgunCampo = new ArrayList<>();
                for (String campo : campos) {
                    enAlgunCampo.add(builder.like(builder.lower(ruta(root, campo).as(String.class)), patron));
                }
                porPalabra.add(builder.or(enAlgunCampo.toArray(Predicate[]::new)));
            }
            return builder.and(porPalabra.toArray(Predicate[]::new));
        };
    }

    /** "persona.nombreCompleto" → join a persona (reutilizado si ya existe) y su campo. */
    private static Path<?> ruta(From<?, ?> root, String campo) {
        String[] partes = campo.split("\\.");
        From<?, ?> actual = root;
        for (int i = 0; i < partes.length - 1; i++) {
            String relacion = partes[i];
            From<?, ?> desde = actual;
            actual = desde.getJoins().stream()
                    .filter(j -> j.getAttribute().getName().equals(relacion))
                    .map(j -> (From<?, ?>) j)
                    .findFirst()
                    .orElseGet(() -> desde.join(relacion, JoinType.LEFT));
        }
        return actual.get(partes[partes.length - 1]);
    }
}
