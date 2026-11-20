package com.ien.prestamoscomputadoras.viewmodel

import com.ien.prestamoscomputadoras.data.model.Alumno
import com.ien.prestamoscomputadoras.data.model.Aula
import com.ien.prestamoscomputadoras.data.model.Computadora
import com.ien.prestamoscomputadoras.data.model.NuevoPrestamo
import com.ien.prestamoscomputadoras.data.model.Prestamo
import com.ien.prestamoscomputadoras.data.remote.ErrorDatos
import com.ien.prestamoscomputadoras.data.remote.MENSAJE_SIN_CONEXION
import com.ien.prestamoscomputadoras.util.SesionActual
import kotlinx.coroutines.test.runTest
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

    private val aula1 = Aula(idAula = 1, nombre = "Aula 1")
    private val lab = Aula(idAula = 2, nombre = "Laboratorio")

    private val repo = FakePrestamos(computadoras = listOf(pc07, pc12), alumnos = listOf(maria))
    private val aulaRepo = FakeAulaRepository(listOf(lab, aula1))

    private fun nuevoVm() = RegistrarPrestamoViewModel(repo, aulaRepo)

    @Before
    fun setUp() {
        SesionActual.usuarioId = 5
    }

    @After
    fun tearDown() {
        SesionActual.usuarioId = null
    }

    @Test
    fun alIniciar_cargaAlumnosYComputadoras() {
        val s = nuevoVm().uiState
        assertEquals(listOf(maria), s.alumnos)
        assertEquals(listOf(pc07, pc12), s.computadorasDisponibles)
        assertFalse(s.cargandoListas)
    }

    @Test
    fun laFechaDelPrestamoLaPoneElServidor() {
        val vm = nuevoVm()
        repo.ahoraServidor = fechaAr(9, 30, 15)
        vm.apply {
            onAlumnoSeleccionado(maria)
            onComputadoraSeleccionada(pc07)
            onAulaSeleccionada(aula1)
            onRegistrarClick()
        }
        assertEquals(fechaAr(9, 30, 15), repo.prestamos.single().fechaPrestamo)
    }

    @Test
    fun sinSeleccion_muestraAmbosErroresYNoGuarda() {
        val vm = nuevoVm()
        vm.onRegistrarClick()

        assertEquals(ERROR_SELECCIONAR_ALUMNO, vm.uiState.errorAlumno)
        assertEquals(ERROR_SELECCIONAR_COMPUTADORA, vm.uiState.errorComputadora)
        assertTrue(repo.prestamos.isEmpty())
    }

    @Test
    fun sinSesion_muestraErrorYNoGuarda() {
        SesionActual.usuarioId = null
        val vm = nuevoVm().apply {
            onAlumnoSeleccionado(maria)
            onComputadoraSeleccionada(pc07)
            onAulaSeleccionada(aula1)
            onRegistrarClick()
        }

        assertEquals(ERROR_SIN_SESION, vm.uiState.errorGeneral)
        assertTrue(repo.prestamos.isEmpty())
    }

    @Test
    fun registroValido_guardaPrestamoActivoYMuestraModal() {
        val vm = nuevoVm().apply {
            onAlumnoSeleccionado(maria)
            onComputadoraSeleccionada(pc07)
            onAulaSeleccionada(aula1)
            onObservacionesChange("  Rayón en la tapa  ")
            onRegistrarClick()
        }

        val p = repo.prestamos.single()
        assertEquals(1L, p.idAlumno)
        assertEquals(7L, p.idComputadora)
        assertEquals(5L, p.idUsuarioPrestamo)
        assertNull(p.idUsuarioDevolucion)
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
            onAulaSeleccionada(aula1)
            onObservacionesChange("   ")
            onRegistrarClick()
        }
        assertNull(repo.prestamos.single().observacionesIniciales)
    }

    @Test
    fun computadoraPrestadaDesdeOtroCelular_mensajeAmigableYSeRecarganLasListas() = runTest {
        val vm = nuevoVm().apply {
            onAlumnoSeleccionado(maria)
            onComputadoraSeleccionada(pc07)
            onAulaSeleccionada(aula1)
        }
        // Mientras tanto, otro celular presta la PC-07.
        repo.insertar(NuevoPrestamo(idAlumno = 1, idComputadora = 7, idUsuarioPrestamo = 9, idAula = 1))

        vm.onRegistrarClick()

        assertEquals(avisoComputadoraYaPrestada("PC-07"), vm.uiState.aviso)
        assertEquals("Esa computadora ya está prestada", vm.uiState.aviso?.titulo)
        assertNull(vm.uiState.computadoraSeleccionada)
        assertEquals(listOf(pc12), vm.uiState.computadorasDisponibles)
        assertFalse(vm.uiState.mostrarModalExito)
        assertEquals(1, repo.prestamos.size)

        vm.onAvisoAceptado()
        assertNull(vm.uiState.aviso)
    }

    @Test
    fun sinInternetAlCargar_muestraErrorYNoDiceQueNoHayComputadoras() {
        repo.falla = ErrorDatos.SinConexion()
        val vm = nuevoVm()

        assertEquals(MENSAJE_SIN_CONEXION, vm.uiState.errorGeneral)
        assertTrue(vm.uiState.errorListas)
        assertFalse(vm.uiState.sinComputadorasDisponibles)

        repo.falla = null
        vm.cargarListas()
        assertNull(vm.uiState.errorGeneral)
        assertEquals(listOf(pc07, pc12), vm.uiState.computadorasDisponibles)
    }

    @Test
    fun realizarOtro_limpiaYSacaLaComputadoraPrestada() {
        val vm = nuevoVm().apply {
            onAlumnoSeleccionado(maria)
            onComputadoraSeleccionada(pc07)
            onAulaSeleccionada(aula1)
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
        val vm = RegistrarPrestamoViewModel(FakePrestamos(alumnos = listOf(maria)), aulaRepo)
        assertTrue(vm.uiState.sinComputadorasDisponibles)
        vm.onAlumnoSeleccionado(maria)
        vm.onRegistrarClick()
        assertNull(vm.uiState.errorComputadora)
    }

    // --- Aula (obligatoria) --------------------------------------------------------------

    @Test
    fun alIniciar_cargaLasAulasOrdenadasPorNombre() {
        val s = nuevoVm().uiState
        assertEquals(listOf(aula1, lab), s.aulas)
        assertFalse(s.sinAulas)
    }

    @Test
    fun sinAula_muestraErrorYNoGuarda() {
        val vm = nuevoVm().apply {
            onAlumnoSeleccionado(maria)
            onComputadoraSeleccionada(pc07)
            onRegistrarClick()
        }

        assertEquals("Debés seleccionar un aula", ERROR_SELECCIONAR_AULA)
        assertEquals(ERROR_SELECCIONAR_AULA, vm.uiState.errorAula)
        assertNull(vm.uiState.errorAlumno)
        assertNull(vm.uiState.errorComputadora)
        assertTrue(repo.prestamos.isEmpty())
        assertFalse(vm.uiState.mostrarModalExito)
    }

    @Test
    fun sinNingunaSeleccion_muestraLosTresErrores() {
        val vm = nuevoVm()
        vm.onRegistrarClick()

        assertEquals(ERROR_SELECCIONAR_ALUMNO, vm.uiState.errorAlumno)
        assertEquals(ERROR_SELECCIONAR_COMPUTADORA, vm.uiState.errorComputadora)
        assertEquals(ERROR_SELECCIONAR_AULA, vm.uiState.errorAula)
        assertTrue(repo.prestamos.isEmpty())
    }

    @Test
    fun elegirAula_limpiaSuError() {
        val vm = nuevoVm()
        vm.onRegistrarClick()
        vm.onAulaSeleccionada(lab)

        assertNull(vm.uiState.errorAula)
        assertEquals(lab, vm.uiState.aulaSeleccionada)
    }

    @Test
    fun registroValido_mandaElIdDelAulaElegida() {
        nuevoVm().apply {
            onAlumnoSeleccionado(maria)
            onComputadoraSeleccionada(pc07)
            onAulaSeleccionada(lab)
            onRegistrarClick()
        }
        assertEquals(2L, repo.prestamos.single().idAula)
    }

    @Test
    fun sinAulasRegistradas_noRegistraYAvisa() {
        val vm = RegistrarPrestamoViewModel(repo, FakeAulaRepository())
        assertTrue(vm.uiState.sinAulas)

        vm.onAlumnoSeleccionado(maria)
        vm.onComputadoraSeleccionada(pc07)
        vm.onRegistrarClick()

        assertTrue(repo.prestamos.isEmpty())
        assertFalse(vm.uiState.mostrarModalExito)
    }

    @Test
    fun sinInternetAlCargarAulas_muestraErrorYNoDiceQueNoHayAulas() {
        aulaRepo.falla = ErrorDatos.SinConexion()
        val vm = nuevoVm()

        assertEquals(MENSAJE_SIN_CONEXION, vm.uiState.errorGeneral)
        assertTrue(vm.uiState.errorListas)
        assertFalse(vm.uiState.sinAulas)

        aulaRepo.falla = null
        vm.cargarListas()
        assertNull(vm.uiState.errorGeneral)
        assertEquals(listOf(aula1, lab), vm.uiState.aulas)
    }

    @Test
    fun realizarOtro_limpiaElAulaElegida() {
        val vm = nuevoVm().apply {
            onAlumnoSeleccionado(maria)
            onComputadoraSeleccionada(pc07)
            onAulaSeleccionada(lab)
            onRegistrarClick()
        }
        vm.onRealizarOtroClick()

        assertNull(vm.uiState.aulaSeleccionada)
        assertEquals(listOf(aula1, lab), vm.uiState.aulas)
    }

    @Test
    fun formatearDni_agregaPuntosSoloParaMostrar() {
        assertEquals("46.111.222", formatearDni("46111222"))
        assertEquals("5.111.222", formatearDni("5111222"))
        assertEquals("123", formatearDni("123"))
    }
}
