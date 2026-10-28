package com.distrimarket.commons.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.query.QueryUtils;
import org.springframework.data.support.PageableExecutionUtils;

import java.util.Collection;
import java.util.List;

/**
 * Solo los ids de una página (con el mismo filtro, orden y total que la consulta completa).
 * Es el primer paso de los listados desde la caché: los objetos se buscan después en Redis por id
 * (ver {@link CacheEnLote}) y solo los que faltan se traen de la base.
 */
public final class ConsultaIds {

    private ConsultaIds() {
    }

    public static <E> Page<Long> pagina(EntityManager entityManager, Class<E> tipo, Specification<E> filtro, Pageable pageable) {
        Pageable orden = Paginas.conDesempatePorId(pageable);
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();

        CriteriaQuery<Long> consulta = builder.createQuery(Long.class);
        Root<E> root = consulta.from(tipo);
        Predicate condicion = filtro == null ? null : filtro.toPredicate(root, consulta, builder);
        consulta.select(root.get("id"));
        if (condicion != null) {
            consulta.where(condicion);
        }
        consulta.orderBy(QueryUtils.toOrders(orden.getSort(), root, builder));
        TypedQuery<Long> ids = entityManager.createQuery(consulta);
        if (orden.isPaged()) {
            ids.setFirstResult((int) orden.getOffset());
            ids.setMaxResults(orden.getPageSize());
        }
        List<Long> contenido = ids.getResultList();

        return PageableExecutionUtils.getPage(contenido, orden, () -> {
            CriteriaQuery<Long> conteo = builder.createQuery(Long.class);
            Root<E> raiz = conteo.from(tipo);
            Predicate condicionConteo = filtro == null ? null : filtro.toPredicate(raiz, conteo, builder);
            conteo.select(builder.count(raiz));
            if (condicionConteo != null) {
                conteo.where(condicionConteo);
            }
            return entityManager.createQuery(conteo).getSingleResult();
        });
    }

    /** Filtro "id en (…)": para traer de la base, en una consulta, los objetos que faltaban en caché. */
    public static <E> Specification<E> idEn(Collection<Long> ids) {
        return (root, query, builder) -> root.get("id").in(ids);
    }

    /**
     * Id de un DTO para usarlo como clave de caché: getId() o, si no existe, getId + nombre de la
     * entidad (por ejemplo getIdProveedor en ProveedorResponseDTO).
     */
    public static Long idDe(Object dto, Class<?> entidad) {
        for (String metodo : new String[]{"getId", "getId" + entidad.getSimpleName()}) {
            try {
                Object valor = dto.getClass().getMethod(metodo).invoke(dto);
                if (valor instanceof Number numero) {
                    return numero.longValue();
                }
            } catch (ReflectiveOperationException ignorado) {
                // Se prueba el siguiente nombre.
            }
        }
        throw new IllegalStateException("No se encontró el id de " + dto.getClass().getSimpleName());
    }
}
