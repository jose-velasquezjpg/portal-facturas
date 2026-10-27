package pe.edu.utp.portalfacturas.model;

import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public final class PortalUserDetails implements UserDetails {
    private final Long usuarioId;
    private final String nombre;
    private final String correo;
    private final String passwordHash;
    private final Long proveedorId;
    private final boolean activo;
    private final String rol;

    public PortalUserDetails(Long usuarioId, String nombre, String correo, String passwordHash,
                             Long proveedorId, boolean activo, String rol) {
        this.usuarioId = usuarioId;
        this.nombre = nombre;
        this.correo = correo;
        this.passwordHash = passwordHash;
        this.proveedorId = proveedorId;
        this.activo = activo;
        this.rol = rol == null ? "" : rol.trim().toUpperCase();
    }
    public Long getUsuarioId() { return usuarioId; }
    public String getNombre() { return nombre; }
    public Long getProveedorId() { return proveedorId; }
    public String getRol() { return rol; }
    @Override public Collection<? extends GrantedAuthority> getAuthorities() {
        return java.util.List.of(new SimpleGrantedAuthority("ROLE_" + rol));
    }
    @Override public String getPassword() { return passwordHash; }
    @Override public String getUsername() { return correo; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return activo; }
}
