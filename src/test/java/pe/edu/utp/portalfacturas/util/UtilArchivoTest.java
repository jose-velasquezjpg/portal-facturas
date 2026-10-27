package pe.edu.utp.portalfacturas.util;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class UtilArchivoTest {
    @TempDir Path temporal;

    @Test void aceptaNombreValido() { assertEquals("reporte_facturas.xlsx", UtilArchivo.nombreSeguro("reporte_facturas")); }
    @Test void neutralizaRecorridoDeRutas() { assertEquals("passwd.xlsx", UtilArchivo.nombreSeguro("../../etc/passwd")); }
    @Test void rechazaCaracteresPeligrosos() { assertThrows(IllegalArgumentException.class, () -> UtilArchivo.nombreSeguro("reporte<script>")); }
    @Test void rechazaNombreVacio() { assertThrows(IllegalArgumentException.class, () -> UtilArchivo.nombreSeguro("")); }
    @Test void laRutaFinalQuedaDentroDeLaCarpetaBase() throws IOException {
        Path ruta = UtilArchivo.resolverEnCarpeta(temporal.resolve("salida").toString(), "../../x");
        assertTrue(ruta.startsWith(temporal.resolve("salida").toAbsolutePath().normalize()));
    }
}
