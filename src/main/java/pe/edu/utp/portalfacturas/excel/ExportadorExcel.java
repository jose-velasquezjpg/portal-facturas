package pe.edu.utp.portalfacturas.excel;

import java.io.IOException;
import java.io.OutputStream;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pe.edu.utp.portalfacturas.model.FacturaResumen;
import pe.edu.utp.portalfacturas.service.FacturaReporteService.ResumenProveedor;
import pe.edu.utp.portalfacturas.util.UtilSeguridad;

/** Genera el reporte Excel de facturas (hoja detalle + hoja resumen por proveedor). */
public class ExportadorExcel {
    private static final Logger log = LoggerFactory.getLogger(ExportadorExcel.class);

    public static final String HOJA_FACTURAS = "Facturas";
    public static final String HOJA_RESUMEN = "Resumen por proveedor";
    static final String[] CABECERAS = {"ID", "Serie", "Número", "Proveedor", "Orden de compra",
        "Fecha emisión", "Moneda", "Total", "Estado"};
    private static final int[] ANCHOS = {8, 10, 16, 34, 18, 16, 10, 16, 16};

    /** El flujo de salida lo cierra quien lo abrió; aquí solo se cierra el libro (try-with-resources). */
    public void exportar(List<FacturaResumen> facturas, List<ResumenProveedor> resumen,
                         OutputStream salida) throws IOException {
        try (XSSFWorkbook libro = new XSSFWorkbook()) {
            Font negrita = libro.createFont();
            negrita.setBold(true);
            Font blanca = libro.createFont();
            blanca.setBold(true);
            blanca.setColor(IndexedColors.WHITE.getIndex());

            CellStyle estiloCabecera = libro.createCellStyle();
            estiloCabecera.setFont(blanca);
            estiloCabecera.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            estiloCabecera.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            CellStyle estiloMoneda = libro.createCellStyle();
            estiloMoneda.setDataFormat(libro.createDataFormat().getFormat("#,##0.00"));
            CellStyle estiloFecha = libro.createCellStyle();
            estiloFecha.setDataFormat(libro.createDataFormat().getFormat("dd/mm/yyyy"));
            CellStyle estiloTotal = libro.createCellStyle();
            estiloTotal.setFont(negrita);
            estiloTotal.setDataFormat(libro.createDataFormat().getFormat("#,##0.00"));

            // ---- Hoja 1: detalle ----
            Sheet hoja = libro.createSheet(HOJA_FACTURAS);
            escribirCabecera(hoja, CABECERAS, ANCHOS, estiloCabecera);
            int fila = 1;
            for (FacturaResumen f : facturas) {
                Row r = hoja.createRow(fila++);
                r.createCell(0).setCellValue(f.facturaId() == null ? 0 : f.facturaId());
                r.createCell(1).setCellValue(UtilSeguridad.neutralizarFormula(f.serie()));
                r.createCell(2).setCellValue(UtilSeguridad.neutralizarFormula(f.numero()));
                r.createCell(3).setCellValue(UtilSeguridad.neutralizarFormula(f.proveedor()));
                r.createCell(4).setCellValue(UtilSeguridad.neutralizarFormula(f.numeroOc()));
                Cell fecha = r.createCell(5);
                fecha.setCellValue(Date.from(f.fechaEmision().atStartOfDay(ZoneId.systemDefault()).toInstant()));
                fecha.setCellStyle(estiloFecha);
                r.createCell(6).setCellValue(UtilSeguridad.neutralizarFormula(f.moneda()));
                Cell total = r.createCell(7);
                total.setCellValue(f.total().doubleValue());
                total.setCellStyle(estiloMoneda);
                r.createCell(8).setCellValue(UtilSeguridad.neutralizarFormula(f.estadoFactura()));
            }
            // Totales con fórmulas de Excel, uno por moneda presente (no se suman PEN con USD)
            if (!facturas.isEmpty()) {
                int ultima = facturas.size() + 1; // fila de Excel de la última factura
                fila++;
                for (String moneda : facturas.stream().map(FacturaResumen::moneda).distinct().sorted().toList()) {
                    Row rt = hoja.createRow(fila++);
                    rt.createCell(5).setCellValue("TOTAL " + moneda);
                    Cell suma = rt.createCell(7);
                    suma.setCellFormula("SUMIF(G2:G" + ultima + ",\"" + moneda + "\",H2:H" + ultima + ")");
                    suma.setCellStyle(estiloTotal);
                }
            }
            hoja.createFreezePane(0, 1);
            hoja.setAutoFilter(new CellRangeAddress(0, 0, 0, CABECERAS.length - 1));

            // ---- Hoja 2: resumen por proveedor ----
            Sheet hojaRes = libro.createSheet(HOJA_RESUMEN);
            escribirCabecera(hojaRes, new String[] {"Proveedor", "Moneda", "N° facturas", "Total"},
                new int[] {34, 10, 14, 18}, estiloCabecera);
            int fr = 1;
            for (ResumenProveedor rp : resumen) {
                Row r = hojaRes.createRow(fr++);
                r.createCell(0).setCellValue(UtilSeguridad.neutralizarFormula(rp.proveedor()));
                r.createCell(1).setCellValue(UtilSeguridad.neutralizarFormula(rp.moneda()));
                r.createCell(2).setCellValue(rp.cantidad());
                Cell total = r.createCell(3);
                total.setCellValue(rp.total().doubleValue());
                total.setCellStyle(estiloMoneda);
            }
            hojaRes.createFreezePane(0, 1);

            libro.setForceFormulaRecalculation(true);
            libro.write(salida);
            log.debug("Excel exportado: {} facturas, {} filas de resumen", facturas.size(), resumen.size());
        }
    }

    private void escribirCabecera(Sheet hoja, String[] cabeceras, int[] anchos, CellStyle estilo) {
        Row fila = hoja.createRow(0);
        for (int i = 0; i < cabeceras.length; i++) {
            Cell c = fila.createCell(i);
            c.setCellValue(cabeceras[i]);
            c.setCellStyle(estilo);
            hoja.setColumnWidth(i, anchos[i] * 256);
        }
    }
}
