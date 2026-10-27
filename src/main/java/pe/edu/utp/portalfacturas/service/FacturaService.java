package pe.edu.utp.portalfacturas.service;

import com.google.common.collect.ImmutableList;
import java.math.BigDecimal;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import pe.edu.utp.portalfacturas.dao.FacturaDAO;
import pe.edu.utp.portalfacturas.model.FacturaForm;
import pe.edu.utp.portalfacturas.model.FacturaResumen;
import pe.edu.utp.portalfacturas.model.OrdenCompraOption;
import pe.edu.utp.portalfacturas.model.ProveedorOption;
import pe.edu.utp.portalfacturas.storage.DocumentoStorageService;
import pe.edu.utp.portalfacturas.util.UtilSeguridad;

@Service
public class FacturaService {
    private static final Logger log = LoggerFactory.getLogger(FacturaService.class);
    private final FacturaDAO dao;
    private final DocumentoStorageService documentoStorage;

    public FacturaService(FacturaDAO dao) {
        this(dao, new DocumentoStorageService("uploads/facturas"));
    }

    @Autowired
    public FacturaService(FacturaDAO dao, DocumentoStorageService documentoStorage) {
        this.dao = dao;
        this.documentoStorage = documentoStorage;
    }

    /** Listas INMUTABLES: ninguna otra capa puede modificar los resultados (Guava). */
    public List<FacturaResumen> listarTodas() { return ImmutableList.copyOf(dao.listarTodas()); }
    public List<FacturaResumen> listarPorProveedor(Long proveedorId) { return ImmutableList.copyOf(dao.listarPorProveedor(proveedorId)); }
    public List<ProveedorOption> listarProveedores() { return dao.listarProveedores(); }
    public List<OrdenCompraOption> listarOrdenesCompra(Long proveedorId) { return dao.listarOrdenesCompra(proveedorId); }

    /** Mantiene compatibilidad con las pruebas y usos anteriores sin adjuntos. */
    @Transactional
    public Long registrar(FacturaForm f, Long usuarioId, Long proveedorForzado) {
        return registrarConLog(f, usuarioId, proveedorForzado, null, null);
    }

    /** Registra la factura y almacena el PDF y XML asociados. */
    @Transactional
    public Long registrar(FacturaForm f, Long usuarioId, Long proveedorForzado, MultipartFile archivoXml, MultipartFile archivoPdf) {
        return registrarConLog(f, usuarioId, proveedorForzado, archivoXml, archivoPdf);
    }

    /** Registra eventos: INFO si se registra, WARN si se rechaza por regla de negocio. */
    private Long registrarConLog(FacturaForm f, Long usuarioId, Long proveedorForzado, MultipartFile archivoXml, MultipartFile archivoPdf) {
        try {
            Long id = registrarInterno(f, usuarioId, proveedorForzado, archivoXml, archivoPdf);
            log.info("Factura registrada: id={}, proveedorId={}, comprobante={}-{}, usuarioId={}, adjuntos={}",
                id, f.getProveedorId(), UtilSeguridad.limpiarParaLog(f.getSerie()),
                UtilSeguridad.limpiarParaLog(f.getNumero()), usuarioId, archivoXml != null || archivoPdf != null);
            return id;
        } catch (ReglaNegocioException ex) {
            log.warn("Registro de factura rechazado (usuarioId={}): {}", usuarioId, UtilSeguridad.limpiarParaLog(ex.getMessage()));
            throw ex;
        }
    }

