package com.distrimarket.commons.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public abstract class Comprobante extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_deposito", nullable = false)
    private Deposito deposito;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_medio_pago", nullable = false)
    private MedioPago medioPago;

    @Column(name = "numero_factura", nullable = false, length = 50)
    private String numeroFactura;

    @Column(name = "fecha_emision", nullable = false)
    private LocalDate fechaEmision;

    @Column(name = "total_iva", nullable = false)
    private BigDecimal totalIva = BigDecimal.ZERO;

    @Column(name = "total_general", nullable = false)
    private BigDecimal totalGeneral = BigDecimal.ZERO;

    // Aquí evitamos poner @PrePersist de fecha_emision porque
    // en Compras, la fecha de emisión la dicta el papel del proveedor, no la fecha actual del sistema.
    // La BaseEntity ya guarda la fecha en que se registró en el sistema.
}