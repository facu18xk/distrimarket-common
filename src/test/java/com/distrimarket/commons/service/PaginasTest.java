package com.distrimarket.commons.service;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;

class PaginasTest {

    @Test
    void agregaIdComoDesempateConLaMismaDireccion() {
        Pageable pedido = PageRequest.of(2, 10, Sort.by(Sort.Direction.DESC, "fechaCreacion"));

        Pageable resultado = Paginas.conDesempatePorId(pedido);

        assertThat(resultado.getPageNumber()).isEqualTo(2);
        assertThat(resultado.getPageSize()).isEqualTo(10);
        assertThat(resultado.getSort().getOrderFor("fechaCreacion").getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(resultado.getSort().getOrderFor("id").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void sinOrdenUsaIdAscendente() {
        Pageable resultado = Paginas.conDesempatePorId(PageRequest.of(0, 5));

        assertThat(resultado.getSort().getOrderFor("id").getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void noTocaSiYaOrdenaPorIdOSiNoHayPaginas() {
        Pageable porId = PageRequest.of(0, 5, Sort.by("id"));

        assertThat(Paginas.conDesempatePorId(porId)).isSameAs(porId);
        assertThat(Paginas.conDesempatePorId(Pageable.unpaged())).isSameAs(Pageable.unpaged());
    }
}
