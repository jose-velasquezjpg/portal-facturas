-- USUARIOS DE DEMOSTRACIÓN DEL PORTAL
-- Ejecutar después de BD_Portal_Facturas.sql, roles_minimos.sql y datos_demo_proveedor_oc.sql.
-- Contraseñas de demo:
-- ADMIN:      Admin123!
-- PROVEEDOR:  Proveedor123!
-- Los hashes se almacenan con BCrypt; nunca se guarda la contraseña en texto plano.

INSERT INTO usuarios (nombre, correo, password_hash, rol_id, proveedor_id, activo)
SELECT 'Administrador TAI LOY', 'admin.portal@ejemplo.test',
       '$2a$10$YiVCwt6dl/1Gpg0o09MOoOxLmoOrolb/ZOUfc1uufdOtpzoIHueLS',
       r.rol_id, NULL, TRUE
FROM roles r WHERE r.nombre_rol = 'ADMIN'
ON CONFLICT (correo) DO UPDATE SET password_hash = EXCLUDED.password_hash, rol_id = EXCLUDED.rol_id, activo = TRUE;

INSERT INTO usuarios (nombre, correo, password_hash, rol_id, proveedor_id, activo)
SELECT 'Proveedor Demo', 'proveedor.portal@ejemplo.test',
       '$2a$10$zUspXrZdpurQ22N7bxu3mOYiQ6gWdvMnof7jJapIj2/mqjKPqcz/e',
       r.rol_id, p.proveedor_id, TRUE
FROM roles r CROSS JOIN proveedores p
WHERE r.nombre_rol = 'PROVEEDOR' AND p.ruc = '20609999991'
ON CONFLICT (correo) DO UPDATE SET password_hash = EXCLUDED.password_hash, rol_id = EXCLUDED.rol_id, proveedor_id = EXCLUDED.proveedor_id, activo = TRUE;

SELECT u.usuario_id, u.nombre, u.correo, r.nombre_rol, u.activo
FROM usuarios u JOIN roles r ON r.rol_id = u.rol_id
WHERE u.correo IN ('admin.portal@ejemplo.test','proveedor.portal@ejemplo.test');
