package pe.edu.utp.portalfacturas.model;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public class FacturaForm {
    @NotNull(message = "Seleccione un proveedor")
    private Long proveedorId;
    @NotNull(message = "Seleccione una orden de compra")
    private Long ordenCompraId;
    @NotBlank(message = "La serie es obligatoria")
    @Size(max = 5, message = "La serie acepta hasta 5 caracteres")
    private String serie;
    @NotBlank(message = "El número de factura es obligatorio")
    @Size(max = 20, message = "El número acepta hasta 20 caracteres")
    private String numero;
    @NotNull(message = "Indique la fecha de emisión")
    private LocalDate fechaEmision;
    @NotBlank(message = "Seleccione la moneda")
    @Pattern(regexp = "PEN|USD", message = "La moneda debe ser PEN o USD")
    private String moneda = "PEN";
    @NotNull(message = "Indique el subtotal")
    @DecimalMin(value = "0.00", message = "El subtotal no puede ser negativo")
    private BigDecimal subtotal;
    @NotNull(message = "Indique el IGV")
    @DecimalMin(value = "0.00", message = "El IGV no puede ser negativo")
    private BigDecimal igv;
    @NotNull(message = "Indique el total")
    @DecimalMin(value = "0.01", message = "El total debe ser mayor que cero")
    private BigDecimal total;

    public Long getProveedorId() { return proveedorId; }
    public void setProveedorId(Long proveedorId) { this.proveedorId = proveedorId; }
    public Long getOrdenCompraId() { return ordenCompraId; }
    public void setOrdenCompraId(Long ordenCompraId) { this.ordenCompraId = ordenCompraId; }
    public String getSerie() { return serie; }
    public void setSerie(String serie) { this.serie = serie; }
    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }
    public LocalDate getFechaEmision() { return fechaEmision; }
    public void setFechaEmision(LocalDate fechaEmision) { this.fechaEmision = fechaEmision; }
    public String getMoneda() { return moneda; }
    public void setMoneda(String moneda) { this.moneda = moneda; }
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    public BigDecimal getIgv() { return igv; }
    public void setIgv(BigDecimal igv) { this.igv = igv; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
}
