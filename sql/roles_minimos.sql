-- Ejecutar después de crear la tabla roles de BD_Portal_Facturas.sql.
-- Nombres esperados por SecurityConfig.java: evita variantes con tildes o espacios.
INSERT INTO roles (nombre_rol) VALUES
    ('ADMIN'), ('COMPRAS'), ('CONTABILIDAD'), ('TESORERIA'), ('PROVEEDOR')
ON CONFLICT (nombre_rol) DO NOTHING;

-- No se insertan usuarios ni contraseñas predeterminadas.
-- Crea cada usuario con password_hash BCrypt y relaciona rol_id.
-- Si el usuario es interno, proveedor_id puede quedar NULL.
-- Si el usuario es proveedor, asigna proveedor_id al registro de proveedores correspondiente.
