package com.ien.prestamoscomputadoras.viewmodel

import com.ien.prestamoscomputadoras.data.entity.Alumno
import com.ien.prestamoscomputadoras.data.entity.Computadora
import com.ien.prestamoscomputadoras.data.entity.Prestamo
import com.ien.prestamoscomputadoras.data.repository.DevolucionRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class RegistrarDevolucionViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val maria = Alumno(idAlumno = 1, nombre = "María", apellido = "Gonzales", dni = "46111222")
    private val pc12 = Computadora(idComputadora = 12, codigo = "PC-12")
    private val pc07 = Computadora(idComputadora = 7, codigo = "PC-07")

    private val alumnoDao = FakeAlumnoDao(mutableListOf(maria))
    private val prestamoDao = FakePrestamoYComputadoraDao(listOf(pc07, pc12))
    private val estadoDao = FakeEstadoComputadoraDao()
    private var reloj = 50_000L

    private fun nuevoVm() = RegistrarDevolucionViewModel(
        DevolucionRepository(prestamoDao, alumnoDao, estadoDao, enTransaccion = { it() }),
        ahora = { reloj },
    )

    @Before
    fun setUp() = runTest {
        prestamoDao.insertar(
            Prestamo(idAlumno = 1, idComputadora = 12, idAdministrador = 1, fechaPrestamo = 10_000L),
        )
    }

    @Test
    fun qrConPrestamoActivo_pasaAConfirmarConLosDatosYChecklistEnTrue() {
        val vm = nuevoVm()
        vm.onCodigoEscaneado(" PC-12 ")

        val s = vm.uiState
        assertEquals(EtapaDevolucion.CONFIRMANDO, s.etapaActual)
        val p = s.prestamoEncontrado!!
        assertEquals(1L, p.idPrestamo)
        assertEquals("María Gonzales", p.nombreAlumno)
        assertEquals("46111222", p.dniAlumno)
        assertEquals("PC-12", p.codigoComputadora)
        assertEquals(10_000L, p.fechaPrestamo)
        assertTrue(s.enciende && s.pantallaOk && s.incluyeCargador)
        assertFalse(s.buscando)
    }

    @Test
    fun qrSinPrestamoActivo_muestraErrorYSigueEscaneando() {
        listOf("PC-07", "PC-99").forEach { codigo ->
            val vm = nuevoVm()
            vm.onCodigoEscaneado(codigo)
            assertEquals(EtapaDevolucion.ESCANEANDO, vm.uiState.etapaActual)
            assertEquals(ERROR_SIN_PRESTAMO_ACTIVO, vm.uiState.errorEscaneo)
        }
    }

    @Test
    fun mismoCodigoRechazado_seIgnoraUnosSegundos() {
        val vm = nuevoVm()
        vm.onCodigoEscaneado("PC-07")
        vm.onErrorEscaneoMostrado()

        reloj += 1_000
        vm.onCodigoEscaneado("PC-07")
        assertNull(vm.uiState.errorEscaneo)

        reloj += 3_000
        vm.onCodigoEscaneado("PC-07")
        assertEquals(ERROR_SIN_PRESTAMO_ACTIVO, vm.uiState.errorEscaneo)
    }

    @Test
    fun confirmar_guardaRevisionYMarcaDevuelto() {
        val vm = nuevoVm().apply {
            onCodigoEscaneado("PC-12")
            onIncluyeCargadorChange(false)
            onObservacionesChange("  Falta el cargador  ")
        }
        reloj = 90_000L
        vm.onConfirmarClick()

        val estado = estadoDao.estados.single()
        assertEquals(1L, estado.idPrestamo)
        assertTrue(estado.enciende)
        assertTrue(estado.pantallaOk)
        assertFalse(estado.cargador)
        assertEquals("Falta el cargador", estado.observaciones)
        assertEquals(90_000L, estado.fechaRevision)

        val prestamo = prestamoDao.prestamos.single()
        assertEquals(Prestamo.ESTADO_DEVUELTO, prestamo.estado)
        assertEquals(90_000L, prestamo.fechaDevolucion)

        assertTrue(vm.uiState.mostrarModalExito)
        assertFalse(vm.uiState.isLoading)
    }

    @Test
    fun prestamoYaDevuelto_noGuardaRevisionYMuestraError() = runTest {
        val vm = nuevoVm().apply { onCodigoEscaneado("PC-12") }
        prestamoDao.actualizarDevolucion(1, 60_000L)

        vm.onConfirmarClick()

        assertTrue(estadoDao.estados.isEmpty())
        assertEquals(ERROR_PRESTAMO_YA_DEVUELTO, vm.uiState.errorConfirmacion)
        assertFalse(vm.uiState.mostrarModalExito)
    }

    @Test
    fun volverAEscanear_descartaTodoSinGuardar() {
        val vm = nuevoVm().apply {
            onCodigoEscaneado("PC-12")
            onEnciendeChange(false)
            volverAEscanear()
        }

        assertEquals(RegistrarDevolucionUiState(), vm.uiState)
        assertTrue(estadoDao.estados.isEmpty())
        assertEquals(Prestamo.ESTADO_ACTIVO, prestamoDao.prestamos.single().estado)
    }

    @Test
    fun despuesDeDevolver_elMismoQrYaNoTienePrestamoActivo() {
        val vm = nuevoVm().apply {
            onCodigoEscaneado("PC-12")
            onConfirmarClick()
            volverAEscanear()
            onCodigoEscaneado("PC-12")
        }
        assertEquals(EtapaDevolucion.ESCANEANDO, vm.uiState.etapaActual)
        assertEquals(ERROR_SIN_PRESTAMO_ACTIVO, vm.uiState.errorEscaneo)
    }

    @Test
    fun formatoFechaDevolucion() {
        val texto = formatearFechaHoraDevolucion(0L)
        assertTrue(texto, Regex("""\d{2}/\d{2}/\d{4} · \d{2}:\d{2} hs""").matches(texto))
    }
}
