package com.distrimarket.commons.mapper;

/**
 * Contrato genérico para mapeo bidireccional entre Entidades y DTOs.
 *
 * @param <E>   Tipo de la entidad
 * @param <REQ> Tipo del DTO de solicitud (Request DTO)
 * @param <RES> Tipo del DTO de respuesta (Response DTO)
 */
public interface GenericMapper<E, REQ, RES> {

    E toEntity(REQ dto);

    RES toDto(E entity);

    void updateEntity(E entity, REQ dto);
}
