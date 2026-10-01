package com.ien.prestamoscomputadoras.viewmodel

import com.ien.prestamoscomputadoras.data.Permiso
import com.ien.prestamoscomputadoras.data.entity.Rol
import com.ien.prestamoscomputadoras.data.entity.RolPermiso
import com.ien.prestamoscomputadoras.data.repository.PermisosRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RolesPermisosViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val rolDao = FakeRolDao(
        rolesPorAdministrador = mapOf(1L to Rol.ID_ADMIN, 2L to Rol.ID_PERSONAL),
        permisosIniciales = Permiso.inicialesPersonal.map { RolPermiso(Rol.ID_PERSONAL, it.name) }.toSet(),
    )

    private fun nuevoVm(idAdministrador: Long? = 1L) =
        RolesPermisosViewModel(PermisosRepository(rolDao), idAdministrador)

    private fun permisosPersonalGuardados() =
        rolDao.permisos.value.filter { it.idRol == Rol.ID_PERSONAL }.map { it.permiso }.toSet()

    @Test
    fun tabAdmin_todosTildadosYNoEditables_incluyeGestion() {
        val vm = nuevoVm()
        assertEquals(RolTab.ADMIN, vm.uiState.tab)

        val items = vm.uiState.items
        assertEquals(Permiso.entries, items.map { it.permiso })
        assertTrue(items.all { it.habilitado && !it.editable })
    }

    @Test
    fun tabAdmin_tocarNoCambiaNada() {
        val vm = nuevoVm()
        val antes = permisosPersonalGuardados()
        vm.onPermisoClick(Permiso.REGISTRAR_ALUMNO)
        assertEquals(antes, permisosPersonalGuardados())
    }

    @Test
    fun tabPersonal_sinGestionDePermisos_yConLosIniciales() {
        val vm = nuevoVm()
        vm.onTabSeleccionado(RolTab.PERSONAL)

        val items = vm.uiState.items
        assertEquals(Permiso.entries.filter { it != Permiso.GESTION_PERMISOS }, items.map { it.permiso })
        assertTrue(items.all { it.editable })
        assertEquals(Permiso.inicialesPersonal, items.filter { it.habilitado }.map { it.permiso }.toSet())
    }

    @Test
    fun tabPersonal_tildarYDestildar_seGuardaEnLaBase() {
        val vm = nuevoVm()
        vm.onTabSeleccionado(RolTab.PERSONAL)

        vm.onPermisoClick(Permiso.REGISTRAR_ALUMNO)
        assertTrue(Permiso.REGISTRAR_ALUMNO.name in permisosPersonalGuardados())
        assertTrue(vm.uiState.items.single { it.permiso == Permiso.REGISTRAR_ALUMNO }.habilitado)

        vm.onPermisoClick(Permiso.REGISTRAR_PRESTAMO)
        assertFalse(Permiso.REGISTRAR_PRESTAMO.name in permisosPersonalGuardados())
        assertFalse(vm.uiState.items.single { it.permiso == Permiso.REGISTRAR_PRESTAMO }.habilitado)
    }

    @Test
    fun gestionDePermisos_nuncaSeOtorgaAPersonal() = runTest {
        val vm = nuevoVm()
        vm.onTabSeleccionado(RolTab.PERSONAL)
        vm.onPermisoClick(Permiso.GESTION_PERMISOS)
        assertFalse(Permiso.GESTION_PERMISOS.name in permisosPersonalGuardados())

        // Tampoco por el repositorio directamente.
        val error = runCatching {
            PermisosRepository(rolDao).cambiarPermisoPersonal(Permiso.GESTION_PERMISOS, habilitado = true)
        }.exceptionOrNull()
        assertTrue(error is IllegalArgumentException)
        assertFalse(Permiso.GESTION_PERMISOS.name in permisosPersonalGuardados())
    }

    @Test
    fun personalAdministrativo_noTieneAcceso_niPuedeEditar() {
        val vm = nuevoVm(idAdministrador = 2L)
        assertFalse(vm.uiState.cargando)
        assertFalse(vm.uiState.esAdmin)

        val antes = permisosPersonalGuardados()
        vm.onTabSeleccionado(RolTab.PERSONAL)
        vm.onPermisoClick(Permiso.REGISTRAR_ALUMNO)
        assertEquals(antes, permisosPersonalGuardados())
    }

    @Test
    fun sinSesion_noTieneAcceso() {
        val vm = nuevoVm(idAdministrador = null)
        assertFalse(vm.uiState.esAdmin)
    }
}
