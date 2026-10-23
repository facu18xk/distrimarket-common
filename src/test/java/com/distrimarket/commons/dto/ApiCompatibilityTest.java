package com.distrimarket.commons.dto;

import com.distrimarket.commons.enums.TipoPersona;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ApiCompatibilityTest {
    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Test
    void comprasRetainsItsNestedIdentifiers() throws Exception {
        PersonaDTO persona = new PersonaDTO();
        persona.setIdPersona(12L);
        persona.setTipoPersona(TipoPersona.JURIDICA);
        persona.setNombreCompleto("Proveedor");
        ProveedorResponseDTO proveedor = new ProveedorResponseDTO();
        proveedor.setIdProveedor(3L);
        proveedor.setPersona(persona);
        DepositoDTO deposito = new DepositoDTO();
        deposito.setIdDeposito(7L);
        deposito.setNombre("Central");
        FacturaCompraResponseDTO factura = new FacturaCompraResponseDTO();
        factura.setProveedor(proveedor);
        factura.setDeposito(deposito);

        var result = json.readTree(json.writeValueAsString(factura));
        assertEquals(12L, result.at("/proveedor/persona/idPersona").asLong());
        assertEquals(7L, result.at("/deposito/idDeposito").asLong());
        assertFalse(result.at("/proveedor/persona").has("id"));
        assertFalse(result.at("/deposito").has("id"));
        var request = json.readValue("""
                {"persona":{"tipoPersona":"JURIDICA","nombreCompleto":"Proveedor"}}
                """, ProveedorRequestDTO.class);
        assertEquals(TipoPersona.JURIDICA, request.getPersona().getTipoPersona());
    }

    @Test
    void ventasAndInventarioRetainCanonicalPersonaIdentifiers() throws Exception {
        PersonaResponseDTO persona = new PersonaResponseDTO();
        persona.setId(12L);
        persona.setTipoPersona(TipoPersona.FISICA);
        persona.setNombreCompleto("Empleado");
        EmpleadoResponseDTO empleado = new EmpleadoResponseDTO();
        empleado.setId(3L);
        empleado.setFechaIngreso(LocalDate.of(2026, 1, 1));
        empleado.setPersona(persona);

        var result = json.readTree(json.writeValueAsString(empleado));
        assertEquals(12L, result.at("/persona/id").asLong());
        assertFalse(result.at("/persona").has("idPersona"));
        assertEquals("2026-01-01", result.at("/fechaIngreso").asText());
    }

    @Test
    void facturaDeVentaEsPlanaYSoloReferenciaOtrosMicroserviciosPorId() throws Exception {
        FacturaVentaResponseDTO factura = new FacturaVentaResponseDTO();
        factura.setIdFacturaVenta(98L);
        factura.setIdCliente(15L);
        factura.setClienteNombre("Ana Pérez");
        factura.setIdEmpleado(3L);
        factura.setIdDeposito(1L);
        FacturaVentaDetalleResponseDTO linea = new FacturaVentaDetalleResponseDTO();
        linea.setIdProducto(101L);
        linea.setCantidad(2);
        factura.setDetalles(List.of(linea));

        var result = json.readTree(json.writeValueAsString(factura));
        assertEquals(3L, result.get("idEmpleado").asLong());
        assertEquals("Ana Pérez", result.get("clienteNombre").asText());
        for (String anidado : List.of("cliente", "empleado", "deposito", "medioPago", "timbrado")) {
            assertFalse(result.has(anidado), "La factura no debe anidar " + anidado);
        }
        assertEquals(101L, result.at("/detalles/0/idProducto").asLong());
        assertFalse(result.at("/detalles/0").has("nombreProducto"));
    }

    @Test
    void existingPaginationFormatsRemainDistinct() throws Exception {
        var page = new PageImpl<>(List.of("item"), PageRequest.of(0, 10), 1);
        var compras = json.readTree(json.writeValueAsString(PageResponseDTO.from(page)));
        assertTrue(compras.has("contenido"));
        assertEquals(10, compras.get("tamanio").asInt());
        assertFalse(compras.has("content"));

        FacturaVentaPageResponseDTO ventas = new FacturaVentaPageResponseDTO();
        ventas.setContent(List.of(new FacturaVentaResumenDTO()));
        ventas.setPageNumber(0);
        ventas.setPageSize(10);
        var result = json.readTree(json.writeValueAsString(ventas));
        assertTrue(result.has("content"));
        assertEquals(10, result.get("pageSize").asInt());
        assertFalse(result.has("contenido"));
    }
}
