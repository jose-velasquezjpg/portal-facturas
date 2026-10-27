-- Ejecutar conectado a la base portal_facturas con una cuenta administradora de PostgreSQL.
-- Otorga permisos de operación al usuario técnico portal_app.
GRANT USAGE ON SCHEMA public TO portal_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO portal_app;
GRANT USAGE, SELECT, UPDATE ON ALL SEQUENCES IN SCHEMA public TO portal_app;
