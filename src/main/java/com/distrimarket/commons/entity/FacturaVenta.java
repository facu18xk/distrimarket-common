package com.distrimarket.commons.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "facturas_ventas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "detalles")
public class FacturaVenta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_factura_venta")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cliente", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_empleado", nullable = false)
    private Empleado empleado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_deposito", nullable = false)
    private Deposito deposito;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_timbrado", nullable = false)
    private Timbrado timbrado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_medio_pago", nullable = false)
    private MedioPago medioPago;

    @Column(name = "numero_factura", nullable = false, unique = true, length = 30)
    private String numeroFactura;

    @Column(name = "fecha_emision")
    private LocalDateTime fechaEmision;

    @Column(name = "total_iva", nullable = false)
    @Builder.Default
    private BigDecimal totalIva = BigDecimal.ZERO;

    @Column(name = "total_general", nullable = false)
    @Builder.Default
    private BigDecimal totalGeneral = BigDecimal.ZERO;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String estado = "EMITIDA";

    @OneToMany(mappedBy = "facturaVenta", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<FacturaVentaDetalle> detalles = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (this.fechaEmision == null) {
            this.fechaEmision = LocalDateTime.now();
        }
    }

    public void agregarDetalle(FacturaVentaDetalle detalle) {
        detalles.add(detalle);
        detalle.setFacturaVenta(this);
    }
}