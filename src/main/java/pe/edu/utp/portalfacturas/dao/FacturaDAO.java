package pe.edu.utp.portalfacturas.dao;

import java.util.List;
import pe.edu.utp.portalfacturas.model.FacturaForm;
import pe.edu.utp.portalfacturas.model.FacturaResumen;
import pe.edu.utp.portalfacturas.model.OrdenCompraOption;
import pe.edu.utp.portalfacturas.model.ProveedorOption;

public interface FacturaDAO {
    List<FacturaResumen> listarTodas();
    List<FacturaResumen> listarPorProveedor(Long proveedorId);
    boolean existeDuplicada(Long proveedorId, String serie, String numero);
    boolean ordenPerteneceProveedor(Long ordenCompraId, Long proveedorId);
    List<ProveedorOption> listarProveedores();
    List<OrdenCompraOption> listarOrdenesCompra(Long proveedorId);
    Long registrar(FacturaForm factura, Long usuarioId);
    void registrarDocumento(Long facturaId, String tipoDocumento, String nombreArchivo, String rutaAlmacenamiento, long tamanioBytes);
}
