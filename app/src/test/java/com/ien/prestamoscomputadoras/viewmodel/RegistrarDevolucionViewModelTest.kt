package com.ien.prestamoscomputadoras.viewmodel

import com.ien.prestamoscomputadoras.data.model.Alumno
import com.ien.prestamoscomputadoras.data.model.Computadora
import com.ien.prestamoscomputadoras.data.model.NuevoPrestamo
import com.ien.prestamoscomputadoras.data.model.Prestamo
import com.ien.prestamoscomputadoras.data.remote.ErrorDatos
import com.ien.prestamoscomputadoras.data.remote.MENSAJE_SIN_CONEXION
import com.ien.prestamoscomputadoras.util.SesionActual
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.After
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

    private val repo = FakePrestamos(
        computadoras = listOf(pc07, pc12),
        alumnos = listOf(maria),
        ahoraServidor = fechaAr(9, 30, 8),
        perfilLogueado = 3,
    )
    private var reloj = 50_000L

    private fun nuevoVm() = RegistrarDevolucionViewModel(repo, ahora = { reloj })

    @Before
    fun setUp() = runTest {
        SesionActual.usuarioId = 3
        repo.insertar(NuevoPrestamo(idAlumno = 1, idComputadora = 12, idUsuarioPrestamo = 1))
    }

    @After
    fun tearDown() {
        SesionActual.usuarioId = null
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
        assertEquals(fechaAr(9, 30, 8), p.fechaPrestamo)
        assertTrue(s.enciende && s.pantallaOk && s.incluyeCargador)
        assertFalse(s.buscando)
    }

    @Test
    fun qrDePrestamoSinObservaciones_noTraeObservacion() {
        val vm = nuevoVm()
        vm.onCodigoEscaneado("PC-12")
        assertNull(vm.uiState.prestamoEncontrado!!.observacionesIniciales)
    }

    @Test
    fun qrDePrestamoConObservacionAlEntregar_laMuestraCompleta() = runTest {
        repo.insertar(
            NuevoPrestamo(
                idAlumno = 1, idComputadora = 7, idUsuarioPrestamo = 1,
                observacionesIniciales = "Rayón en la tapa y tecla F5 floja.",
            ),
        )
        val vm = nuevoVm()
        vm.onCodigoEscaneado("PC-07")
        assertEquals("Rayón en la tapa y tecla F5 floja.", vm.uiState.prestamoEncontrado!!.observacionesIniciales)
    }

    @Test
    fun observacionSoloConEspacios_noSeMuestra() = runTest {
        repo.insertar(NuevoPrestamo(idAlumno = 1, idComputadora = 7, idUsuarioPrestamo = 1, observacionesIniciales = "   "))
        val vm = nuevoVm()
        vm.onCodigoEscaneado("PC-07")
        assertNull(vm.uiState.prestamoEncontrado!!.observacionesIniciales)
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
    fun sinInternetAlEscanear_avisaYSigueEscaneando() {
        repo.falla = ErrorDatos.SinConexion()
        val vm = nuevoVm()
        vm.onCodigoEscaneado("PC-12")
        assertEquals(EtapaDevolucion.ESCANEANDO, vm.uiState.etapaActual)
        assertEquals(MENSAJE_SIN_CONEXION, vm.uiState.errorEscaneo)
        assertFalse(vm.uiState.buscando)
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
    fun confirmar_guardaRevisionYElTriggerCompletaFechaYQuienRecibio() {
        val vm = nuevoVm().apply {
            onCodigoEscaneado("PC-12")
            onIncluyeCargadorChange(false)
            onObservacionesChange("  Falta el cargador  ")
        }
        repo.ahoraServidor = fechaAr(9, 30, 11)
        vm.onConfirmarClick()

        val estado = repo.revisiones.single()
        assertEquals(1L, estado.idPrestamo)
        assertTrue(estado.enciende)
        assertTrue(estado.pantallaOk)
        assertFalse(estado.cargador)
        assertEquals("Falta el cargador", estado.observaciones)

        val prestamo = repo.prestamos.single()
        assertEquals(Prestamo.ESTADO_DEVUELTO, prestamo.estado)
        assertEquals(fechaAr(9, 30, 11), prestamo.fechaDevolucion)
        // Quien entregó no cambia; quien recibió es el usuario logueado.
        assertEquals(1L, prestamo.idUsuarioPrestamo)
        assertEquals(3L, prestamo.idUsuarioDevolucion)

        assertTrue(vm.uiState.mostrarModalExito)
        assertFalse(vm.uiState.isLoading)
    }

    @Test
    fun sinSesion_noGuardaYMuestraError() {
        val vm = nuevoVm().apply { onCodigoEscaneado("PC-12") }
        SesionActual.usuarioId = null

        vm.onConfirmarClick()

        assertTrue(repo.revisiones.isEmpty())
        assertEquals(Prestamo.ESTADO_ACTIVO, repo.prestamos.single().estado)
        assertEquals(ERROR_SIN_SESION, vm.uiState.errorConfirmacion)
        assertFalse(vm.uiState.mostrarModalExito)
    }

    @Test
    fun prestamoYaDevueltoDesdeOtroCelular_noGuardaRevisionYMuestraError() {
        val vm = nuevoVm().apply { onCodigoEscaneado("PC-12") }
        repo.devolverDirecto(1, quien = 2)

        vm.onConfirmarClick()

        assertTrue(repo.revisiones.isEmpty())
        assertEquals(ERROR_PRESTAMO_YA_DEVUELTO, vm.uiState.errorConfirmacion)
        assertFalse(vm.uiState.mostrarModalExito)
    }

    @Test
    fun sinInternetAlConfirmar_muestraMensajeClaro() {
        val vm = nuevoVm().apply { onCodigoEscaneado("PC-12") }
        repo.falla = ErrorDatos.SinConexion()

        vm.onConfirmarClick()

        assertEquals(MENSAJE_SIN_CONEXION, vm.uiState.errorConfirmacion)
        assertFalse(vm.uiState.isLoading)
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
        assertTrue(repo.revisiones.isEmpty())
        assertEquals(Prestamo.ESTADO_ACTIVO, repo.prestamos.single().estado)
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
    fun formatoFechaDevolucion_enHoraArgentina() {
        // 30/09/2026 20:46 UTC = 17:46 en Argentina.
        assertEquals("30/09/2026 · 17:46 hs", formatearFechaHoraDevolucion(Instant.parse("2026-09-30T20:46:00Z")))
    }
}
