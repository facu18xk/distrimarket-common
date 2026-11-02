package com.distrimarket.commons.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Set;

/** Utilidades de paginación compartidas por los microservicios. */
public final class Paginas {

    private Paginas() {
    }

    /**
     * Aplica un orden seguro a un {@link Pageable}: valida que cada propiedad solicitada esté
     * permitida, usa {@code ordenPorDefecto} cuando el cliente no pidió un orden y agrega "id"
     * como último criterio de desempate.
     *
     * <p>Si el cliente pidió varios criterios, se conservan todos y en el mismo orden. Si ya
     * incluyó "id", no se duplica. Las propiedades y direcciones del orden por defecto también
     * se validan.
     *
     * <p>Ejemplo: {@code Paginas.conOrdenPermitido(pageable, Set.of("id", "username",
     * "fechaCreacion"), Sort.by(Sort.Direction.ASC, "id"))}.
     *
     * @param pageable             página y orden pedidos por el cliente; puede ser {@code null}
     * @param propiedadesPermitidas propiedades por las que se puede ordenar; no puede estar vacía
     * @param ordenPorDefecto      orden a usar cuando {@code pageable} no trae uno; puede ser
     *                             {@code null} para usar solo el desempate por "id"
     * @return el pageable con el orden validado y el desempate por "id", o {@code null} si
     *         {@code pageable} era {@code null}
     * @throws IllegalArgumentException si una propiedad o dirección no está permitida
     */
    public static Pageable conOrdenPermitido(Pageable pageable,
                                             Collection<String> propiedadesPermitidas,
                                             Sort ordenPorDefecto) {
        return conOrdenPermitido(pageable, propiedadesPermitidas, ordenPorDefecto,
                EnumSet.allOf(Sort.Direction.class));
    }

    /**
     * Variante que además limita las direcciones aceptadas. Es útil para endpoints en los que
     * solo tiene sentido un orden ascendente o descendente.
     *
     * @see #conOrdenPermitido(Pageable, Collection, Sort)
     */
    public static Pageable conOrdenPermitido(Pageable pageable,
                                             Collection<String> propiedadesPermitidas,
                                             Sort ordenPorDefecto,
                                             Collection<Sort.Direction> direccionesPermitidas) {
        if (pageable == null) {
            return null;
        }
        Set<String> propiedades = validarPropiedadesPermitidas(propiedadesPermitidas);
        Set<Sort.Direction> direcciones = validarDireccionesPermitidas(direccionesPermitidas);

        Sort solicitado = pageable.getSort();
        validarOrden(solicitado, propiedades, direcciones);

        Sort orden = solicitado.isSorted()
                ? solicitado
                : (ordenPorDefecto == null ? Sort.unsorted() : ordenPorDefecto);
        validarOrden(orden, propiedades, direcciones);

        Pageable paginado = pageable.isUnpaged()
                ? Pageable.unpaged(orden)
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), orden);
        return conDesempatePorId(paginado);
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

    private static Set<String> validarPropiedadesPermitidas(Collection<String> propiedadesPermitidas) {
        if (propiedadesPermitidas == null || propiedadesPermitidas.isEmpty()) {
            throw new IllegalArgumentException("Debe indicar al menos una propiedad de orden permitida.");
        }
        Set<String> propiedades = new LinkedHashSet<>();
        for (String propiedad : propiedadesPermitidas) {
            if (propiedad == null || propiedad.isBlank()) {
                throw new IllegalArgumentException("Las propiedades de orden permitidas no pueden estar vacías.");
            }
            propiedades.add(propiedad);
        }
        return propiedades;
    }

    private static Set<Sort.Direction> validarDireccionesPermitidas(Collection<Sort.Direction> direccionesPermitidas) {
        if (direccionesPermitidas == null || direccionesPermitidas.isEmpty()) {
            throw new IllegalArgumentException("Debe indicar al menos una dirección de orden permitida.");
        }
        Set<Sort.Direction> direcciones = EnumSet.noneOf(Sort.Direction.class);
        for (Sort.Direction direccion : direccionesPermitidas) {
            if (direccion == null) {
                throw new IllegalArgumentException("Las direcciones de orden permitidas no pueden estar vacías.");
            }
            direcciones.add(direccion);
        }
        return direcciones;
    }

    private static void validarOrden(Sort orden, Set<String> propiedadesPermitidas,
                                     Set<Sort.Direction> direccionesPermitidas) {
        for (Sort.Order criterio : orden) {
            if (!propiedadesPermitidas.contains(criterio.getProperty())) {
                throw new IllegalArgumentException("No se puede ordenar por '" + criterio.getProperty()
                        + "'. Valores permitidos: " + propiedadesPermitidas + ".");
            }
            if (criterio.getDirection() == null || !direccionesPermitidas.contains(criterio.getDirection())) {
                throw new IllegalArgumentException("No se puede ordenar por '" + criterio.getProperty()
                        + "' en dirección " + criterio.getDirection() + ". Direcciones permitidas: "
                        + direccionesPermitidas + ".");
            }
        }
    }
}
