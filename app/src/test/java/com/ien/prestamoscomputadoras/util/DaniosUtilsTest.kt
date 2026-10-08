package com.ien.prestamoscomputadoras.util

import com.ien.prestamoscomputadoras.data.entity.EstadoComputadora
import com.ien.prestamoscomputadoras.data.entity.Prestamo
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DaniosUtilsTest {

    private fun prestamo(
        observaciones: String? = null,
        estado: String = Prestamo.ESTADO_ACTIVO,
    ) = Prestamo(
        idPrestamo = 1,
        idAlumno = 1,
        idComputadora = 1,
        idAdministradorPrestamo = 1,
        fechaPrestamo = 0,
        estado = estado,
        observacionesIniciales = observaciones,
    )

    private fun devuelto() = prestamo(estado = Prestamo.ESTADO_DEVUELTO)

    private fun revision(
        enciende: Boolean = true,
        pantallaOk: Boolean = true,
        cargador: Boolean = true,
        observaciones: String? = null,
    ) = EstadoComputadora(
        idPrestamo = 1,
        enciende = enciende,
        pantallaOk = pantallaOk,
        cargador = cargador,
        observaciones = observaciones,
        fechaRevision = 0,
    )

    @Test
    fun `observacion al entregar solo con texto`() {
        assertFalse(tieneObservacionAlEntregar(prestamo()))
        assertFalse(tieneObservacionAlEntregar(prestamo("   ")))
        assertTrue(tieneObservacionAlEntregar(prestamo("Rayón en la tapa")))
    }

    @Test
    fun `observacion al entregar vale tambien para devueltos`() {
        assertTrue(
            tieneObservacionAlEntregar(prestamo("Rayón en la tapa", estado = Prestamo.ESTADO_DEVUELTO)),
        )
    }

    @Test
    fun `activo nunca tiene danio en la devolucion`() {
        assertFalse(tieneDanioEnDevolucion(prestamo("Rayón en la tapa"), null))
    }

    @Test
    fun `devuelto todo ok no tiene danio`() {
        assertFalse(tieneDanioEnDevolucion(devuelto(), revision()))
        assertFalse(tieneDanioEnDevolucion(devuelto(), revision(observaciones = "  ")))
    }

    @Test
    fun `devuelto con algun check en problema tiene danio`() {
        assertTrue(tieneDanioEnDevolucion(devuelto(), revision(enciende = false)))
        assertTrue(tieneDanioEnDevolucion(devuelto(), revision(pantallaOk = false)))
        assertTrue(tieneDanioEnDevolucion(devuelto(), revision(cargador = false)))
        assertTrue(tieneDanioEnDevolucion(devuelto(), revision(observaciones = "Tecla floja")))
    }

    @Test
    fun `observaciones iniciales no disparan danio en la devolucion`() {
        val p = prestamo("Rayón en la tapa", estado = Prestamo.ESTADO_DEVUELTO)
        assertFalse(tieneDanioEnDevolucion(p, revision()))
    }
}
