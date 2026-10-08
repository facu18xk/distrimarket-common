package com.distrimarket.commons.service;

import com.distrimarket.commons.dto.PageResponseDTO;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

/**
 * Contrato de servicio base genérico con operaciones CRUD, paginación,
 * búsqueda por especificaciones JPA y borrado lógico.
 *
 * @param <REQ> Tipo del DTO de solicitud (Request DTO)
 * @param <RES> Tipo del DTO de respuesta (Response DTO)
 * @param <ID>  Tipo del identificador
 */
public interface BaseService<REQ, RES, ID> {

    RES crear(REQ dto);

    RES actualizar(ID id, REQ dto);

    RES obtenerPorId(ID id);

    PageResponseDTO<RES> listarPaginado(Pageable pageable);

    PageResponseDTO<RES> buscarPaginado(Specification<?> spec, Pageable pageable);

    void eliminarLogico(ID id);
}
