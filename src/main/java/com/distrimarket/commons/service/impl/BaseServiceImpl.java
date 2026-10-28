package com.distrimarket.commons.service.impl;

import com.distrimarket.commons.dto.PageResponseDTO;
import com.distrimarket.commons.exception.ResourceNotFoundException;
import com.distrimarket.commons.mapper.GenericMapper;
import com.distrimarket.commons.model.SoftDeletable;
import com.distrimarket.commons.repository.BaseRepository;
import com.distrimarket.commons.service.BaseService;
import com.distrimarket.commons.service.Paginas;
import com.distrimarket.commons.service.CacheEnLote;
import com.distrimarket.commons.service.ConsultaIds;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.core.GenericTypeResolver;
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

    @PersistenceContext
    private EntityManager entityManager;
    private CacheManager cacheManager;

    @Autowired(required = false)
    public void setCacheManager(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    /**
     * Dominio de caché de los objetos de este servicio (por ejemplo "proveedor"); null = sin caché.
     * Con dominio, los listados se arman desde la caché objeto por objeto (ver paginaDe).
     */
    protected String dominioCache() {
        return null;
    }

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

    /** Campos donde busca el parámetro q (pueden ser de relaciones: "persona.nombreCompleto"). */
    protected String[] camposDeBusqueda() {
        return new String[0];
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<RES> buscarTexto(String q, Pageable pageable) {
        String[] campos = camposDeBusqueda();
        if (q == null || q.isBlank() || campos.length == 0) {
            return listarPaginado(pageable);
        }
        return buscarPaginado(com.distrimarket.commons.service.BusquedaTexto.<E>en(q, campos), pageable);
    }

    /** Excluye los registros dados de baja (entidades con borrado lógico). */
    protected Specification<E> soloActivos() {
        return (root, query, builder) -> SoftDeletable.class.isAssignableFrom(root.getJavaType())
                ? builder.isTrue(root.get("activo"))
                : null;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<RES> listarPaginado(Pageable pageable) {
        log.debug("Listando entidades paginadas. Página: {}, Tamaño: {}", pageable.getPageNumber(), pageable.getPageSize());
        // Solo activos (borrado lógico) y con desempate por id para que las páginas no se repitan.
        return paginaDe(soloActivos(), pageable);
    }

    @Override
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public PageResponseDTO<RES> buscarPaginado(Specification<?> spec, Pageable pageable) {
        log.debug("Buscando entidades con especificación paginada. Página: {}, Tamaño: {}", pageable.getPageNumber(), pageable.getPageSize());
        Specification<E> filtro = spec == null ? soloActivos() : soloActivos().and((Specification<E>) spec);
        return paginaDe(filtro, pageable);
    }

    /**
     * Una página del listado. Con dominio de caché: de la base solo los ids (filtro y orden), cada
     * objeto desde Redis por id (GET) y los que faltan de la base en una consulta (SET). Sin dominio:
     * la consulta completa de siempre.
     */
    private PageResponseDTO<RES> paginaDe(Specification<E> filtro, Pageable pageable) {
        String dominio = dominioCache();
        if (dominio == null || cacheManager == null) {
            Page<E> pagina = repository.findAll(filtro, Paginas.conDesempatePorId(pageable));
            return PageResponseDTO.from(pagina.map(mapper::toDto));
        }
        Class<E> tipo = tipoEntidad();
        Page<Long> ids = ConsultaIds.pagina(entityManager, tipo, filtro, pageable);
        return PageResponseDTO.from(CacheEnLote.pagina(ids, cacheManager.getCache(dominio),
                faltan -> repository.findAll(ConsultaIds.<E>idEn(faltan), Pageable.unpaged()).map(mapper::toDto).getContent(),
                dto -> ConsultaIds.idDe(dto, tipo)));
    }

    @SuppressWarnings("unchecked")
    private Class<E> tipoEntidad() {
        Class<?>[] tipos = GenericTypeResolver.resolveTypeArguments(getClass(), BaseServiceImpl.class);
        if (tipos == null || tipos[0] == null) {
            throw new IllegalStateException("No se pudo determinar la entidad de " + getClass().getSimpleName());
        }
        return (Class<E>) tipos[0];
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
