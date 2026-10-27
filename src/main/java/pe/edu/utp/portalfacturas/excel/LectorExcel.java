package pe.edu.utp.portalfacturas.excel;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.Validate;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pe.edu.utp.portalfacturas.model.FacturaResumen;
import pe.edu.utp.portalfacturas.util.UtilSeguridad;

/** Lee de vuelta un reporte de facturas. Un .xlsx recibido es NO confiable: se limitan tamaño y filas. */
public class LectorExcel {
    private static final Logger log = LoggerFactory.getLogger(LectorExcel.class);
    private static final long MAX_BYTES = 5L * 1024 * 1024; // 5 MB (zip bomb, CWE-409)
    private static final int MAX_FILAS = 10_000;

    public List<FacturaResumen> leer(Path archivo) throws IOException {
        long bytes = Files.size(archivo); // Control 1: tamaño ANTES de abrir
        if (bytes > MAX_BYTES) {
            throw new IllegalArgumentException("Archivo demasiado grande: " + FileUtils.byteCountToDisplaySize(bytes));
        }
        List<FacturaResumen> facturas = new ArrayList<>();
        try (InputStream entrada = Files.newInputStream(archivo);
             XSSFWorkbook libro = new XSSFWorkbook(entrada)) {
            Sheet hoja = libro.getSheet(ExportadorExcel.HOJA_FACTURAS);
            if (hoja == null) hoja = libro.getSheetAt(0);
            int ultima = Math.min(hoja.getLastRowNum(), MAX_FILAS); // Control 2: límite de filas
            for (int i = 1; i <= ultima; i++) {
                Row fila = hoja.getRow(i);
                if (fila == null || fila.getCell(0) == null) continue; // vacía o fila de TOTAL
                try {
                    facturas.add(convertir(fila));
                } catch (RuntimeException e) { // Control 3: una fila mala NO detiene el proceso
                    log.warn("Fila {} ignorada: {}", i + 1, UtilSeguridad.limpiarParaLog(e.getMessage()));
                }
            }
        }
        log.info("Excel leído: {} facturas válidas ({})", facturas.size(), FileUtils.byteCountToDisplaySize(bytes));
        return facturas;
    }

    private FacturaResumen convertir(Row f) {
        String serie = f.getCell(1).getStringCellValue();
        String numero = f.getCell(2).getStringCellValue();
        String moneda = f.getCell(6).getStringCellValue();
        BigDecimal total = BigDecimal.valueOf(f.getCell(7).getNumericCellValue()).setScale(2, RoundingMode.HALF_UP);
        Validate.notBlank(serie, "La serie es obligatoria");   // Commons Lang3
        Validate.notBlank(numero, "El número es obligatorio");
        checkArgument("PEN".equals(moneda) || "USD".equals(moneda), "Moneda inválida: %s", moneda); // Guava
        checkArgument(total.signum() > 0, "El total debe ser mayor que cero (recibido: %s)", total);
        return new FacturaResumen(
            (long) f.getCell(0).getNumericCellValue(), f.getCell(3).getStringCellValue(),
            f.getCell(4).getStringCellValue(), serie, numero,
            f.getCell(5).getLocalDateTimeCellValue().toLocalDate(), total,
            f.getCell(8).getStringCellValue(), moneda);
    }
}
