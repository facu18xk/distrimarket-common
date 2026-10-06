package com.distrimarket.commons.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.io.Serializable;
import java.util.List;

/**
 * DTO normalizado para respuestas paginadas en todo el ecosistema Distrimarket.
 *
 * @param <T> Tipo de los elementos contenidos en la página
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponseDTO<T> implements Serializable {

    private List<T> contenido;
    private int numeroPagina;
    private int tamanio;
    private long totalElementos;
    private int totalPaginas;
    private boolean primera;
    private boolean ultima;

    /**
     * Construye un PageResponseDTO a partir de un Page de Spring Data.
     *
     * @param page Objeto Page de Spring Data
     * @param <T>  Tipo de los elementos
     * @return Instancia de PageResponseDTO normalizada
     */
    public static <T> PageResponseDTO<T> from(Page<T> page) {
        return PageResponseDTO.<T>builder()
                .contenido(page.getContent())
                .numeroPagina(page.getNumber())
                .tamanio(page.getSize())
                .totalElementos(page.getTotalElements())
                .totalPaginas(page.getTotalPages())
                .primera(page.isFirst())
                .ultima(page.isLast())
                .build();
    }
}
