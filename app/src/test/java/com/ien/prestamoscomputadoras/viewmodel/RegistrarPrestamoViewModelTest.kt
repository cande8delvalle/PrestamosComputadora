package com.ien.prestamoscomputadoras.viewmodel

import com.ien.prestamoscomputadoras.data.entity.Alumno
import com.ien.prestamoscomputadoras.data.entity.Computadora
import com.ien.prestamoscomputadoras.data.entity.Prestamo
import com.ien.prestamoscomputadoras.data.repository.PrestamoRepository
import com.ien.prestamoscomputadoras.util.SesionActual
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class RegistrarPrestamoViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val maria = Alumno(idAlumno = 1, nombre = "María", apellido = "Gonzales", dni = "46111222")
    private val pc07 = Computadora(idComputadora = 7, codigo = "PC-07")
    private val pc12 = Computadora(idComputadora = 12, codigo = "PC-12")

    private val alumnoDao = FakeAlumnoDao(mutableListOf(maria))
    private val prestamoYComputadoraDao = FakePrestamoYComputadoraDao(listOf(pc07, pc12))
    private var reloj = 1_000L

    private fun nuevoVm() = RegistrarPrestamoViewModel(
        PrestamoRepository(prestamoYComputadoraDao, prestamoYComputadoraDao, alumnoDao),
        ahora = { reloj },
    )

    @Before
    fun setUp() {
        SesionActual.administradorId = 5
    }

    @After
    fun tearDown() {
        SesionActual.administradorId = null
    }

    @Test
    fun alIniciar_cargaAlumnosYComputadoras() {
        val s = nuevoVm().uiState
        assertEquals(listOf(maria), s.alumnos)
        assertEquals(listOf(pc07, pc12), s.computadorasDisponibles)
        assertFalse(s.cargandoListas)
    }

    @Test
    fun laFechaDelPrestamoEsLaDelMomentoDeGuardar_noLaDeAbrirLaPantalla() {
        val vm = nuevoVm()
        reloj = 9_000L
        vm.apply {
            onAlumnoSeleccionado(maria)
            onComputadoraSeleccionada(pc07)
            onRegistrarClick()
        }
        assertEquals(9_000L, prestamoYComputadoraDao.prestamos.single().fechaPrestamo)
    }

    @Test
    fun sinSeleccion_muestraAmbosErroresYNoGuarda() {
        val vm = nuevoVm()
        vm.onRegistrarClick()

        assertEquals(ERROR_SELECCIONAR_ALUMNO, vm.uiState.errorAlumno)
        assertEquals(ERROR_SELECCIONAR_COMPUTADORA, vm.uiState.errorComputadora)
        assertTrue(prestamoYComputadoraDao.prestamos.isEmpty())
    }

    @Test
    fun sinSesion_muestraErrorYNoGuarda() {
        SesionActual.administradorId = null
        val vm = nuevoVm().apply {
            onAlumnoSeleccionado(maria)
            onComputadoraSeleccionada(pc07)
            onRegistrarClick()
        }

        assertEquals(ERROR_SIN_SESION, vm.uiState.errorGeneral)
        assertTrue(prestamoYComputadoraDao.prestamos.isEmpty())
    }

    @Test
    fun registroValido_guardaPrestamoActivoYMuestraModal() {
        val vm = nuevoVm().apply {
            onAlumnoSeleccionado(maria)
            onComputadoraSeleccionada(pc07)
            onObservacionesChange("  Rayón en la tapa  ")
            onRegistrarClick()
        }

        val p = prestamoYComputadoraDao.prestamos.single()
        assertEquals(1L, p.idAlumno)
        assertEquals(7L, p.idComputadora)
        assertEquals(5L, p.idAdministrador)
        assertEquals(1_000L, p.fechaPrestamo)
        assertNull(p.fechaDevolucion)
        assertEquals(Prestamo.ESTADO_ACTIVO, p.estado)
        assertEquals("Rayón en la tapa", p.observacionesIniciales)
        assertTrue(vm.uiState.mostrarModalExito)
        assertFalse(vm.uiState.isLoading)
    }

    @Test
    fun observacionesVacias_seGuardanComoNull() {
        nuevoVm().apply {
            onAlumnoSeleccionado(maria)
            onComputadoraSeleccionada(pc07)
            onObservacionesChange("   ")
            onRegistrarClick()
        }
        assertNull(prestamoYComputadoraDao.prestamos.single().observacionesIniciales)
    }

    @Test
    fun realizarOtro_limpiaYSacaLaComputadoraPrestada() {
        val vm = nuevoVm().apply {
            onAlumnoSeleccionado(maria)
            onComputadoraSeleccionada(pc07)
            onObservacionesChange("ok")
            onRegistrarClick()
        }
        vm.onRealizarOtroClick()

        val s = vm.uiState
        assertFalse(s.mostrarModalExito)
        assertNull(s.alumnoSeleccionado)
        assertNull(s.computadoraSeleccionada)
        assertEquals("", s.observacionesIniciales)
        assertEquals(listOf(pc12), s.computadorasDisponibles)
    }

    @Test
    fun sinComputadorasDisponibles_noRegistra() {
        val vm = RegistrarPrestamoViewModel(
            PrestamoRepository(
                FakePrestamoYComputadoraDao(emptyList()),
                FakePrestamoYComputadoraDao(emptyList()),
                alumnoDao,
            ),
        )
        assertTrue(vm.uiState.sinComputadorasDisponibles)
        vm.onAlumnoSeleccionado(maria)
        vm.onRegistrarClick()
        assertNull(vm.uiState.errorComputadora)
    }

    @Test
    fun formatearDni_agregaPuntosSoloParaMostrar() {
        assertEquals("46.111.222", formatearDni("46111222"))
        assertEquals("5.111.222", formatearDni("5111222"))
        assertEquals("123", formatearDni("123"))
    }
}
