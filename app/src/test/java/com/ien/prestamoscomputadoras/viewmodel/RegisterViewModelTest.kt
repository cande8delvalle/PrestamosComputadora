package com.ien.prestamoscomputadoras.viewmodel

import com.ien.prestamoscomputadoras.data.model.Rol
import com.ien.prestamoscomputadoras.data.remote.ErrorDatos
import com.ien.prestamoscomputadoras.data.remote.MENSAJE_SIN_CONEXION
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RegisterViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repo = FakeUsuarioRepository()

    private fun nuevoVm() = RegisterViewModel(repo)

    private fun vmCompleto(
        dni: String = "40123456",
        correo: String = "admin@ien.edu.ar",
        nombreUsuario: String = "jperez",
    ) = nuevoVm().apply {
        onNombreChange("Juan")
        onApellidoChange("Pérez")
        onNombreUsuarioChange(nombreUsuario)
        onDniChange(dni)
        onCorreoChange(correo)
        onContrasenaChange("secreta1")
        onRepetirContrasenaChange("secreta1")
    }

    @Test
    fun formularioVacio_marcaTodosLosCamposComoObligatorios() {
        val vm = nuevoVm()
        vm.onCrearCuentaClick()

        val e = vm.uiState.errores
        listOf(e.nombre, e.apellido, e.nombreUsuario, e.dni, e.correo, e.contrasena, e.repetirContrasena)
            .forEach { assertEquals(ERROR_CAMPO_OBLIGATORIO, it) }
        assertFalse(vm.uiState.mostrarModalExito)
    }

    @Test
    fun formularioValido_muestraModal() {
        val vm = vmCompleto()
        vm.onCrearCuentaClick()

        assertFalse(vm.uiState.errores.hayAlguno)
        assertTrue(vm.uiState.mostrarModalExito)
    }

    @Test
    fun formularioValido_mandaLosDatosConCorreoEnMinuscula() {
        val vm = vmCompleto(correo = " Admin@IEN.edu.ar ")
        vm.onCrearCuentaClick()

        val perfil = repo.perfiles.single()
        assertEquals("jperez", perfil.nombreUsuario)
        assertEquals("admin@ien.edu.ar", perfil.email)
        assertEquals("40123456", perfil.dni)
        assertEquals("secreta1", repo.contrasenas["admin@ien.edu.ar"])
        assertFalse(vm.uiState.isLoading)
    }

    @Test
    fun primeraCuentaAdmin_lasSiguientesPersonal() {
        // La regla vive en el trigger handle_new_user de la base; el fake la imita.
        vmCompleto().onCrearCuentaClick()
        vmCompleto(correo = "otro@ien.edu.ar", nombreUsuario = "otro").onCrearCuentaClick()
        assertEquals(listOf(Rol.ID_ADMIN, Rol.ID_PERSONAL), repo.perfiles.map { it.idRol })
    }

    @Test
    fun nombreUsuarioEnUso_muestraErrorEnEseCampo() {
        vmCompleto().onCrearCuentaClick()

        val vm = vmCompleto(correo = "otro@ien.edu.ar")
        vm.onCrearCuentaClick()

        assertEquals(ERROR_NOMBRE_USUARIO_EN_USO, vm.uiState.errores.nombreUsuario)
        assertFalse(vm.uiState.mostrarModalExito)
        assertEquals(1, repo.perfiles.size)
    }

    @Test
    fun correoEnUso_muestraErrorEnEseCampo() {
        vmCompleto().onCrearCuentaClick()

        val vm = vmCompleto(nombreUsuario = "otro")
        vm.onCrearCuentaClick()

        assertEquals(ERROR_CORREO_EN_USO, vm.uiState.errores.correo)
        assertFalse(vm.uiState.mostrarModalExito)
    }

    @Test
    fun sinInternet_muestraErrorGeneral() {
        repo.falla = ErrorDatos.SinConexion()
        val vm = vmCompleto()
        vm.onCrearCuentaClick()

        assertEquals(MENSAJE_SIN_CONEXION, vm.uiState.errorGeneral)
        assertFalse(vm.uiState.mostrarModalExito)
        assertFalse(vm.uiState.isLoading)
    }

    @Test
    fun dniConPuntosGuionesEspaciosOLetras_esInvalido() {
        listOf("40.123.456", "40-123456", "40 123 456", "40a23456").forEach { dni ->
            val vm = vmCompleto(dni = dni)
            vm.onCrearCuentaClick()
            assertEquals(dni, ERROR_DNI_FORMATO, vm.uiState.errores.dni)
            assertFalse(vm.uiState.mostrarModalExito)
        }
    }

    @Test
    fun correoMalFormado_esInvalido() {
        listOf("juan", "juan@", "juan@ien", "@ien.com", "juan@ien.", "juan ien@ien.com").forEach { correo ->
            val vm = vmCompleto(correo = correo)
            vm.onCrearCuentaClick()
            assertEquals(correo, ERROR_CORREO_FORMATO, vm.uiState.errores.correo)
        }
    }

    @Test
    fun correoValido_pasa() {
        listOf("admin@ien.edu.ar", "j.perez+x@gmail.com", " admin2@ien.com ").forEachIndexed { i, correo ->
            val vm = vmCompleto(correo = correo, nombreUsuario = "u$i")
            vm.onCrearCuentaClick()
            assertNull(correo, vm.uiState.errores.correo)
        }
    }

    @Test
    fun variosErrores_seMuestranTodosALaVez_yObligatorioTienePrioridad() {
        val vm = vmCompleto(dni = "40.123.456", correo = "juan").apply {
            onNombreUsuarioChange("")
            onRepetirContrasenaChange("otra")
        }
        vm.onCrearCuentaClick()

        val e = vm.uiState.errores
        assertEquals(ERROR_CAMPO_OBLIGATORIO, e.nombreUsuario)
        assertEquals(ERROR_DNI_FORMATO, e.dni)
        assertEquals(ERROR_CORREO_FORMATO, e.correo)
        assertEquals(ERROR_CONTRASENAS_NO_COINCIDEN, e.repetirContrasena)
    }
}
