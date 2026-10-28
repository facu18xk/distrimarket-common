package com.distrimarket.commons.service;

import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CacheEnLoteTest {

    record Dato(Long id, String nombre) {
    }

    /** Simula la base: registra cada consulta con los ids pedidos. */
    static class Base implements Function<Collection<Long>, List<Dato>> {
        final List<List<Long>> consultas = new ArrayList<>();

        @Override
        public List<Dato> apply(Collection<Long> ids) {
            consultas.add(List.copyOf(ids));
            return ids.stream().map(id -> new Dato(id, "base " + id)).toList();
        }
    }

    @Test
    void soloLosFaltantesVanALaBaseEnUnaConsultaYSeGuardan() {
        Cache cache = new ConcurrentMapCache("producto");
        cache.put(2L, new Dato(2L, "cache 2"));
        Base base = new Base();

        List<Dato> resultado = CacheEnLote.leer(List.of(3L, 2L, 1L), cache, base, Dato::id);

        assertThat(resultado).extracting(Dato::id).containsExactly(3L, 2L, 1L);
        assertThat(resultado.get(1).nombre()).isEqualTo("cache 2");
        assertThat(base.consultas).containsExactly(List.of(3L, 1L));
        assertThat(cache.get(1L, Dato.class)).isNotNull();
        assertThat(cache.get(3L, Dato.class)).isNotNull();
    }

    @Test
    void segundaLecturaNoConsultaLaBase() {
        Cache cache = new ConcurrentMapCache("producto");
        Base base = new Base();

        CacheEnLote.leer(List.of(1L, 2L), cache, base, Dato::id);
        List<Dato> segunda = CacheEnLote.leer(List.of(1L, 2L), cache, base, Dato::id);

        assertThat(segunda).hasSize(2);
        assertThat(base.consultas).hasSize(1);
    }

    @Test
    void siLaCacheFallaSeLeeTodoDeLaBase() {
        Cache rota = mock(Cache.class);
        when(rota.getName()).thenReturn("producto");
        when(rota.get(any(), any(Class.class))).thenThrow(new IllegalStateException("Redis caído"));
        Base base = new Base();

        List<Dato> resultado = CacheEnLote.leer(List.of(5L, 6L), rota, base, Dato::id);

        assertThat(resultado).extracting(Dato::id).containsExactly(5L, 6L);
        assertThat(base.consultas).hasSize(1);
    }

    @Test
    void omiteIdsQueYaNoExistenYConservaElTotal() {
        Cache cache = new ConcurrentMapCache("producto");
        Function<Collection<Long>, List<Dato>> base = ids -> List.of(new Dato(7L, "x"));
        Page<Long> ids = new PageImpl<>(List.of(7L, 8L), PageRequest.of(1, 2), 40);

        Page<Dato> pagina = CacheEnLote.pagina(ids, cache, base, Dato::id);

        assertThat(pagina.getContent()).extracting(Dato::id).containsExactly(7L);
        assertThat(pagina.getTotalElements()).isEqualTo(40);
        assertThat(pagina.getNumber()).isEqualTo(1);
    }

    @Test
    void sinCacheFuncionaIgual() {
        List<Dato> resultado = CacheEnLote.leer(List.of(1L), null, new Base(), Dato::id);

        assertThat(resultado).hasSize(1);
    }
}
