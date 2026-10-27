# Portal de Facturas - TAI LOY

Portal web para la gestión de facturas de proveedores.
Curso Integrador I - UTP.

## Requisitos
- Java 21
- Maven 3.9+
- PostgreSQL

## Base de datos
Ejecutar los scripts de la carpeta `sql/` en este orden:
1. `00_configuracion_demo.sql`
2. `BD_Portal_Facturas.sql`
3. `roles_minimos.sql`
4. `datos_demo_proveedor_oc.sql`
5. `usuarios_demo.sql`
6. `04_permisos_portal_app.sql`

## Ejecución
    mvn spring-boot:run

En Windows también se puede usar `iniciar_portal.bat`.