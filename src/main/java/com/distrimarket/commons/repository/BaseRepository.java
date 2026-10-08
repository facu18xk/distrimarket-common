package com.distrimarket.commons.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;

/**
 * Repositorio base genérico para el ecosistema Distrimarket.
 * Extiende JpaRepository y JpaSpecificationExecutor para filtrado dinámico.
 *
 * @param <E>  Tipo de la entidad
 * @param <ID> Tipo del identificador de la entidad
 */
@NoRepositoryBean
public interface BaseRepository<E, ID> extends JpaRepository<E, ID>, JpaSpecificationExecutor<E> {
}
