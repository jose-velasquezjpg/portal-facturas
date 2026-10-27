-- 1. TABLA DE ROLES
CREATE TABLE roles (
    rol_id SERIAL PRIMARY KEY,
    nombre_rol VARCHAR(50) NOT NULL UNIQUE
);


-- 2. TABLA DE USUARIOS
CREATE TABLE usuarios (
    usuario_id SERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    correo VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    rol_id INT NOT NULL,
	proveedor_id INT,
    activo BOOLEAN DEFAULT TRUE NOT NULL,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_usuarios_rol FOREIGN KEY (rol_id) REFERENCES roles(rol_id) ON DELETE RESTRICT
);


-- 3. TABLA DE PROVEEDORES
CREATE TABLE proveedores (
    proveedor_id SERIAL PRIMARY KEY,
    ruc VARCHAR(11) NOT NULL UNIQUE,
    razon_social VARCHAR(150) NOT NULL,
    correo VARCHAR(150) NOT NULL UNIQUE,
    telefono VARCHAR(20),
    direccion TEXT,
    estado VARCHAR(20) DEFAULT 'ACTIVO' NOT NULL,
    fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT chk_ruc_longitud CHECK (LENGTH(ruc) = 11)
);

-- RELACIÓN USUARIOS - PROVEEDORES
ALTER TABLE usuarios
ADD CONSTRAINT fk_usuarios_proveedor
FOREIGN KEY (proveedor_id)
REFERENCES proveedores(proveedor_id)
ON DELETE RESTRICT;

-- 4. TABLA DE ÓRDENES DE COMPRA
CREATE TABLE ordenes_compra (
    orden_compra_id SERIAL PRIMARY KEY,
    numero_oc VARCHAR(50) NOT NULL UNIQUE,
    proveedor_id INT NOT NULL,
    fecha_emision DATE NOT NULL,
    monto_total DECIMAL(12, 2) NOT NULL,
    moneda VARCHAR(3) DEFAULT 'PEN' NOT NULL,
    estado_oc VARCHAR(30) DEFAULT 'EMITIDA' NOT NULL,
    CONSTRAINT fk_oc_proveedor FOREIGN KEY (proveedor_id) REFERENCES proveedores(proveedor_id) ON DELETE RESTRICT,
    CONSTRAINT chk_monto_oc CHECK (monto_total > 0)
);

-- 5. TABLA DE DETALLE DE ÓRDENES DE COMPRA
CREATE TABLE detalle_ordenes_compra (
    detalle_oc_id SERIAL PRIMARY KEY,
    orden_compra_id INT NOT NULL,
    descripcion_item TEXT NOT NULL,
    cantidad INT NOT NULL,
    precio_unitario DECIMAL(12, 2) NOT NULL,
    subtotal DECIMAL(12, 2) NOT NULL,
    CONSTRAINT fk_detalle_oc FOREIGN KEY (orden_compra_id) REFERENCES ordenes_compra(orden_compra_id) ON DELETE CASCADE,
    CONSTRAINT chk_cantidad_oc CHECK (cantidad > 0),
    CONSTRAINT chk_precio_oc CHECK (precio_unitario >= 0)
);

-- 6. TABLA DE FACTURAS
CREATE TABLE facturas (
    factura_id SERIAL PRIMARY KEY,
    proveedor_id INT NOT NULL,
    orden_compra_id INT NOT NULL,
    serie VARCHAR(5) NOT NULL,
    numero VARCHAR(20) NOT NULL,
    fecha_emision DATE NOT NULL,
    fecha_recepcion TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    moneda VARCHAR(3) DEFAULT 'PEN' NOT NULL,
    subtotal DECIMAL(12, 2) NOT NULL,
    igv DECIMAL(12, 2) NOT NULL,
    total DECIMAL(12, 2) NOT NULL,
    estado_factura VARCHAR(30) DEFAULT 'Registrada' NOT NULL,
    CONSTRAINT fk_factura_proveedor FOREIGN KEY (proveedor_id) REFERENCES proveedores(proveedor_id) ON DELETE RESTRICT,
    CONSTRAINT fk_factura_oc FOREIGN KEY (orden_compra_id) REFERENCES ordenes_compra(orden_compra_id) ON DELETE RESTRICT,
    CONSTRAINT unq_factura_duplicada UNIQUE (proveedor_id, serie, numero),
    CONSTRAINT chk_subtotal_factura CHECK (subtotal >= 0),
    CONSTRAINT chk_igv_factura CHECK (igv >= 0),
    CONSTRAINT chk_total_factura CHECK (total > 0)
);

