package com.distrimarket.commons.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConsultaIdsTest {

    static class Proveedor {
    }

    public static class ConGetId {
        public Long getId() {
            return 4L;
        }
    }

    public static class ConGetIdEntidad {
        public Integer getIdProveedor() {
            return 9;
        }
    }

    @Test
    void leeGetIdOGetIdMasNombreDeEntidad() {
        assertThat(ConsultaIds.idDe(new ConGetId(), Proveedor.class)).isEqualTo(4L);
        assertThat(ConsultaIds.idDe(new ConGetIdEntidad(), Proveedor.class)).isEqualTo(9L);
    }

    @Test
    void sinIdFalla() {
        assertThatThrownBy(() -> ConsultaIds.idDe(new Object(), Proveedor.class))
                .isInstanceOf(IllegalStateException.class);
    }
}
