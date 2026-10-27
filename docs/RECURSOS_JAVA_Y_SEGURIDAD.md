# Recursos Java aplicados al Portal de Facturas TAI LOY (Lab S10)

## Mapa de librerías → dónde se usan en el portal

| Librería | Uso en el portal | Archivo |
|---|---|---|
| Guava `ImmutableList` | Listados de facturas inmutables (ninguna capa puede alterarlos) | `FacturaService` |
| Guava `Multimaps.index` | Agrupar facturas por proveedor | `FacturaReporteService` |
| Guava `Lists.partition` | Lotes de pago para Tesorería | `FacturaReporteService` |
| Guava `Preconditions` | Validar moneda/total al leer Excel y tamaño de lote | `LectorExcel`, `FacturaReporteService` |
| Guava `Joiner`, `Stopwatch` | Proveedores incluidos y tiempo de generación del reporte | `FacturaReporteService` |
| Commons Lang3 `StringUtils`, `Validate` | Validar serie/número; validar filas del Excel | `FacturaService`, `LectorExcel`, `UtilSeguridad` |
| Commons IO `FilenameUtils`, `FileUtils` | Nombres/rutas seguras de adjuntos y del reporte | `UtilArchivo`, `DocumentoStorageService` |
| Apache POI | `GET /facturas/exportar` → `.xlsx` (hoja *Facturas* con fórmulas `SUMIF` por moneda + hoja *Resumen por proveedor*) y lectura de vuelta | `ExportadorExcel`, `LectorExcel` |
| Logback + SLF4J | Consola + `logs/portal-facturas.log` (rotación) + `logs/errores.log` (solo ERROR) | `logback-spring.xml` |

## Riesgos y defensas

| Riesgo | Defensa en el portal | CWE |
|---|---|---|
| Dependencias vulnerables | Versiones en `<properties>`, `mvn dependency:tree`, escaneo `mvn -Pseguridad verify` (OWASP Dependency-Check) | OWASP A06 |
| Path traversal | `FilenameUtils.getName` + lista blanca + `normalize()/startsWith`; adjuntos guardados con nombre `UUID` | CWE-22 |
| Inyección de logs | `UtilSeguridad.limpiarParaLog` en todo dato de usuario que se registra | CWE-117 |
| Datos sensibles en logs | `UtilSeguridad.enmascarar` (correo en recuperación de clave); no se registran claves ni tokens | CWE-532 |
| Excel no confiable | Máx. 5 MB y 10 000 filas, try-with-resources, fila inválida no detiene la lectura | CWE-409 |
| Fórmulas maliciosas | `UtilSeguridad.neutralizarFormula` en textos exportados | CWE-1236 |
| Mensajes de error detallados | Detalle solo en `log.error`; el usuario ve mensaje genérico (`server.error.include-*=never`) | CWE-209 |
| Secretos en el código | `application.properties` lee `DB_URL`, `DB_USER`, `DB_PASSWORD` del entorno | CWE-798 |

## Cómo comprobarlo

```
mvn dependency:tree          # árbol de dependencias (E1)
mvn test                     # utilitarios, Excel (ida y vuelta) y reporte
mvn spring-boot:run          # luego iniciar sesión y usar «Exportar Excel» en la bandeja
mvn -Pseguridad verify       # escaneo de dependencias (primera vez descarga la base NVD)
```

## Checklist de seguridad

- [x] Árbol de dependencias revisado y versiones controladas en `pom.xml`
- [ ] Quitar el valor por defecto de `DB_PASSWORD` en `application.properties` antes de producción
- [x] Logs sin datos personales completos ni claves
- [x] Datos validados y limpiados antes de usarlos en archivos o logs
- [x] Archivos con límite de tamaño y extensión permitida (adjuntos 10 MB; Excel 5 MB)
- [x] Errores técnicos al log, no al usuario
- [ ] Plan periódico de actualización (Dependabot / `-Pseguridad`)
