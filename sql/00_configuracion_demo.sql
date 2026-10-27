-- EJECUTAR CON UNA CUENTA ADMINISTRADORA DE POSTGRESQL (por ejemplo postgres).
-- Este archivo prepara una cuenta técnica para la aplicación.
-- Credenciales de DEMO local:
-- usuario BD: portal_app
-- contraseña BD: PortalDB123!

DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'portal_app') THEN
        CREATE ROLE portal_app LOGIN PASSWORD 'PortalDB123!';
    ELSE
        ALTER ROLE portal_app WITH LOGIN PASSWORD 'PortalDB123!';
    END IF;
END $$;

-- Si la base ya existe, el siguiente GRANT permite el acceso.
GRANT CONNECT ON DATABASE portal_facturas TO portal_app;
