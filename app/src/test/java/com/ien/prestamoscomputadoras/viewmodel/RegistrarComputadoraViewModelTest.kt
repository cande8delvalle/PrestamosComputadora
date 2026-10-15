package com.ien.prestamoscomputadoras.viewmodel

import com.ien.prestamoscomputadoras.data.model.Computadora
import com.ien.prestamoscomputadoras.data.remote.ErrorDatos
import com.ien.prestamoscomputadoras.data.remote.MENSAJE_SIN_PERMISO
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RegistrarComputadoraViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repo = FakeComputadoraRepository(listOf(Computadora(idComputadora = 7, codigo = "PC-07")))

    private fun registrar(codigo: String, modelo: String = "") =
        RegistrarComputadoraViewModel(repo).apply {
            onCodigoChange(codigo)
            onModeloChange(modelo)
            onRegistrarClick()
        }

    @Test
    fun codigoValido_insertaConIdIgualAlNumeroYMuestraModal() {
        val vm = registrar(" PC-11 ", modelo = " ProBook 440 G8 ")

        assertEquals(Computadora(idComputadora = 11, codigo = "PC-11", modelo = "ProBook 440 G8"), repo.computadoras.last())
        assertNull(vm.uiState.errorCodigo)
        assertTrue(vm.uiState.mostrarModalExito)
        assertFalse(vm.uiState.isLoading)
    }

    @Test
    fun modeloVacio_seGuardaComoNull() {
        registrar("PC-12", modelo = "   ")
        assertNull(repo.computadoras.last().modelo)
    }

    @Test
    fun codigoVacio_esObligatorio() {
        val vm = registrar("  ")
        assertEquals(ERROR_CAMPO_OBLIGATORIO, vm.uiState.errorCodigo)
        assertEquals(1, repo.computadoras.size)
    }

    @Test
    fun codigoSinFormato_esInvalido() {
        listOf("PC11", "11", "PC-", "PC-1A", "P C-11", "-11").forEach { codigo ->
            val vm = registrar(codigo)
            assertEquals(codigo, ERROR_CODIGO_FORMATO, vm.uiState.errorCodigo)
        }
        assertEquals(1, repo.computadoras.size)
    }

    @Test
    fun codigoExistente_muestraErrorYNoInserta() {
        val vm = registrar("PC-07")
        assertEquals(ERROR_CODIGO_DUPLICADO, vm.uiState.errorCodigo)
        assertFalse(vm.uiState.mostrarModalExito)
        assertEquals(1, repo.computadoras.size)
    }

    @Test
    fun numeroYaUsadoPorOtroCodigo_muestraErrorYNoInserta() {
        listOf("LAB-7", "PC-7").forEach { codigo ->
            val vm = registrar(codigo)
            assertEquals(codigo, errorNumeroEnUso(7, "PC-07"), vm.uiState.errorCodigo)
        }
        assertEquals(1, repo.computadoras.size)
    }

    @Test
    fun noAdmin_laBaseLoRechazaYSeMuestraMensajeClaro() {
        repo.fallaAlInsertar = ErrorDatos.SinPermiso()
        val vm = registrar("PC-11")

        assertEquals(MENSAJE_SIN_PERMISO, vm.uiState.errorGeneral)
        assertNull(vm.uiState.errorCodigo)
        assertFalse(vm.uiState.mostrarModalExito)
        assertFalse(vm.uiState.isLoading)
    }

    @Test
    fun registrarOtra_limpiaElFormulario() {
        val vm = registrar("PC-11", modelo = "ProBook")
        vm.onRegistrarOtraClick()
        assertEquals(RegistrarComputadoraUiState(), vm.uiState)
    }
}
