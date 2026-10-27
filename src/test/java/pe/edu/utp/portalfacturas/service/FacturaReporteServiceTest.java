package pe.edu.utp.portalfacturas.service;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import pe.edu.utp.portalfacturas.model.FacturaResumen;

class FacturaReporteServiceTest {
    private final FacturaReporteService service = new FacturaReporteService();

    private static FacturaResumen f(long id, String prov, String total, String moneda) {
        return new FacturaResumen(id, prov, "OC-1", "F001", String.valueOf(id), LocalDate.of(2026, 10, 20),
            new BigDecimal(total), "Registrada", moneda);
    }
    private final List<FacturaResumen> datos = List.of(f(1, "Aceros SAC", "100.00", "PEN"), f(2, "Lima SAC", "40.00", "PEN"),
        f(3, "Aceros SAC", "60.00", "PEN"), f(4, "Aceros SAC", "25.00", "USD"));

    @Test void agrupaPorProveedor() {
        var grupos = service.agruparPorProveedor(datos);
        assertEquals(2, grupos.keySet().size());
        assertEquals(3, grupos.get("Aceros SAC").size());
    }
    @Test void divideEnLotes() {
        var lotes = service.dividirEnLotes(datos, 3);
        assertEquals(2, lotes.size());
        assertEquals(1, lotes.get(1).size());
    }
    @Test void rechazaLoteInvalido() { assertThrows(IllegalArgumentException.class, () -> service.dividirEnLotes(datos, 0)); }
    @Test void totalesNoMezclanMonedas() {
        var totales = service.totalPorMoneda(datos);
        assertEquals(new BigDecimal("200.00"), totales.get("PEN"));
        assertEquals(new BigDecimal("25.00"), totales.get("USD"));
    }
    @Test void resumenPorProveedorYMoneda() {
        var resumen = service.resumenPorProveedor(datos);
        assertEquals(3, resumen.size());
        assertEquals(new FacturaReporteService.ResumenProveedor("Aceros SAC", "PEN", 2, new BigDecimal("160.00")), resumen.get(0));
    }
    @Test void generaArchivoXlsxValido() throws Exception {
        byte[] bytes = service.generarExcel(datos);
        assertTrue(bytes.length > 0);
        assertEquals('P', bytes[0]); assertEquals('K', bytes[1]); // un .xlsx es un ZIP
    }
}
