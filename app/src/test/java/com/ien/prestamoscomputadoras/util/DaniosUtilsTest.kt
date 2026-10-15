package com.ien.prestamoscomputadoras.util

import com.ien.prestamoscomputadoras.viewmodel.fechaAr
import com.ien.prestamoscomputadoras.viewmodel.filaHistorial
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DaniosUtilsTest {

    private val prestado = fechaAr(9, 30, 9)
    private val devueltoA = fechaAr(9, 30, 11)

    private fun activo(observaciones: String? = null) =
        filaHistorial(1, prestado, observacionesIniciales = observaciones)

    private fun devuelto(
        enciende: Boolean? = true,
        pantallaOk: Boolean? = true,
        cargador: Boolean? = true,
        observacionesDevolucion: String? = null,
        observacionesIniciales: String? = null,
    ) = filaHistorial(
        1, prestado, fechaDevolucion = devueltoA,
        observacionesIniciales = observacionesIniciales,
        enciende = enciende, pantallaOk = pantallaOk, cargador = cargador,
        observacionesDevolucion = observacionesDevolucion,
    )

    @Test
    fun `observacion al entregar solo con texto`() {
        assertFalse(tieneObservacionAlEntregar(activo()))
        assertFalse(tieneObservacionAlEntregar(activo("   ")))
        assertTrue(tieneObservacionAlEntregar(activo("Rayón en la tapa")))
    }

    @Test
    fun `observacion al entregar vale tambien para devueltos`() {
        assertTrue(tieneObservacionAlEntregar(devuelto(observacionesIniciales = "Rayón en la tapa")))
    }

    @Test
    fun `activo nunca tiene danio en la devolucion`() {
        assertFalse(tieneDanioEnDevolucion(activo("Rayón en la tapa")))
    }

    @Test
    fun `devuelto todo ok no tiene danio`() {
        assertFalse(tieneDanioEnDevolucion(devuelto()))
        assertFalse(tieneDanioEnDevolucion(devuelto(observacionesDevolucion = "  ")))
    }

    @Test
    fun `devuelto sin revision guardada no tiene danio`() {
        assertFalse(tieneDanioEnDevolucion(devuelto(enciende = null, pantallaOk = null, cargador = null)))
    }

    @Test
    fun `devuelto con algun check en problema tiene danio`() {
        assertTrue(tieneDanioEnDevolucion(devuelto(enciende = false)))
        assertTrue(tieneDanioEnDevolucion(devuelto(pantallaOk = false)))
        assertTrue(tieneDanioEnDevolucion(devuelto(cargador = false)))
        assertTrue(tieneDanioEnDevolucion(devuelto(observacionesDevolucion = "Tecla floja")))
    }

    @Test
    fun `observaciones iniciales no disparan danio en la devolucion`() {
        assertFalse(tieneDanioEnDevolucion(devuelto(observacionesIniciales = "Rayón en la tapa")))
    }
}