    private Long registrarInterno(FacturaForm f, Long usuarioId, Long proveedorForzado, MultipartFile archivoXml, MultipartFile archivoPdf) {
        if (f == null) throw new ReglaNegocioException("Debe ingresar la información de la factura");
        Long proveedorId = proveedorForzado != null ? proveedorForzado : f.getProveedorId();
        if (proveedorId == null || f.getOrdenCompraId() == null || usuarioId == null)
            throw new ReglaNegocioException("Proveedor, orden de compra y usuario son obligatorios");
        if (StringUtils.isBlank(f.getSerie())) throw new ReglaNegocioException("La serie es obligatoria");
        if (StringUtils.isBlank(f.getNumero())) throw new ReglaNegocioException("El número de factura es obligatorio");
        if (f.getFechaEmision() == null) throw new ReglaNegocioException("La fecha de emisión es obligatoria");
        if (f.getSubtotal() == null || f.getSubtotal().signum() < 0) throw new ReglaNegocioException("El subtotal no puede ser negativo");
        if (f.getIgv() == null || f.getIgv().signum() < 0) throw new ReglaNegocioException("El IGV no puede ser negativo");
        if (f.getTotal() == null || f.getTotal().signum() <= 0) throw new ReglaNegocioException("El total debe ser mayor que cero");
        if (!"PEN".equals(f.getMoneda()) && !"USD".equals(f.getMoneda())) throw new ReglaNegocioException("La moneda debe ser PEN o USD");
        BigDecimal calculado = f.getSubtotal().add(f.getIgv()).setScale(2, java.math.RoundingMode.HALF_UP);
        if (calculado.compareTo(f.getTotal().setScale(2, java.math.RoundingMode.HALF_UP)) != 0)
            throw new ReglaNegocioException("El total debe ser igual al subtotal más el IGV");
        if (!dao.ordenPerteneceProveedor(f.getOrdenCompraId(), proveedorId))
            throw new ReglaNegocioException("La orden de compra seleccionada no pertenece al proveedor");
        String serie = f.getSerie().trim().toUpperCase(java.util.Locale.ROOT);
        String numero = f.getNumero().trim();
        if (dao.existeDuplicada(proveedorId, serie, numero))
            throw new ReglaNegocioException("Ya existe una factura con ese proveedor, serie y número");
        f.setProveedorId(proveedorId);
        f.setSerie(serie);
        f.setNumero(numero);

        boolean conAdjuntos = archivoXml != null || archivoPdf != null;
        if (conAdjuntos) {
            validarArchivo(archivoXml, "XML");
            validarArchivo(archivoPdf, "PDF");
        }

        Long facturaId = dao.registrar(f, usuarioId);
        java.util.List<DocumentoStorageService.DocumentoGuardado> guardados = new java.util.ArrayList<>();
        try {
            if (conAdjuntos) {
                var xml = documentoStorage.guardar(facturaId, "XML", archivoXml);
                guardados.add(xml);
                dao.registrarDocumento(facturaId, "XML", xml.nombreOriginal(), xml.rutaAlmacenamiento(), xml.tamanioBytes());

                var pdf = documentoStorage.guardar(facturaId, "PDF", archivoPdf);
                guardados.add(pdf);
                dao.registrarDocumento(facturaId, "PDF", pdf.nombreOriginal(), pdf.rutaAlmacenamiento(), pdf.tamanioBytes());
            }
            return facturaId;
        } catch (RuntimeException ex) {
            log.error("Falló el guardado de adjuntos de la factura {}; se eliminan los archivos ya escritos", facturaId, ex);
            guardados.forEach(documentoStorage::eliminar);
            throw ex;
        }
    }

    private void validarArchivo(MultipartFile archivo, String tipo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new ReglaNegocioException("Debe adjuntar el archivo " + tipo + " de la factura.");
        }
        String nombre = archivo.getOriginalFilename() == null ? "" : archivo.getOriginalFilename().toLowerCase(java.util.Locale.ROOT);
        String extension = "XML".equals(tipo) ? ".xml" : ".pdf";
        if (!nombre.endsWith(extension)) {
            throw new ReglaNegocioException("El archivo " + tipo + " debe tener formato " + extension + ".");
        }
        long maximo = 10L * 1024L * 1024L;
        if (archivo.getSize() > maximo) {
            throw new ReglaNegocioException("El archivo " + tipo + " no puede superar los 10 MB.");
        }
    }
}
