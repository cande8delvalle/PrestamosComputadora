package com.ien.prestamoscomputadoras.viewmodel

import com.ien.prestamoscomputadoras.data.remote.ErrorDatos
import com.ien.prestamoscomputadoras.data.remote.MENSAJE_CODIGO_INVALIDO
import com.ien.prestamoscomputadoras.data.remote.MENSAJE_DEMASIADOS_INTENTOS
import com.ien.prestamoscomputadoras.data.remote.MENSAJE_SIN_CONEXION
import com.ien.prestamoscomputadoras.util.SesionActual
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RecuperarContrasenaViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repo = FakeRecuperacionRepository()

    @After
    fun tearDown() {
        SesionActual.consumirAvisoContrasenaActualizada()
        SesionActual.usuarioId = null
    }

    /** Por defecto el `reloj` nunca avanza: el contador se queda en 60 s. */
    private fun vm(reloj: suspend () -> Unit = { awaitCancellation() }) =
        RecuperarContrasenaViewModel(repo, reloj, scopeDescarte = CoroutineScope(Dispatchers.Main))

    private fun RecuperarContrasenaViewModel.pedirCodigo(email: String = "ana@ien.edu.ar"): Boolean {
        var enviado = false
        onEmailChange(email)
        onContinuarCorreo { enviado = true }
        return enviado
    }

    private fun RecuperarContrasenaViewModel.verificar(codigo: String = "123456"): Boolean {
        var ok = false
        onCodigoChange(codigo)
        onContinuarCodigo { ok = true }
        return ok
    }

    private fun RecuperarContrasenaViewModel.guardar(nueva: String, repetir: String): Boolean {
        var ok = false
        onNuevaContrasenaChange(nueva)
        onRepetirContrasenaChange(repetir)
        onGuardarContrasena { ok = true }
        return ok
    }

    // ---- Pantalla 1: correo

    @Test
    fun correoVacio_oConFormatoInvalido_noPideCodigo() {
        val vm = vm()
        assertFalse(vm.pedirCodigo("  "))
        assertEquals(ERROR_RECUPERAR_EMAIL_VACIO, vm.uiState.error)
        assertFalse(vm.pedirCodigo("ana@ien"))
        assertEquals(ERROR_RECUPERAR_EMAIL_FORMATO, vm.uiState.error)
        assertTrue(repo.correosPedidos.isEmpty())
    }

    @Test
    fun correoValido_pideElCodigo_normalizado_yAvisaConMensajeNeutro() {
        val vm = vm()
        assertTrue(vm.pedirCodigo(" ANA@ien.edu.ar "))
        assertEquals(listOf("ana@ien.edu.ar"), repo.correosPedidos)
        // Mismo aviso exista o no la cuenta: el servidor responde igual en los dos casos.
        assertEquals("Si el correo está registrado, te enviamos un código.", vm.uiState.aviso)
        assertEquals(ESPERA_REENVIO_SEGUNDOS, vm.uiState.segundosEspera)
        assertFalse(vm.uiState.isLoading)
    }

    @Test
    fun sinConexion_alPedirCodigo_muestraElError_yNoAvanza() {
        repo.falla = ErrorDatos.SinConexion()
        val vm = vm()
        assertFalse(vm.pedirCodigo())
        assertEquals(MENSAJE_SIN_CONEXION, vm.uiState.error)
        assertEquals(0, vm.uiState.segundosEspera)
    }

    @Test
    fun demasiadosEnvios_muestraElLimite() {
        repo.falla = ErrorDatos.DemasiadosIntentos()
        val vm = vm()
        vm.pedirCodigo()
        assertEquals(MENSAJE_DEMASIADOS_INTENTOS, vm.uiState.error)
    }

    @Test
    fun volverYContinuarConElMismoCorreo_noReenvia_peroConOtroSi() {
        val vm = vm()
        vm.pedirCodigo("ana@ien.edu.ar")
        assertTrue(vm.pedirCodigo("ana@ien.edu.ar")) // pasa a la pantalla 2 sin pedir otro
        assertEquals(1, repo.correosPedidos.size)
        // El correo se conserva al ir y volver.
        assertEquals("ana@ien.edu.ar", vm.uiState.email)
        vm.pedirCodigo("otra@ien.edu.ar")
        assertEquals(listOf("ana@ien.edu.ar", "otra@ien.edu.ar"), repo.correosPedidos)
    }

    // ---- Pantalla 2: código y reenvío

    @Test
    fun codigo_aceptaSoloDigitos_hastaSeis() {
        val vm = vm()
        vm.onCodigoChange("12a3-4567 89")
        assertEquals("123456", vm.uiState.codigo)
    }

    @Test
    fun codigoIncompleto_noLlamaAlServidor() {
        val vm = vm()
        vm.pedirCodigo()
        assertFalse(vm.verificar("123"))
        assertEquals(ERROR_RECUPERAR_CODIGO_INCOMPLETO, vm.uiState.error)
        assertEquals(0, repo.verificaciones)
    }

    @Test
    fun codigoIncorrecto_muestraMensajeClaro() {
        val vm = vm()
        vm.pedirCodigo()
        assertFalse(vm.verificar("000000"))
        assertEquals(MENSAJE_CODIGO_INVALIDO, vm.uiState.error)
        assertFalse(repo.sesionAbierta)
    }

    @Test
    fun codigoCorrecto_avanza() {
        val vm = vm()
        vm.pedirCodigo()
        assertTrue(vm.verificar())
        assertNull(vm.uiState.error)
        assertTrue(repo.sesionAbierta)
    }

    @Test
    fun reenviar_bloqueadoMientrasCuentaElContador() {
        val vm = vm()
        vm.pedirCodigo()
        vm.onReenviarCodigo()
        assertEquals(1, repo.correosPedidos.size) // todavía faltan 60 s
    }

    @Test
    fun reenviar_alTerminarElContador_pideOtroCodigo_yLimpiaLoEscrito() {
        // Reloj que deja pasar 60 segundos y después se frena (así el segundo contador no corre).
        var pausas = 0
        val vm = vm(reloj = { if (++pausas > 60) awaitCancellation() })
        vm.pedirCodigo()
        assertEquals(0, vm.uiState.segundosEspera)
        vm.onCodigoChange("123")
        vm.onReenviarCodigo()
        assertEquals(2, repo.correosPedidos.size)
        assertEquals("", vm.uiState.codigo)
        assertEquals(ESPERA_REENVIO_SEGUNDOS, vm.uiState.segundosEspera)
    }

    // ---- Pantalla 3: contraseña nueva

    @Test
    fun contrasenaCorta_noSeEnvia() {
        val vm = vm()
        assertFalse(vm.guardar("1234567", "1234567"))
        assertEquals(ERROR_RECUPERAR_CONTRASENA_CORTA, vm.uiState.error)
        assertNull(repo.contrasenaCambiada)
    }

    @Test
    fun contrasenasDistintas_noSeEnvia() {
        val vm = vm()
        assertFalse(vm.guardar("secreta123", "secreta124"))
        assertEquals(ERROR_RECUPERAR_CONTRASENAS_DISTINTAS, vm.uiState.error)
        assertNull(repo.contrasenaCambiada)
    }

    @Test
    fun flujoCompleto_cambiaLaContrasena_cierraLaSesion_yAvisaAlLogin() {
        val vm = vm()
        vm.pedirCodigo()
        vm.verificar()
        assertTrue(vm.guardar("secreta123", "secreta123"))
        assertEquals("secreta123", repo.contrasenaCambiada)
        assertFalse(repo.sesionAbierta)
        assertTrue(vm.uiState.exito)
        // No quedan las contraseñas en el estado.
        assertEquals("", vm.uiState.nuevaContrasena)
        assertEquals("", vm.uiState.repetirContrasena)
        // El login muestra "Contraseña actualizada" una sola vez.
        assertEquals(MENSAJE_CONTRASENA_ACTUALIZADA, LoginViewModel(FakeUsuarioRepository()).uiState.aviso)
        assertNull(LoginViewModel(FakeUsuarioRepository()).uiState.aviso)
        // El flujo nunca inició la sesión de la app (no entra a Home).
        assertNull(SesionActual.usuarioId)
    }

    @Test
    fun errorAlGuardar_sinConexion_igualAnterior_oDebil_muestraElMensaje() {
        listOf(
            ErrorDatos.SinConexion(),
            ErrorDatos.ContrasenaIgualAnterior(),
            ErrorDatos.ContrasenaDebil(),
        ).forEach { falla ->
            val vm = vm()
            repo.falla = null
            vm.pedirCodigo()
            vm.verificar()
            repo.falla = falla
            assertFalse(vm.guardar("secreta123", "secreta123"))
            assertEquals(falla.message, vm.uiState.error)
            assertFalse(vm.uiState.exito)
        }
    }

    @Test
    fun mostrarContrasena_seAlterna() {
        val vm = vm()
        assertFalse(vm.uiState.mostrarContrasena)
        vm.onToggleMostrarContrasena()
        assertTrue(vm.uiState.mostrarContrasena)
    }

    // ---- Abandonar el flujo con la sesión de recuperación abierta

    @Test
    fun abandonarDespuesDeVerificar_cierraLaSesionDeRecuperacion_unaSolaVez() {
        val vm = vm()
        vm.pedirCodigo()
        vm.verificar()
        assertTrue(repo.sesionAbierta)
        vm.descartarSesion()
        assertFalse(repo.sesionAbierta)
        vm.descartarSesion()
        assertEquals(1, repo.sesionesDescartadas)
    }

    @Test
    fun abandonarAntesDeVerificar_noTocaLaSesion() {
        val vm = vm()
        vm.pedirCodigo()
        vm.descartarSesion()
        assertEquals(0, repo.sesionesDescartadas)
    }

    @Test
    fun despuesDeCambiarLaContrasena_noSeDescartaOtraVez() {
        val vm = vm()
        vm.pedirCodigo()
        vm.verificar()
        vm.guardar("secreta123", "secreta123")
        vm.descartarSesion()
        assertEquals(0, repo.sesionesDescartadas)
    }
}
