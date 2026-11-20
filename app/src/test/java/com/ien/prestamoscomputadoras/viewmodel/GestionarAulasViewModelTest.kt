package com.ien.prestamoscomputadoras.viewmodel

import com.ien.prestamoscomputadoras.data.model.Aula
import com.ien.prestamoscomputadoras.data.model.Rol
import com.ien.prestamoscomputadoras.data.remote.ErrorDatos
import com.ien.prestamoscomputadoras.data.remote.MENSAJE_SIN_CONEXION
import com.ien.prestamoscomputadoras.data.remote.MENSAJE_SIN_PERMISO
import com.ien.prestamoscomputadoras.data.repository.MENSAJE_AULA_CON_PRESTAMOS
import com.ien.prestamoscomputadoras.data.repository.MENSAJE_AULA_DUPLICADA
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class GestionarAulasViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val aula1 = Aula(idAula = 1, nombre = "Aula 1")
    private val lab = Aula(idAula = 2, nombre = "Laboratorio")

    private val aulas = FakeAulaRepository(listOf(lab, aula1))
    private val permisos = FakePermisosRepository(
        rolesPorUsuario = mapOf(1L to Rol.ID_ADMIN, 2L to Rol.ID_PERSONAL),
    )

    private fun nuevoVm(idUsuario: Long? = 1L) = GestionarAulasViewModel(aulas, permisos, idUsuario)

    @Test
    fun admin_cargaLasAulasOrdenadasPorNombre() {
        val s = nuevoVm().uiState
        assertTrue(s.puedeGestionar)
        assertFalse(s.cargando)
        assertEquals(listOf(aula1, lab), s.aulas)
        assertNull(s.error)
    }

    @Test
    fun noAdmin_noPuedeGestionarNiSeLePidenLasAulas() {
        val vm = nuevoVm(idUsuario = 2L)
        assertFalse(vm.uiState.cargando)
        assertFalse(vm.uiState.puedeGestionar)
        assertTrue(vm.uiState.aulas.isEmpty())
        assertEquals(0, aulas.lecturas)

        // Aunque la UI no ofrece nada, los comandos tampoco hacen nada.
        vm.onNombreChange("Aula 9")
        vm.onAgregarClick()
        vm.onEliminarClick(aula1)
        vm.onConfirmarEliminar()
        assertEquals(2, aulas.aulas.size)
        assertNull(vm.uiState.aulaPorEliminar)
    }

    @Test
    fun sinSesion_noPuedeGestionar() {
        assertFalse(nuevoVm(idUsuario = null).uiState.puedeGestionar)
    }

    @Test
    fun agregar_guardaConNombreRecortadoLimpiaElCampoYRecarga() {
        val vm = nuevoVm()
        vm.onNombreChange("  Aula 3  ")
        vm.onAgregarClick()

        assertEquals(setOf("Aula 1", "Laboratorio", "Aula 3"), aulas.aulas.map { it.nombre }.toSet())
        assertEquals(listOf("Aula 1", "Aula 3", "Laboratorio"), vm.uiState.aulas.map { it.nombre })
        assertEquals("", vm.uiState.nombre)
        assertFalse(vm.uiState.guardando)
        assertNull(vm.uiState.errorNombre)
    }

    @Test
    fun agregar_nombreVacio_muestraErrorYNoGuarda() {
        val vm = nuevoVm()
        vm.onNombreChange("   ")
        vm.onAgregarClick()

        assertEquals(ERROR_CAMPO_OBLIGATORIO, vm.uiState.errorNombre)
        assertEquals(2, aulas.aulas.size)

        vm.onNombreChange("Aula 3")
        assertNull(vm.uiState.errorNombre)
    }

    @Test
    fun agregar_nombreRepetido_mostrarMensajeClaroDebajoDelCampo() {
        val vm = nuevoVm()
        vm.onNombreChange("aula 1") // mismo nombre sin distinguir mayúsculas
        vm.onAgregarClick()

        assertEquals(MENSAJE_AULA_DUPLICADA, vm.uiState.errorNombre)
        assertEquals("Ya existe un aula con ese nombre", vm.uiState.errorNombre)
        assertNull(vm.uiState.error)
        assertEquals(2, aulas.aulas.size)
        assertFalse(vm.uiState.guardando)
        // Se conserva lo escrito para corregirlo.
        assertEquals("aula 1", vm.uiState.nombre)
    }

    @Test
    fun agregar_sinInternet_muestraElErrorArriba() {
        val vm = nuevoVm()
        aulas.falla = ErrorDatos.SinConexion()
        vm.onNombreChange("Aula 3")
        vm.onAgregarClick()

        assertEquals(MENSAJE_SIN_CONEXION, vm.uiState.error)
        assertNull(vm.uiState.errorNombre)
        assertFalse(vm.uiState.guardando)
    }

    @Test
    fun agregar_sinPermisoEnLaBase_avisa() {
        val vm = nuevoVm()
        aulas.fallaAlInsertar = ErrorDatos.SinPermiso()
        vm.onNombreChange("Aula 3")
        vm.onAgregarClick()

        assertEquals(MENSAJE_SIN_PERMISO, vm.uiState.error)
    }

    @Test
    fun eliminar_pideConfirmacionYSePuedeCancelar() {
        val vm = nuevoVm()
        vm.onEliminarClick(lab)
        assertEquals(lab, vm.uiState.aulaPorEliminar)
        assertEquals(2, aulas.aulas.size)

        vm.onCancelarEliminar()
        assertNull(vm.uiState.aulaPorEliminar)
        assertEquals(2, aulas.aulas.size)
    }

    @Test
    fun eliminar_confirmado_borraYRecarga() {
        val vm = nuevoVm()
        vm.onEliminarClick(lab)
        vm.onConfirmarEliminar()

        assertEquals(listOf(aula1), aulas.aulas)
        assertEquals(listOf(aula1), vm.uiState.aulas)
        assertNull(vm.uiState.aulaPorEliminar)
        assertNull(vm.uiState.error)
    }

    @Test
    fun eliminar_aulaConPrestamos_muestraMensajeClaroYNoBorra() {
        aulas.idsConPrestamos += lab.idAula
        val vm = nuevoVm()
        vm.onEliminarClick(lab)
        vm.onConfirmarEliminar()

        assertEquals("No se puede eliminar: tiene préstamos registrados", MENSAJE_AULA_CON_PRESTAMOS)
        assertEquals(MENSAJE_AULA_CON_PRESTAMOS, vm.uiState.error)
        assertFalse(vm.uiState.errorDeCarga) // no ofrece "Reintentar"
        assertEquals(listOf(aula1, lab), vm.uiState.aulas)
        assertEquals(2, aulas.aulas.size)
    }

    @Test
    fun sinInternetAlCargar_muestraErrorYReintentarFunciona() {
        aulas.falla = ErrorDatos.SinConexion()
        val vm = nuevoVm()

        assertEquals(MENSAJE_SIN_CONEXION, vm.uiState.error)
        assertTrue(vm.uiState.errorDeCarga)
        assertTrue(vm.uiState.puedeGestionar)

        aulas.falla = null
        vm.cargar()
        assertNull(vm.uiState.error)
        assertEquals(listOf(aula1, lab), vm.uiState.aulas)
    }
}
