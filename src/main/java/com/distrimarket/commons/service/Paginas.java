package com.distrimarket.commons.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/** Utilidades de paginación compartidas por los microservicios. */
public final class Paginas {

    private Paginas() {
    }

    /**
     * Agrega "id" como último criterio de orden (desempate). Sin él, filas con el mismo valor de
     * orden (por ejemplo, la misma fechaCreacion) pueden salir en cualquier orden en cada consulta:
     * entre una página y la siguiente algunas se repiten y otras nunca aparecen.
     */
    public static Pageable conDesempatePorId(Pageable pageable) {
        if (pageable == null || pageable.isUnpaged() || pageable.getSort().getOrderFor("id") != null) {
            return pageable;
        }
        Sort.Direction direccion = pageable.getSort().stream().findFirst().map(Sort.Order::getDirection).orElse(Sort.Direction.ASC);
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort().and(Sort.by(direccion, "id")));
    }
}
