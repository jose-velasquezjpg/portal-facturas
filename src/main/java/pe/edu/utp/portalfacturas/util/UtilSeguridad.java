package pe.edu.utp.portalfacturas.util;

import org.apache.commons.lang3.StringUtils;

/** Defensas reutilizables: datos sensibles en logs, inyección de logs y fórmulas en Excel. */
public final class UtilSeguridad {
    private UtilSeguridad() { }

    /** Oculta un dato sensible (RUC, correo, documento) mostrando solo los últimos 3 caracteres. CWE-532. */
    public static String enmascarar(String dato) {
        if (StringUtils.length(dato) <= 3) return "***";
        return StringUtils.leftPad(StringUtils.right(dato, 3), dato.length(), '*');
    }

    /** Evita la inyección de logs (CWE-117): elimina saltos de línea y tabulaciones. */
    public static String limpiarParaLog(String texto) {
        return StringUtils.trimToEmpty(texto).replaceAll("[\\r\\n\\t]", "_");
    }

    /** Evita que un texto se interprete como fórmula al abrirlo en Excel/CSV (CWE-1236). */
    public static String neutralizarFormula(String texto) {
        if (texto == null) return "";
        return StringUtils.startsWithAny(texto, "=", "+", "-", "@") ? "'" + texto : texto;
    }
}
