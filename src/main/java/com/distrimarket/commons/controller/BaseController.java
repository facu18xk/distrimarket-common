package com.distrimarket.commons.controller;

import com.distrimarket.commons.dto.PageResponseDTO;
import com.distrimarket.commons.service.BaseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador base genérico para el ecosistema Distrimarket.
 * Expone endpoints REST universales: POST (201), PUT (200), GET /{id} (200),
 * GET paginado (200) y DELETE /{id} (204).
 *
 * @param <REQ> Tipo del DTO de solicitud (Request DTO)
 * @param <RES> Tipo del DTO de respuesta (Response DTO)
 * @param <ID>  Tipo del identificador
 */
@RequiredArgsConstructor
public abstract class BaseController<REQ, RES, ID> {

    protected final BaseService<REQ, RES, ID> service;

    @PostMapping
    public ResponseEntity<RES> crear(@Valid @RequestBody REQ dto) {
        RES creado = service.crear(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @PutMapping("/{id}")
    public ResponseEntity<RES> actualizar(@PathVariable("id") ID id, @Valid @RequestBody REQ dto) {
        RES actualizado = service.actualizar(id, dto);
        return ResponseEntity.ok(actualizado);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RES> obtenerPorId(@PathVariable("id") ID id) {
        RES res = service.obtenerPorId(id);
        return ResponseEntity.ok(res);
    }

    @GetMapping
    public ResponseEntity<PageResponseDTO<RES>> listarPaginado(Pageable pageable) {
        PageResponseDTO<RES> pagina = service.listarPaginado(pageable);
        return ResponseEntity.ok(pagina);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable("id") ID id) {
        service.eliminarLogico(id);
        return ResponseEntity.noContent().build();
    }
}
