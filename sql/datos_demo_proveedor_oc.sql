-- DATOS FICTICIOS PARA PRUEBAS LOCALES. Ejecutar después de crear el esquema.
-- El RUC solo satisface la restricción de longitud del esquema académico; no representa un proveedor real.

INSERT INTO proveedores (ruc, razon_social, correo, telefono, direccion, estado)
VALUES ('20609999991', 'Proveedor Demo S.A.C.', 'demo.proveedor@ejemplo.test', '900000001', 'Dirección ficticia - Lima', 'ACTIVO')
ON CONFLICT (ruc) DO NOTHING;

INSERT INTO ordenes_compra (numero_oc, proveedor_id, fecha_emision, monto_total, moneda, estado_oc)
SELECT 'OC-DEMO-001', p.proveedor_id, DATE '2026-10-01', 1180.00, 'PEN', 'EMITIDA'
FROM proveedores p
WHERE p.ruc = '20609999991'
ON CONFLICT (numero_oc) DO NOTHING;

-- Verifica los identificadores que se muestran en el formulario web.
SELECT proveedor_id, ruc, razon_social FROM proveedores WHERE ruc = '20609999991';
SELECT orden_compra_id, numero_oc, proveedor_id, monto_total FROM ordenes_compra WHERE numero_oc = 'OC-DEMO-001';
