package com.distrimarket.commons.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Listados desde la caché, objeto por objeto.
 *
 * <p>La consulta paginada trae solo los ids de la página (consulta liviana). Cada id se busca en la
 * caché (GET); los que no están se traen de la base en <b>una sola</b> consulta y se guardan (SET).
 * La página nunca se guarda entera: se arma con los objetos individuales, en el orden de los ids.
 * La segunda vez que se pide la misma página solo hay GET (HIT), sin SET ni consulta de objetos.</p>
 *
 * <p>Si la caché falla (Redis caído), se sigue sin caché: todo sale de la base.</p>
 */
@Slf4j
public final class CacheEnLote {

    private CacheEnLote() {
    }

    /**
     * @param ids              ids de la página, en el orden en que deben mostrarse
     * @param cache            sección (dominio) de la caché del microservicio
     * @param cargarFaltantes  trae de la base, en una consulta, los objetos de esos ids
     * @param idDe             id de cada objeto (clave en la caché)
     */
    public static <D> List<D> leer(List<Long> ids, Cache cache, Function<Collection<Long>, List<D>> cargarFaltantes, Function<D, Long> idDe) {
        Map<Long, D> encontrados = new HashMap<>();
        List<Long> faltan = new ArrayList<>();
        boolean conCache = cache != null;

        for (Long id : ids) {
            D dato = null;
            if (conCache) {
                try {
                    @SuppressWarnings("unchecked")
                    D enCache = (D) cache.get(id, Object.class);
                    dato = enCache;
                } catch (RuntimeException ex) {
                    log.warn("Caché no disponible ({}); se lee de la base: {}", cache.getName(), ex.getMessage());
                    conCache = false;
                }
            }
            if (dato != null) {
                encontrados.put(id, dato);
            } else {
                faltan.add(id);
            }
        }

        if (!faltan.isEmpty()) {
            for (D dato : cargarFaltantes.apply(faltan)) {
                Long id = idDe.apply(dato);
                encontrados.put(id, dato);
                if (conCache) {
                    try {
                        cache.put(id, dato);
                    } catch (RuntimeException ex) {
                        log.warn("No se pudo guardar en caché {}:{}: {}", cache.getName(), id, ex.getMessage());
                        conCache = false;
                    }
                }
            }
        }
        return ids.stream().map(encontrados::get).filter(Objects::nonNull).toList();
    }

    /** Igual que {@link #leer}, pero arma la página con el total de la consulta de ids. */
    public static <D> Page<D> pagina(Page<Long> ids, Cache cache, Function<Collection<Long>, List<D>> cargarFaltantes, Function<D, Long> idDe) {
        return new PageImpl<>(leer(ids.getContent(), cache, cargarFaltantes, idDe), ids.getPageable(), ids.getTotalElements());
    }
}
