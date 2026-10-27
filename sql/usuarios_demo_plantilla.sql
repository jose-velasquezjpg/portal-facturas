-- PLANTILLA: REEMPLAZA el texto PEGAR_HASH_BCRYPT_REAL por el resultado de GenerarHashBCrypt.
-- No uses una contraseña en texto plano en password_hash.
-- El proveedor demo se crea en datos_demo_proveedor_oc.sql.

-- Usuario interno administrador. Ejecutar después de roles_minimos.sql y del esquema principal.
INSERT INTO usuarios (nombre, correo, password_hash, rol_id, proveedor_id, activo)
SELECT 'Administrador de prueba', 'admin.portal@ejemplo.test', 'PEGAR_HASH_BCRYPT_REAL', r.rol_id, NULL, TRUE
FROM roles r WHERE r.nombre_rol = 'ADMIN'
ON CONFLICT (correo) DO NOTHING;

-- Usuario proveedor de prueba. Usa otro hash BCrypt, preferentemente otra contraseña.
INSERT INTO usuarios (nombre, correo, password_hash, rol_id, proveedor_id, activo)
SELECT 'Usuario proveedor demo', 'proveedor.portal@ejemplo.test', 'PEGAR_HASH_BCRYPT_REAL', r.rol_id, p.proveedor_id, TRUE
FROM roles r CROSS JOIN proveedores p
WHERE r.nombre_rol = 'PROVEEDOR' AND p.ruc = '20609999991'
ON CONFLICT (correo) DO NOTHING;

-- Antes de iniciar sesión, confirma que ambos usuarios tengan hashes reales, no el texto de plantilla.
SELECT u.usuario_id, u.correo, r.nombre_rol, u.proveedor_id, u.activo
FROM usuarios u JOIN roles r ON r.rol_id = u.rol_id
WHERE u.correo IN ('admin.portal@ejemplo.test', 'proveedor.portal@ejemplo.test');
