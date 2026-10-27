package pe.edu.utp.portalfacturas.service;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Joiner;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimaps;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import pe.edu.utp.portalfacturas.excel.ExportadorExcel;
import pe.edu.utp.portalfacturas.model.FacturaResumen;

/** Agrupaciones (Guava) y reporte Excel (POI) sobre las facturas visibles para el usuario. */
@Service
public class FacturaReporteService {
    private static final Logger log = LoggerFactory.getLogger(FacturaReporteService.class);
    private final ExportadorExcel exportador = new ExportadorExcel();

    public record ResumenProveedor(String proveedor, String moneda, int cantidad, BigDecimal total) { }

    /** Multimap: un proveedor con VARIAS facturas. */
    public ImmutableListMultimap<String, FacturaResumen> agruparPorProveedor(List<FacturaResumen> facturas) {
        return Multimaps.index(facturas, FacturaResumen::proveedor);
    }

    /** Divide en lotes (p. ej. lotes de pago para Tesorería); el último puede ser menor. */
    public List<List<FacturaResumen>> dividirEnLotes(List<FacturaResumen> facturas, int tamanoLote) {
        checkArgument(tamanoLote > 0, "El tamaño del lote debe ser mayor que 0 (recibido: %s)", tamanoLote);
        return Lists.partition(facturas, tamanoLote);
    }

    /** Total por moneda: nunca se suman PEN con USD. */
    public ImmutableSortedMap<String, BigDecimal> totalPorMoneda(List<FacturaResumen> facturas) {
        Map<String, BigDecimal> mapa = facturas.stream().collect(Collectors.groupingBy(
            FacturaResumen::moneda, TreeMap::new,
            Collectors.reducing(BigDecimal.ZERO, FacturaResumen::total, BigDecimal::add)));
        return ImmutableSortedMap.copyOf(mapa);
    }

    public ImmutableList<ResumenProveedor> resumenPorProveedor(List<FacturaResumen> facturas) {
        ImmutableList.Builder<ResumenProveedor> resumen = ImmutableList.builder();
        agruparPorProveedor(facturas).asMap().forEach((proveedor, grupo) -> {
            Map<String, List<FacturaResumen>> porMoneda = grupo.stream()
                .collect(Collectors.groupingBy(FacturaResumen::moneda, TreeMap::new, Collectors.toList()));
            porMoneda.forEach((moneda, lista) -> resumen.add(new ResumenProveedor(proveedor, moneda, lista.size(),
                lista.stream().map(FacturaResumen::total).reduce(BigDecimal.ZERO, BigDecimal::add))));
        });
        return resumen.build();
    }

    public String proveedoresAtendidos(List<FacturaResumen> facturas) {
        return Joiner.on(", ").join(agruparPorProveedor(facturas).keySet());
    }

    /** Genera el .xlsx en memoria (no se escribe en disco del servidor). */
    public byte[] generarExcel(List<FacturaResumen> facturas) throws IOException {
        Stopwatch cronometro = Stopwatch.createStarted();
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        exportador.exportar(facturas, resumenPorProveedor(facturas), salida);
        log.info("Reporte Excel generado: {} facturas, {} bytes, {}", facturas.size(), salida.size(), cronometro.stop());
        log.debug("Proveedores incluidos: {}", proveedoresAtendidos(facturas));
        return salida.toByteArray();
    }
}
