package pe.edu.utp.portalfacturas.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.utp.portalfacturas.dao.FacturaDAO;
import pe.edu.utp.portalfacturas.model.FacturaForm;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FacturaServiceTest {
    @Mock private FacturaDAO dao;
    private FacturaService service;
    @BeforeEach void preparar() { service = new FacturaService(dao); }

    private FacturaForm valida() {
        FacturaForm f = new FacturaForm();
        f.setProveedorId(10L); f.setOrdenCompraId(20L); f.setSerie("f001");
        f.setNumero("00001234"); f.setFechaEmision(LocalDate.of(2026, 10, 20));
        f.setMoneda("PEN"); f.setSubtotal(new BigDecimal("1000.00"));
        f.setIgv(new BigDecimal("180.00")); f.setTotal(new BigDecimal("1180.00"));
        return f;
    }
    @Test void registraFacturaValida() {
        FacturaForm f = valida(); when(dao.ordenPerteneceProveedor(20L,10L)).thenReturn(true);
        when(dao.existeDuplicada(10L,"F001","00001234")).thenReturn(false);
        when(dao.registrar(f,7L)).thenReturn(99L);
        assertEquals(99L, service.registrar(f,7L,null));
        assertEquals("F001",f.getSerie()); verify(dao).registrar(f,7L);
    }
    @Test void rechazaFacturaDuplicada() {
        FacturaForm f = valida(); when(dao.ordenPerteneceProveedor(20L,10L)).thenReturn(true);
        when(dao.existeDuplicada(10L,"F001","00001234")).thenReturn(true);
        assertThrows(ReglaNegocioException.class,()->service.registrar(f,7L,null));
        verify(dao,never()).registrar(any(),any());
    }
    @Test void rechazaCuandoTotalNoCoincide() {
        FacturaForm f = valida(); f.setTotal(new BigDecimal("1200.00"));
        assertThrows(ReglaNegocioException.class,()->service.registrar(f,7L,null));
        verify(dao,never()).ordenPerteneceProveedor(any(),any());
        verify(dao,never()).registrar(any(),any());
    }
    @Test void rechazaOrdenDeOtroProveedor() {
        FacturaForm f = valida(); when(dao.ordenPerteneceProveedor(20L,10L)).thenReturn(false);
        assertThrows(ReglaNegocioException.class,()->service.registrar(f,7L,null));
        verify(dao,never()).registrar(any(),any());
    }
    @Test void usuarioProveedorNoPuedeElegirOtroProveedor() {
        FacturaForm f = valida(); f.setProveedorId(99L);
        when(dao.ordenPerteneceProveedor(20L,10L)).thenReturn(true);
        when(dao.existeDuplicada(10L,"F001","00001234")).thenReturn(false);
        when(dao.registrar(f,7L)).thenReturn(100L);
        assertEquals(100L,service.registrar(f,7L,10L));
        assertEquals(10L,f.getProveedorId());
    }
    @Test void rechazaImporteNegativo() {
        FacturaForm f = valida(); f.setSubtotal(new BigDecimal("-1.00"));
        assertThrows(ReglaNegocioException.class,()->service.registrar(f,7L,null));
        verify(dao,never()).registrar(any(),any());
    }
}
