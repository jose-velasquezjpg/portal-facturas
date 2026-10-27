package pe.edu.utp.portalfacturas.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import pe.edu.utp.portalfacturas.model.PortalUserDetails;

@Service
public class PortalUserDetailsService implements UserDetailsService {
    private final JdbcTemplate jdbc;
    public PortalUserDetailsService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override public PortalUserDetails loadUserByUsername(String correo) {
        String sql = "SELECT u.usuario_id, u.nombre, u.correo, u.password_hash, u.proveedor_id, u.activo, r.nombre_rol " +
            "FROM usuarios u JOIN roles r ON r.rol_id = u.rol_id WHERE LOWER(u.correo) = LOWER(?)";
        return jdbc.query(sql, rs -> {
            if (!rs.next()) throw new UsernameNotFoundException("Usuario no encontrado");
            long providerValue = rs.getLong("proveedor_id");
            Long proveedorId = rs.wasNull() ? null : providerValue;
            return new PortalUserDetails(rs.getLong("usuario_id"), rs.getString("nombre"),
                rs.getString("correo"), rs.getString("password_hash"), proveedorId,
                rs.getBoolean("activo"), rs.getString("nombre_rol"));
        }, correo);
    }
}
