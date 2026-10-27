package pe.edu.utp.portalfacturas.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class UtilSeguridadTest {
    @Test void enmascaraDejandoUltimosTresCaracteres() { assertEquals("********789", UtilSeguridad.enmascarar("20123456789")); assertTrue(UtilSeguridad.enmascarar("20123456789").endsWith("789")); }
    @Test void datoCortoSeOcultaPorCompleto() { assertEquals("***", UtilSeguridad.enmascarar("ab")); assertEquals("***", UtilSeguridad.enmascarar(null)); }
    @Test void limpiaSaltosDeLineaParaEvitarInyeccionDeLogs() {
        String limpio = UtilSeguridad.limpiarParaLog("Juan\n2026-10-26 ERROR Acceso concedido\tadmin");
        assertFalse(limpio.contains("\n")); assertFalse(limpio.contains("\t"));
    }
    @Test void neutralizaFormulas() {
        assertEquals("'=CMD()", UtilSeguridad.neutralizarFormula("=CMD()"));
        assertEquals("'@SUM(A1)", UtilSeguridad.neutralizarFormula("@SUM(A1)"));
        assertEquals("Proveedor SAC", UtilSeguridad.neutralizarFormula("Proveedor SAC"));
        assertEquals("", UtilSeguridad.neutralizarFormula(null));
    }
}