-- 7. TABLA DE DETALLE DE FACTURAS
CREATE TABLE detalle_facturas (
    detalle_factura_id SERIAL PRIMARY KEY,
    factura_id INT NOT NULL,
    descripcion_item TEXT NOT NULL,
    cantidad INT NOT NULL,
    precio_unitario DECIMAL(12, 2) NOT NULL,
    subtotal DECIMAL(12, 2) NOT NULL,
    CONSTRAINT fk_detalle_factura FOREIGN KEY (factura_id) REFERENCES facturas(factura_id) ON DELETE CASCADE,
    CONSTRAINT chk_cantidad_factura CHECK (cantidad > 0),
    CONSTRAINT chk_precio_factura CHECK (precio_unitario >= 0)
);

-- 8. TABLA DE DOCUMENTOS ADJUNTOS
CREATE TABLE documentos_adjuntos (
    documento_id SERIAL PRIMARY KEY,
    factura_id INT NOT NULL,
    tipo_documento VARCHAR(10) NOT NULL, -- Ej: 'PDF', 'XML'
    nombre_archivo VARCHAR(255) NOT NULL,
    ruta_almacenamiento TEXT NOT NULL,
    tamanio_bytes INT NOT NULL,
    fecha_carga TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_documento_factura FOREIGN KEY (factura_id) REFERENCES facturas(factura_id) ON DELETE CASCADE,
    CONSTRAINT chk_tipo_doc CHECK (tipo_documento IN ('PDF', 'XML'))
);

-- 9. TABLA DE HISTORIAL DE FACTURAS / ESTADOS
CREATE TABLE historial_facturas (
    historial_id SERIAL PRIMARY KEY,
    factura_id INT NOT NULL,
    usuario_id INT NOT NULL,
    estado_anterior VARCHAR(30),
    estado_nuevo VARCHAR(30) NOT NULL,
    observacion TEXT,
    fecha_cambio TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_historial_factura FOREIGN KEY (factura_id) REFERENCES facturas(factura_id) ON DELETE CASCADE,
    CONSTRAINT fk_historial_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(usuario_id) ON DELETE RESTRICT
);

-- 10. TABLA DE PAGOS
CREATE TABLE pagos (
    pago_id SERIAL PRIMARY KEY,
    factura_id INT NOT NULL,
    usuario_id INT NOT NULL,
    monto_pagado DECIMAL(12, 2) NOT NULL,
    fecha_programada DATE,
    fecha_pago TIMESTAMP,
    metodo_pago VARCHAR(50), -- Ej: Transferencia, Cheque
    estado_pago VARCHAR(30) DEFAULT 'Programado' NOT NULL, -- Programado, Pagado, Anulado
    CONSTRAINT fk_pago_factura FOREIGN KEY (factura_id) REFERENCES facturas(factura_id) ON DELETE RESTRICT,
    CONSTRAINT fk_pago_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(usuario_id) ON DELETE RESTRICT,
    CONSTRAINT chk_monto_pagado CHECK (monto_pagado > 0)
);

-- ÍNDICES RECOMENDADOS PARA BÚSQUEDA Y JOINS
CREATE INDEX idx_facturas_proveedor ON facturas(proveedor_id);
CREATE INDEX idx_facturas_estado ON facturas(estado_factura);
CREATE INDEX idx_oc_proveedor ON ordenes_compra(proveedor_id);
CREATE INDEX idx_pagos_factura ON pagos(factura_id);