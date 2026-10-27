package pe.edu.utp.portalfacturas.controller;

import jakarta.validation.Valid;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import pe.edu.utp.portalfacturas.model.FacturaForm;
import pe.edu.utp.portalfacturas.model.FacturaResumen;
import pe.edu.utp.portalfacturas.model.PortalUserDetails;
import pe.edu.utp.portalfacturas.service.FacturaReporteService;
import pe.edu.utp.portalfacturas.service.FacturaService;
import pe.edu.utp.portalfacturas.service.ReglaNegocioException;
import pe.edu.utp.portalfacturas.util.UtilArchivo;
import pe.edu.utp.portalfacturas.util.UtilSeguridad;

@Controller
public class FacturaController {
    private static final Logger log = LoggerFactory.getLogger(FacturaController.class);
    private static final MediaType XLSX =
        MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    private final FacturaService service;
    private final FacturaReporteService reporteService;

    public FacturaController(FacturaService service, FacturaReporteService reporteService) {
        this.service = service;
        this.reporteService = reporteService;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/recuperar-clave")
    public String recuperarClave(@RequestParam(required = false) String enviado, Model model) {
        if ("1".equals(enviado)) {
            model.addAttribute("mensaje", "Si el correo está registrado, recibirás las instrucciones para recuperar el acceso.");
        }
        return "recuperar-clave";
    }

    @PostMapping("/recuperar-clave")
    public String solicitarRecuperacion(@RequestParam String correo) {
        // Dato sensible: se registra enmascarado y sin saltos de línea (CWE-532 / CWE-117)
        log.info("Solicitud de recuperación de clave para {}", UtilSeguridad.enmascarar(UtilSeguridad.limpiarParaLog(correo)));
        // Para la demostración académica no se envía correo real.
        // La pantalla reproduce el flujo habitual de recuperación sin exponer si el usuario existe.
        return "redirect:/recuperar-clave?enviado=1";
    }

    @GetMapping("/error/403")
    public String forbidden() {
        return "error/403";
    }

    @GetMapping("/modulos/{modulo}")
    public String modulo(@org.springframework.web.bind.annotation.PathVariable String modulo, Model model, Authentication authentication) {
        PortalUserDetails usuario = usuario(authentication);
        model.addAttribute("usuario", usuario);
        model.addAttribute("modulo", modulo.toUpperCase());
        return "modulos/modulo";
    }

    @GetMapping({"/", "/facturas"})
    public String listar(Model model, Authentication authentication) {
        PortalUserDetails usuario = usuario(authentication);
        if ("PROVEEDOR".equals(usuario.getRol())) {
            if (usuario.getProveedorId() == null) {
                model.addAttribute("mensaje", "La cuenta de proveedor no tiene un proveedor asociado.");
                model.addAttribute("facturas", java.util.List.of());
            } else {
                model.addAttribute("facturas", service.listarPorProveedor(usuario.getProveedorId()));
            }
        } else {
            model.addAttribute("facturas", service.listarTodas());
        }
        model.addAttribute("usuario", usuario);
        return "facturas/lista";
    }

    /** Descarga el reporte Excel de las facturas que el usuario puede ver (el proveedor solo las suyas). */
    @GetMapping("/facturas/exportar")
    public ResponseEntity<byte[]> exportar(Authentication authentication) {
        PortalUserDetails usuario = usuario(authentication);
        List<FacturaResumen> facturas;
        if ("PROVEEDOR".equals(usuario.getRol())) {
            facturas = usuario.getProveedorId() == null ? List.of() : service.listarPorProveedor(usuario.getProveedorId());
        } else {
            facturas = service.listarTodas();
        }
        try {
            byte[] excel = reporteService.generarExcel(facturas);
            String nombre = UtilArchivo.nombreSeguro("reporte_facturas_" + LocalDate.now());
            log.info("Exportación Excel solicitada por usuarioId={} (rol {})", usuario.getUsuarioId(), usuario.getRol());
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(nombre).build().toString())
                .contentType(XLSX)
                .body(excel);
        } catch (IOException | RuntimeException ex) {
            // Detalle técnico solo al log; el usuario no recibe rutas ni clases internas (CWE-209)
            log.error("No se pudo generar el reporte Excel (usuarioId={})", usuario.getUsuarioId(), ex);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/facturas/nueva")
    public String nueva(Model model, Authentication authentication) {
        PortalUserDetails usuario = usuario(authentication);
        FacturaForm form = new FacturaForm();
        if ("PROVEEDOR".equals(usuario.getRol())) form.setProveedorId(usuario.getProveedorId());
        model.addAttribute("facturaForm", form);
        cargarCatalogos(model, usuario);
        return "facturas/formulario";
    }

    @PostMapping("/facturas/guardar")
    public String guardar(@Valid @ModelAttribute("facturaForm") FacturaForm form,
                          BindingResult binding,
                          @RequestParam("archivoXml") MultipartFile archivoXml,
                          @RequestParam("archivoPdf") MultipartFile archivoPdf,
                          Model model, Authentication authentication) {
        PortalUserDetails usuario = usuario(authentication);
        if (binding.hasErrors()) {
            cargarCatalogos(model, usuario);
            return "facturas/formulario";
        }
        try {
            Long proveedorForzado = "PROVEEDOR".equals(usuario.getRol()) ? usuario.getProveedorId() : null;
            service.registrar(form, usuario.getUsuarioId(), proveedorForzado, archivoXml, archivoPdf);
            return "redirect:/facturas?registrada";
        } catch (ReglaNegocioException ex) {
            model.addAttribute("errorNegocio", ex.getMessage());
            cargarCatalogos(model, usuario);
            return "facturas/formulario";
        } catch (RuntimeException ex) {
            log.error("Error inesperado al registrar factura (usuarioId={})", usuario.getUsuarioId(), ex);
            model.addAttribute("errorNegocio", "No fue posible registrar la factura. Verifique la información o contacte al administrador.");
            cargarCatalogos(model, usuario);
            return "facturas/formulario";
        }
    }

    private void cargarCatalogos(Model model, PortalUserDetails usuario) {
        boolean proveedor = "PROVEEDOR".equals(usuario.getRol());
        model.addAttribute("rol", usuario.getRol());
        model.addAttribute("proveedorFijo", usuario.getProveedorId());
        model.addAttribute("proveedores", service.listarProveedores());
        model.addAttribute("ordenes", service.listarOrdenesCompra(proveedor ? usuario.getProveedorId() : null));
    }

    private PortalUserDetails usuario(Authentication authentication) {
        Object principal = authentication == null ? null : authentication.getPrincipal();
        if (principal instanceof PortalUserDetails details) return details;
        throw new IllegalStateException("No se encontró el usuario autenticado del portal");
    }
}
