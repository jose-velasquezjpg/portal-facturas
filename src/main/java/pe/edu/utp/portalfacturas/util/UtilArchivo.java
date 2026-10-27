package pe.edu.utp.portalfacturas.util;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Nombres y rutas de archivo confiables (path traversal, CWE-22). */
public final class UtilArchivo {
    private static final Logger log = LoggerFactory.getLogger(UtilArchivo.class);
    private static final Pattern NOMBRE_VALIDO = Pattern.compile("^[A-Za-z0-9_-]{1,50}$");

    private UtilArchivo() { }

    /** Devuelve un nombre .xlsx seguro (lista blanca) o lanza IllegalArgumentException. */
    public static String nombreSeguro(String nombreSolicitado) {
        String soloNombre = FilenameUtils.getName(StringUtils.trimToEmpty(nombreSolicitado));
        String base = FilenameUtils.getBaseName(soloNombre);
        if (!NOMBRE_VALIDO.matcher(base).matches()) {
            log.warn("Nombre de archivo rechazado: '{}'", UtilSeguridad.limpiarParaLog(nombreSolicitado));
            throw new IllegalArgumentException("Nombre de archivo no permitido");
        }
        return base + ".xlsx";
    }

    /** Construye la ruta final garantizando que quede DENTRO de la carpeta base. */
    public static Path resolverEnCarpeta(String carpeta, String nombreSolicitado) throws IOException {
        Path base = Paths.get(carpeta).toAbsolutePath().normalize();
        FileUtils.forceMkdir(base.toFile());
        Path destino = base.resolve(nombreSeguro(nombreSolicitado)).normalize();
        if (!destino.startsWith(base)) {
            throw new SecurityException("Intento de escritura fuera de la carpeta permitida");
        }
        return destino;
    }
}
