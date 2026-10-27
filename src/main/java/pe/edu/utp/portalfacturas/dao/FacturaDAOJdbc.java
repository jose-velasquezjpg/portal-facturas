package pe.edu.utp.portalfacturas.dao;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import pe.edu.utp.portalfacturas.model.FacturaForm;
import pe.edu.utp.portalfacturas.model.FacturaResumen;
import pe.edu.utp.portalfacturas.model.OrdenCompraOption;
import pe.edu.utp.portalfacturas.model.ProveedorOption;

@Repository
public class FacturaDAOJdbc implements FacturaDAO {
    private final JdbcTemplate jdbc;
    public FacturaDAOJdbc(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    private static final RowMapper<FacturaResumen> FACTURA_MAPPER = (rs, rowNum) ->
        new FacturaResumen(rs.getLong("factura_id"), rs.getString("razon_social"),
            rs.getString("numero_oc"), rs.getString("serie"), rs.getString("numero"),
            rs.getDate("fecha_emision").toLocalDate(), rs.getBigDecimal("total"),
            rs.getString("estado_factura"), rs.getString("moneda"));

    private static final String SELECT_FACTURAS = "SELECT f.factura_id, p.razon_social, oc.numero_oc, " +
        "f.serie, f.numero, f.fecha_emision, f.total, f.estado_factura, f.moneda " +
        "FROM facturas f JOIN proveedores p ON p.proveedor_id = f.proveedor_id " +
        "JOIN ordenes_compra oc ON oc.orden_compra_id = f.orden_compra_id ";

    @Override public List<FacturaResumen> listarTodas() {
        return jdbc.query(SELECT_FACTURAS + " ORDER BY f.fecha_recepcion DESC, f.factura_id DESC", FACTURA_MAPPER);
    }
    @Override public List<FacturaResumen> listarPorProveedor(Long proveedorId) {
        return jdbc.query(SELECT_FACTURAS + " WHERE f.proveedor_id = ? ORDER BY f.fecha_recepcion DESC, f.factura_id DESC",
            FACTURA_MAPPER, proveedorId);
    }
    @Override public boolean existeDuplicada(Long proveedorId, String serie, String numero) {
        Integer cantidad = jdbc.queryForObject("SELECT COUNT(*) FROM facturas WHERE proveedor_id = ? AND UPPER(serie) = UPPER(?) AND numero = ?",
            Integer.class, proveedorId, serie, numero);
        return cantidad != null && cantidad > 0;
    }
    @Override public boolean ordenPerteneceProveedor(Long ordenCompraId, Long proveedorId) {
        Integer cantidad = jdbc.queryForObject("SELECT COUNT(*) FROM ordenes_compra WHERE orden_compra_id = ? AND proveedor_id = ?",
            Integer.class, ordenCompraId, proveedorId);
        return cantidad != null && cantidad > 0;
    }
    @Override public List<ProveedorOption> listarProveedores() {
        return jdbc.query("SELECT proveedor_id, razon_social FROM proveedores WHERE estado = 'ACTIVO' ORDER BY razon_social",
            (rs, n) -> new ProveedorOption(rs.getLong("proveedor_id"), rs.getString("razon_social")));
    }
    @Override public List<OrdenCompraOption> listarOrdenesCompra(Long proveedorId) {
        String sql = "SELECT orden_compra_id, numero_oc, proveedor_id FROM ordenes_compra " +
            (proveedorId == null ? "" : "WHERE proveedor_id = ? ") + "ORDER BY numero_oc";
        RowMapper<OrdenCompraOption> mapper = (rs, n) -> new OrdenCompraOption(
            rs.getLong("orden_compra_id"), rs.getString("numero_oc"), rs.getLong("proveedor_id"));
        return proveedorId == null ? jdbc.query(sql, mapper) : jdbc.query(sql, mapper, proveedorId);
    }
    @Override public void registrarDocumento(Long facturaId, String tipoDocumento, String nombreArchivo, String rutaAlmacenamiento, long tamanioBytes) {
        jdbc.update("INSERT INTO documentos_adjuntos (factura_id, tipo_documento, nombre_archivo, ruta_almacenamiento, tamanio_bytes) VALUES (?, ?, ?, ?, ?)",
            facturaId, tipoDocumento, nombreArchivo, rutaAlmacenamiento, tamanioBytes);
    }

    @Override public Long registrar(FacturaForm f, Long usuarioId) {
        String sql = "INSERT INTO facturas (proveedor_id, orden_compra_id, serie, numero, fecha_emision, moneda, subtotal, igv, total, estado_factura) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'Registrada')";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, f.getProveedorId());
            ps.setLong(2, f.getOrdenCompraId());
            ps.setString(3, f.getSerie());
            ps.setString(4, f.getNumero());
            ps.setDate(5, Date.valueOf(f.getFechaEmision()));
            ps.setString(6, f.getMoneda());
            ps.setBigDecimal(7, f.getSubtotal());
            ps.setBigDecimal(8, f.getIgv());
            ps.setBigDecimal(9, f.getTotal());
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) throw new IllegalStateException("No se pudo obtener el ID de la factura registrada");
        Long facturaId = key.longValue();
        jdbc.update("INSERT INTO historial_facturas (factura_id, usuario_id, estado_anterior, estado_nuevo, observacion) VALUES (?, ?, NULL, 'Registrada', ?)",
            facturaId, usuarioId, "Registro inicial desde el portal web");
        return facturaId;
    }
}
