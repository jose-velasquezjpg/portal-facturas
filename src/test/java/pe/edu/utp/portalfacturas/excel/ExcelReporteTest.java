package pe.edu.utp.portalfacturas.excel;

import static org.junit.jupiter.api.Assertions.*;

import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pe.edu.utp.portalfacturas.model.FacturaResumen;
import pe.edu.utp.portalfacturas.service.FacturaReporteService;

class ExcelReporteTest {
    @TempDir Path temporal;
    private final FacturaReporteService reporte = new FacturaReporteService();

    private static FacturaResumen f(long id, String prov, String serie, String total, String moneda) {
        return new FacturaResumen(id, prov, "OC-0001", serie, "0000" + id, LocalDate.of(2026, 10, 20),
            new BigDecimal(total), "Registrada", moneda);
    }

    private Path exportar(List<FacturaResumen> facturas) throws Exception {
        Path archivo = temporal.resolve("reporte.xlsx");
        try (OutputStream out = Files.newOutputStream(archivo)) {
            new ExportadorExcel().exportar(facturas, reporte.resumenPorProveedor(facturas), out);
        }
        return archivo;
    }

    @Test void exportaYLeeDeVuelta() throws Exception {
        List<FacturaResumen> origen = List.of(f(1, "Aceros SAC", "F001", "1180.00", "PEN"), f(2, "Aceros SAC", "F001", "590.50", "USD"));
        List<FacturaResumen> leidas = new LectorExcel().leer(exportar(origen));
        assertEquals(2, leidas.size());
        assertEquals(origen.get(0), leidas.get(0));
        assertEquals(new BigDecimal("590.50"), leidas.get(1).total());
    }

    @Test void incluyeFormulasDeTotalPorMonedaYHojaResumen() throws Exception {
        Path archivo = exportar(List.of(f(1, "Aceros SAC", "F001", "100.00", "PEN"), f(2, "Lima SAC", "F002", "50.00", "PEN")));
        try (var in = Files.newInputStream(archivo); XSSFWorkbook libro = new XSSFWorkbook(in)) {
            assertEquals("SUMIF(G2:G3,\"PEN\",H2:H3)", libro.getSheet(ExportadorExcel.HOJA_FACTURAS).getRow(4).getCell(7).getCellFormula());
            assertNotNull(libro.getSheet(ExportadorExcel.HOJA_RESUMEN));
        }
    }

    @Test void neutralizaFormulasMaliciosasEnTextos() throws Exception {
        List<FacturaResumen> leidas = new LectorExcel().leer(exportar(List.of(f(1, "=HYPERLINK(\"http://x\")", "F001", "10.00", "PEN"))));
        assertTrue(leidas.get(0).proveedor().startsWith("'="));
    }

    @Test void ignoraFilasInvalidasSinDetenerElProceso() throws Exception {
        Path archivo = exportar(List.of(f(1, "Aceros SAC", "F001", "100.00", "PEN"), f(2, "Lima SAC", "F002", "50.00", "PEN")));
        try (var in = Files.newInputStream(archivo); XSSFWorkbook libro = new XSSFWorkbook(in)) {
            libro.getSheet(ExportadorExcel.HOJA_FACTURAS).getRow(1).getCell(6).setCellValue("EUR");
            try (OutputStream out = Files.newOutputStream(archivo)) { libro.write(out); }
        }
        assertEquals(1, new LectorExcel().leer(archivo).size());
    }
}
