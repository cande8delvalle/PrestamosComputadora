package com.ien.prestamoscomputadoras.viewmodel

import com.ien.prestamoscomputadoras.data.entity.Administrador
import com.ien.prestamoscomputadoras.data.repository.AdministradorRepository
import com.ien.prestamoscomputadoras.data.repository.PermisosRepository
import com.ien.prestamoscomputadoras.util.SesionActual
import com.ien.prestamoscomputadoras.util.hashPassword
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class LoginViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val dao = FakeAdministradorDao()
    private var idAdmin = 0L

    @Before
    fun setUp() = runTest {
        idAdmin = dao.insertar(
            Administrador(
                nombre = "Juan",
                apellido = "Pérez",
                nombreUsuario = "Jperez",
                dni = "40123456",
                email = "admin@ien.edu.ar",
                contrasenaHash = hashPassword("secreta1"),
            ),
        )
    }

    @After
    fun tearDown() {
        SesionActual.administradorId = null
    }

    private fun login(usuario: String, contrasena: String): Pair<LoginViewModel, Boolean> {
        var exitoso = false
        val vm = LoginViewModel(AdministradorRepository(dao)).apply {
            onUsuarioChange(usuario)
            onContrasenaChange(contrasena)
            onLoginClick { exitoso = true }
        }
        return vm to exitoso
    }

    @Test
    fun credencialesCorrectas_conUsuarioOEmail_guardanLaSesion() {
        listOf("Jperez", "jperez", "admin@ien.edu.ar", "ADMIN@ien.edu.ar").forEach { usuario ->
            SesionActual.administradorId = null
            val (vm, exitoso) = login(usuario, "secreta1")
            assertTrue(usuario, exitoso)
            assertEquals(usuario, idAdmin, SesionActual.administradorId)
            assertNull(vm.uiState.error)
        }
    }

    @Test
    fun contrasenaIncorrectaOUsuarioInexistente_muestraError() {
        listOf("Jperez" to "otra", "nadie" to "secreta1").forEach { (usuario, contrasena) ->
            val (vm, exitoso) = login(usuario, contrasena)
            assertFalse(exitoso)
            assertEquals(ERROR_LOGIN_CREDENCIALES, vm.uiState.error)
            assertNull(SesionActual.administradorId)
            assertFalse(vm.uiState.isLoading)
        }
    }

    @Test
    fun cerrarSesion_limpiaElAdministrador() {
        login("Jperez", "secreta1")
        HomeViewModel(FakeHomeDao(), PermisosRepository(FakeRolDao()), idAdministrador = null).cerrarSesion()
        assertNull(SesionActual.administradorId)
    }
}
