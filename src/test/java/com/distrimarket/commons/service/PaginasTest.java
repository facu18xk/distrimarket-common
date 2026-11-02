package com.distrimarket.commons.service;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaginasTest {

    private static final Set<String> ORDENES_PERMITIDOS = Set.of("id", "username", "fechaCreacion");

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

    @Test
    void usaOrdenPorDefectoCuandoElClienteNoPideOrden() {
        Pageable pedido = PageRequest.of(1, 25);

        Pageable resultado = Paginas.conOrdenPermitido(pedido, ORDENES_PERMITIDOS,
                Sort.by(Sort.Direction.ASC, "id"));

        assertThat(resultado.getPageNumber()).isEqualTo(1);
        assertThat(resultado.getPageSize()).isEqualTo(25);
        assertThat(resultado.getSort().toList()).containsExactly(Sort.Order.asc("id"));
    }

    @Test
    void usaSoloElDesempateCuandoNoHayOrdenNiOrdenPorDefecto() {
        Pageable resultado = Paginas.conOrdenPermitido(PageRequest.of(0, 10), ORDENES_PERMITIDOS, null);

        assertThat(resultado.getSort().toList()).containsExactly(Sort.Order.asc("id"));
    }

    @Test
    void conservaMultiplesOrdenesYAgregaIdAlFinal() {
        Pageable pedido = PageRequest.of(3, 15,
                Sort.by(Sort.Direction.DESC, "fechaCreacion").and(Sort.by(Sort.Direction.ASC, "username")));

        Pageable resultado = Paginas.conOrdenPermitido(pedido, ORDENES_PERMITIDOS,
                Sort.by(Sort.Direction.ASC, "id"));

        assertThat(resultado.getPageNumber()).isEqualTo(3);
        assertThat(resultado.getPageSize()).isEqualTo(15);
        assertThat(resultado.getSort().stream().map(Sort.Order::getProperty).toList())
                .containsExactly("fechaCreacion", "username", "id");
        assertThat(resultado.getSort().getOrderFor("fechaCreacion").getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(resultado.getSort().getOrderFor("username").getDirection()).isEqualTo(Sort.Direction.ASC);
        assertThat(resultado.getSort().getOrderFor("id").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void noDuplicaIdSiElClienteYaLoSolicito() {
        Pageable pedido = PageRequest.of(0, 10,
                Sort.by(Sort.Direction.ASC, "username").and(Sort.by(Sort.Direction.DESC, "id")));

        Pageable resultado = Paginas.conOrdenPermitido(pedido, ORDENES_PERMITIDOS,
                Sort.by(Sort.Direction.ASC, "id"));

        List<String> propiedades = resultado.getSort().stream().map(Sort.Order::getProperty).toList();
        assertThat(propiedades).containsExactly("username", "id");
        assertThat(resultado.getSort().getOrderFor("id").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void rechazaPropiedadesNoPermitidasInclusoEnOrdenesMultiples() {
        Pageable pedido = PageRequest.of(0, 10,
                Sort.by(Sort.Direction.ASC, "username").and(Sort.by(Sort.Direction.ASC, "password")));

        assertThatThrownBy(() -> Paginas.conOrdenPermitido(pedido, ORDENES_PERMITIDOS,
                Sort.by(Sort.Direction.ASC, "id")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("password")
                .hasMessageContaining("Valores permitidos");
    }

    @Test
    void rechazaDireccionesNoPermitidas() {
        Pageable pedido = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "fechaCreacion"));

        assertThatThrownBy(() -> Paginas.conOrdenPermitido(pedido, ORDENES_PERMITIDOS,
                Sort.by(Sort.Direction.ASC, "id"), Set.of(Sort.Direction.ASC)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fechaCreacion")
                .hasMessageContaining("DESC");
    }

    @Test
    void rechazaListaDePropiedadesVacia() {
        assertThatThrownBy(() -> Paginas.conOrdenPermitido(PageRequest.of(0, 10), Set.of(),
                Sort.by(Sort.Direction.ASC, "id")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("al menos una propiedad");
    }
}
