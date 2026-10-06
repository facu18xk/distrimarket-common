package com.distrimarket.commons.service.impl;

import com.distrimarket.commons.dto.PageResponseDTO;
import com.distrimarket.commons.exception.ResourceNotFoundException;
import com.distrimarket.commons.mapper.GenericMapper;
import com.distrimarket.commons.model.SoftDeletable;
import com.distrimarket.commons.repository.BaseRepository;
import com.distrimarket.commons.service.BaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación abstracta genérica de BaseService.
 * Maneja transaccionalidad, logging SLF4J, excepciones de recursos no encontrados
 * y borrado lógico polimórfico mediante la interfaz SoftDeletable.
 *
 * @param <E>   Tipo de la entidad JPA
 * @param <ID>  Tipo del identificador
 * @param <REQ> Tipo del DTO de entrada (Request)
 * @param <RES> Tipo del DTO de salida (Response)
 */
@Slf4j
@RequiredArgsConstructor
@Transactional
public abstract class BaseServiceImpl<E, ID, REQ, RES> implements BaseService<REQ, RES, ID> {

    protected final BaseRepository<E, ID> repository;
    protected final GenericMapper<E, REQ, RES> mapper;

    @Override
    public RES crear(REQ dto) {
        log.info("Creando entidad con datos de entrada: {}", dto);
        E entity = mapper.toEntity(dto);
        E guardada = repository.save(entity);
        log.info("Entidad creada exitosamente");
        return mapper.toDto(guardada);
    }

    @Override
    public RES actualizar(ID id, REQ dto) {
        log.info("Actualizando entidad con ID: {}", id);
        E existente = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recurso no encontrado con ID: " + id));

        mapper.updateEntity(existente, dto);
        E actualizada = repository.save(existente);
        log.info("Entidad con ID: {} actualizada exitosamente", id);
        return mapper.toDto(actualizada);
    }

    @Override
    @Transactional(readOnly = true)
    public RES obtenerPorId(ID id) {
        log.debug("Buscando entidad con ID: {}", id);
        E entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recurso no encontrado con ID: " + id));
        return mapper.toDto(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<RES> listarPaginado(Pageable pageable) {
        log.debug("Listando entidades paginadas. Página: {}, Tamaño: {}", pageable.getPageNumber(), pageable.getPageSize());
        Page<E> pagina = repository.findAll(pageable);
        return PageResponseDTO.from(pagina.map(mapper::toDto));
    }

    @Override
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public PageResponseDTO<RES> buscarPaginado(Specification<?> spec, Pageable pageable) {
        log.debug("Buscando entidades con especificación paginada. Página: {}, Tamaño: {}", pageable.getPageNumber(), pageable.getPageSize());
        Page<E> pagina = repository.findAll((Specification<E>) spec, pageable);
        return PageResponseDTO.from(pagina.map(mapper::toDto));
    }

    @Override
    public void eliminarLogico(ID id) {
        log.info("Iniciando borrado lógico para entidad con ID: {}", id);
        E entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recurso no encontrado con ID: " + id));

        if (entity instanceof SoftDeletable softDeletable) {
            softDeletable.setActivo(false);
            repository.save(entity);
            log.info("Entidad con ID: {} dada de baja lógicamente (activo=false)", id);
        } else {
            log.warn("La entidad con ID: {} no implementa la interfaz SoftDeletable", id);
            throw new UnsupportedOperationException("La entidad no soporta borrado lógico ya que no implementa SoftDeletable");
        }
    }
}
