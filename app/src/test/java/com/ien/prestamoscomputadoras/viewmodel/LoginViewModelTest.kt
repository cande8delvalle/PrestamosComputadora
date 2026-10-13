package com.ien.prestamoscomputadoras.viewmodel

import com.ien.prestamoscomputadoras.data.remote.ErrorDatos
import com.ien.prestamoscomputadoras.data.remote.MENSAJE_SIN_CONEXION
import com.ien.prestamoscomputadoras.data.repository.DatosRegistro
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import com.ien.prestamoscomputadoras.util.SesionActual

class LoginViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repo = FakeUsuarioRepository()

    @Before
    fun setUp() = runTest {
        repo.registrar(
            DatosRegistro(
                nombre = "Juan",
                apellido = "Pérez",
                nombreUsuario = "Jperez",
                dni = "40123456",
                email = "admin@ien.edu.ar",
            ),
            "secreta1",
        )
    }

    @After
    fun tearDown() {
        SesionActual.usuarioId = null
    }

    private fun login(usuario: String, contrasena: String): Pair<LoginViewModel, Boolean> {
        var exitoso = false
        val vm = LoginViewModel(repo).apply {
            onUsuarioChange(usuario)
            onContrasenaChange(contrasena)
            onLoginClick { exitoso = true }
        }
        return vm to exitoso
    }

    @Test
    fun emailCorrecto_sinImportarMayusculas_guardaElPerfilEnLaSesion() {
        listOf("admin@ien.edu.ar", " ADMIN@ien.edu.ar ").forEach { usuario ->
            SesionActual.usuarioId = null
            val (vm, exitoso) = login(usuario, "secreta1")
            assertTrue(usuario, exitoso)
            assertEquals(usuario, 1L, SesionActual.usuarioId)
            assertNull(vm.uiState.error)
            assertFalse(vm.uiState.isLoading)
        }
    }

    @Test
    fun nombreDeUsuario_pideElCorreo() {
        val (vm, exitoso) = login("Jperez", "secreta1")
        assertFalse(exitoso)
        assertEquals(ERROR_LOGIN_EMAIL, vm.uiState.error)
    }

    @Test
    fun contrasenaIncorrectaOEmailInexistente_muestraError() {
        listOf("admin@ien.edu.ar" to "otra", "nadie@ien.edu.ar" to "secreta1").forEach { (usuario, contrasena) ->
            val (vm, exitoso) = login(usuario, contrasena)
            assertFalse(exitoso)
            assertEquals(ERROR_LOGIN_CREDENCIALES, vm.uiState.error)
            assertNull(SesionActual.usuarioId)
            assertFalse(vm.uiState.isLoading)
        }
    }

    @Test
    fun sinInternet_muestraMensajeClaro() {
        repo.falla = ErrorDatos.SinConexion()
        val (vm, exitoso) = login("admin@ien.edu.ar", "secreta1")
        assertFalse(exitoso)
        assertEquals(MENSAJE_SIN_CONEXION, vm.uiState.error)
        assertFalse(vm.uiState.isLoading)
    }

    @Test
    fun cerrarSesion_limpiaElPerfilYAvisaASupabase() {
        login("admin@ien.edu.ar", "secreta1")
        HomeViewModel(FakeHomeRepository(), FakePermisosRepository(), idUsuario = 1L, usuarioRepository = repo)
            .cerrarSesion()
        assertNull(SesionActual.usuarioId)
        // El cierre en Supabase corre en otro scope (SesionActual.scope): se espera hasta 2 s.
        val limite = System.currentTimeMillis() + 2_000
        while (repo.sesionesCerradas == 0 && System.currentTimeMillis() < limite) Thread.sleep(10)
        assertEquals(1, repo.sesionesCerradas)
    }
}
