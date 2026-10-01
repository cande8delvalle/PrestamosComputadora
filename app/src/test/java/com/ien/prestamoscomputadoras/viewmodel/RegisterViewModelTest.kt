package com.ien.prestamoscomputadoras.viewmodel

import com.ien.prestamoscomputadoras.data.repository.AdministradorRepository
import com.ien.prestamoscomputadoras.util.hashPassword
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RegisterViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val dao = FakeAdministradorDao()

    private fun nuevoVm() = RegisterViewModel(AdministradorRepository(dao))

    private fun vmCompleto(
        dni: String = "40123456",
        correo: String = "admin@ien.edu.ar",
    ) = nuevoVm().apply {
        onNombreChange("Juan")
        onApellidoChange("Pérez")
        onNombreUsuarioChange("jperez")
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
    fun formularioValido_guardaAdministradorConHashYCorreoEnMinuscula() {
        val vm = vmCompleto(correo = " Admin@IEN.edu.ar ")
        vm.onCrearCuentaClick()

        val guardado = dao.administradores.single()
        assertEquals("jperez", guardado.nombreUsuario)
        assertEquals("admin@ien.edu.ar", guardado.email)
        assertEquals(hashPassword("secreta1"), guardado.contrasenaHash)
        assertFalse(vm.uiState.isLoading)
    }

    @Test
    fun nombreUsuarioOCorreoEnUso_muestraErrorYNoGuarda() {
        vmCompleto().onCrearCuentaClick()

        val vm = vmCompleto().apply { onNombreUsuarioChange("JPEREZ") }
        vm.onCrearCuentaClick()

        assertEquals(ERROR_NOMBRE_USUARIO_EN_USO, vm.uiState.errores.nombreUsuario)
        assertEquals(ERROR_CORREO_EN_USO, vm.uiState.errores.correo)
        assertFalse(vm.uiState.mostrarModalExito)
        assertEquals(1, dao.administradores.size)
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
        listOf("admin@ien.edu.ar", "j.perez+x@gmail.com", " admin@ien.com ").forEach { correo ->
            val vm = vmCompleto(correo = correo)
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
