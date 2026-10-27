package pe.edu.utp.portalfacturas.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import pe.edu.utp.portalfacturas.util.UtilSeguridad;

@Service
public class DocumentoStorageService {
    private static final Logger log = LoggerFactory.getLogger(DocumentoStorageService.class);
    private final Path basePath;

    public DocumentoStorageService(@Value("${portal.archivos.ruta:uploads/facturas}") String rutaBase) {
        this.basePath = Path.of(rutaBase).toAbsolutePath().normalize();
    }

    public DocumentoGuardado guardar(Long facturaId, String tipoDocumento, MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new IllegalArgumentException("Debe adjuntar el archivo " + tipoDocumento + " de la factura.");
        }

        // Commons IO: descarta cualquier ruta que venga en el nombre ("../../x.pdf" -> "x.pdf")
        String original = FilenameUtils.getName(archivo.getOriginalFilename() == null ? "archivo" : archivo.getOriginalFilename());
        String extension = FilenameUtils.getExtension(original).toLowerCase(Locale.ROOT);
        String esperada = "PDF".equals(tipoDocumento) ? "pdf" : "xml";
        if (!esperada.equals(extension)) {
            log.warn("Adjunto rechazado por extensión: factura={}, tipo={}, nombre='{}'",
                facturaId, tipoDocumento, UtilSeguridad.limpiarParaLog(original));
            throw new IllegalArgumentException("El archivo " + tipoDocumento + " debe tener formato ." + esperada + ".");
        }

        // El nombre en disco nunca es el que envía el usuario
        String nombreSeguro = UUID.randomUUID() + "." + extension;
        Path carpeta = basePath.resolve(String.valueOf(facturaId)).normalize();
        Path destino = carpeta.resolve(nombreSeguro).normalize();
        if (!destino.startsWith(carpeta)) {
            throw new IllegalArgumentException("Nombre de archivo no válido.");
        }

        try {
            FileUtils.forceMkdir(carpeta.toFile());
            try (InputStream input = archivo.getInputStream()) {
                Files.copy(input, destino, StandardCopyOption.REPLACE_EXISTING);
            }
            log.debug("Adjunto {} guardado: factura={}, {} ({})", tipoDocumento, facturaId, nombreSeguro,
                FileUtils.byteCountToDisplaySize(archivo.getSize()));
            return new DocumentoGuardado(original, destino.toString(), archivo.getSize());
        } catch (IOException ex) {
            log.error("No se pudo guardar el adjunto {} de la factura {}", tipoDocumento, facturaId, ex);
            throw new IllegalStateException("No se pudo guardar el archivo " + tipoDocumento + ".", ex);
        }
    }

    public void eliminar(DocumentoGuardado documento) {
        if (documento == null) return;
        try {
            Files.deleteIfExists(Path.of(documento.rutaAlmacenamiento()));
        } catch (IOException ex) {
            // Limpieza best-effort; el error principal ya fue reportado.
            log.warn("No se pudo eliminar el archivo temporal {}", documento.rutaAlmacenamiento(), ex);
        }
    }

    public record DocumentoGuardado(String nombreOriginal, String rutaAlmacenamiento, long tamanioBytes) {}
}
