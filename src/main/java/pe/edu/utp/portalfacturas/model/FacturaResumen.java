package pe.edu.utp.portalfacturas.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FacturaResumen(Long facturaId, String proveedor, String numeroOc,
        String serie, String numero, LocalDate fechaEmision, BigDecimal total,
        String estadoFactura, String moneda) { }
