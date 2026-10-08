package com.distrimarket.commons.model;

/**
 * Contrato para entidades que soportan borrado lógico.
 */
public interface SoftDeletable {

    Boolean getActivo();

    void setActivo(Boolean activo);
}
