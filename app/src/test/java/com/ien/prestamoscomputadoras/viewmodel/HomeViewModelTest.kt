package com.ien.prestamoscomputadoras.viewmodel

import com.ien.prestamoscomputadoras.data.Permiso
import com.ien.prestamoscomputadoras.data.entity.Prestamo
import com.ien.prestamoscomputadoras.data.entity.Rol
import com.ien.prestamoscomputadoras.data.entity.RolPermiso
import com.ien.prestamoscomputadoras.data.repository.PermisosRepository
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    /** Epoch ms en la zona horaria del dispositivo (la misma que usa el ViewModel). */
    private fun fecha(dia: Int, hora: Int, minuto: Int = 0): Long =
        Calendar.getInstance().apply {
            clear()
            set(2026, Calendar.SEPTEMBER, dia, hora, minuto)
        }.timeInMillis

    // Miércoles 30/09/2026 a las 18:00.
    private val ahora = fecha(30, 18)

    private fun prestamo(id: Long, prestado: Long, devuelto: Long? = null) = Prestamo(
        idPrestamo = id,
        idAlumno = 1,
        idComputadora = id,
        idAdministrador = 1,
        fechaPrestamo = prestado,
        fechaDevolucion = devuelto,
        estado = if (devuelto != null) Prestamo.ESTADO_DEVUELTO else Prestamo.ESTADO_ACTIVO,
    )

    @Test
    fun sinPrestamos_todoEnCero() {
        val vm = HomeViewModel(FakeHomeDao(), PermisosRepository(FakeRolDao()), idAdministrador = null, ahora = { ahora })
        assertEquals(0, vm.uiState.cantidadPrestados)
        assertEquals(0, vm.uiState.cantidadDevueltosHoy)
        assertTrue(vm.uiState.actividadReciente.isEmpty())
    }

    @Test
    fun contadores_activosYDevueltosHoy() {
        val dao = FakeHomeDao()
        dao.prestamos.value = listOf(
            prestamo(1, fecha(28, 9)), // activo desde el lunes: cuenta como prestado
            prestamo(2, fecha(30, 8)), // activo de hoy
            prestamo(3, fecha(29, 9), devuelto = fecha(30, 10)), // prestado ayer, devuelto hoy
            prestamo(4, fecha(30, 9), devuelto = fecha(30, 11)), // prestado y devuelto hoy
            prestamo(5, fecha(28, 9), devuelto = fecha(29, 10)), // devuelto ayer: no cuenta
        )
        val vm = HomeViewModel(dao, PermisosRepository(FakeRolDao()), idAdministrador = null, ahora = { ahora })

        assertEquals(2, vm.uiState.cantidadPrestados)
        assertEquals(2, vm.uiState.cantidadDevueltosHoy)
    }

    @Test
    fun actividadReciente_movimientosDeHoyDelMasRecienteAlMasAntiguo() {
        val dao = FakeHomeDao()
        dao.prestamos.value = listOf(
            prestamo(1, fecha(28, 9)), // activo de otro día: no aparece
            prestamo(2, fecha(30, 8, 30)),
            prestamo(3, fecha(29, 9), devuelto = fecha(30, 10, 15)),
            prestamo(4, fecha(30, 9), devuelto = fecha(30, 17, 45)),
        )
        val vm = HomeViewModel(dao, PermisosRepository(FakeRolDao()), idAdministrador = null, ahora = { ahora })

        assertEquals(
            listOf(
                ActividadReciente("Juan Pérez", "PC-4", "17:45", EstadoPrestamo.DEVUELTO),
                ActividadReciente("Juan Pérez", "PC-3", "10:15", EstadoPrestamo.DEVUELTO),
                ActividadReciente("Juan Pérez", "PC-2", "08:30", EstadoPrestamo.ACTIVO),
            ),
            vm.uiState.actividadReciente,
        )
    }

    @Test
    fun actividadReciente_maximoCinco() {
        val dao = FakeHomeDao()
        dao.prestamos.value = (1L..7L).map { prestamo(it, fecha(30, 8 + it.toInt())) }
        val vm = HomeViewModel(dao, PermisosRepository(FakeRolDao()), idAdministrador = null, ahora = { ahora })

        assertEquals(listOf("PC-7", "PC-6", "PC-5", "PC-4", "PC-3"), vm.uiState.actividadReciente.map { it.codigoComputadora })
    }

    @Test
    fun cambiosEnLaBase_seReflejanSolos() {
        val dao = FakeHomeDao()
        val vm = HomeViewModel(dao, PermisosRepository(FakeRolDao()), idAdministrador = null, ahora = { ahora })
        assertEquals(0, vm.uiState.cantidadPrestados)

        // Se registra un préstamo (Room volvería a emitir).
        dao.prestamos.value = listOf(prestamo(1, fecha(30, 9)))
        assertEquals(1, vm.uiState.cantidadPrestados)
        assertEquals(1, vm.uiState.actividadReciente.size)

        // Se devuelve.
        dao.prestamos.value = listOf(prestamo(1, fecha(30, 9), devuelto = fecha(30, 12)))
        assertEquals(0, vm.uiState.cantidadPrestados)
        assertEquals(1, vm.uiState.cantidadDevueltosHoy)
        assertEquals(EstadoPrestamo.DEVUELTO, vm.uiState.actividadReciente.single().estado)
    }

    @Test
    fun acceso_adminVeTodoYAdministracion() {
        val rolDao = FakeRolDao(rolesPorAdministrador = mapOf(1L to Rol.ID_ADMIN))
        val vm = HomeViewModel(FakeHomeDao(), PermisosRepository(rolDao), idAdministrador = 1L, ahora = { ahora })

        assertTrue(vm.uiState.acceso.esAdmin)
        assertEquals(Permiso.entries.toSet(), vm.uiState.acceso.permisos)
    }

    @Test
    fun acceso_personalSoloSusPermisosYSeActualiza() {
        val rolDao = FakeRolDao(
            rolesPorAdministrador = mapOf(2L to Rol.ID_PERSONAL),
            permisosIniciales = Permiso.inicialesPersonal.map { RolPermiso(Rol.ID_PERSONAL, it.name) }.toSet(),
        )
        val vm = HomeViewModel(FakeHomeDao(), PermisosRepository(rolDao), idAdministrador = 2L, ahora = { ahora })

        assertFalse(vm.uiState.acceso.esAdmin)
        assertEquals(Permiso.inicialesPersonal, vm.uiState.acceso.permisos)
        assertFalse(vm.uiState.acceso.puede(Permiso.REGISTRAR_ALUMNO))

        // El Admin le habilita Registrar Alumnos: Home lo refleja sola.
        rolDao.permisos.value = rolDao.permisos.value + RolPermiso(Rol.ID_PERSONAL, Permiso.REGISTRAR_ALUMNO.name)
        assertTrue(vm.uiState.acceso.puede(Permiso.REGISTRAR_ALUMNO))
    }

    @Test
    fun acceso_personalNuncaTieneGestionDePermisos() {
        // Aunque la base tuviera la fila (no debería), no se le otorga.
        val rolDao = FakeRolDao(
            rolesPorAdministrador = mapOf(2L to Rol.ID_PERSONAL),
            permisosIniciales = setOf(RolPermiso(Rol.ID_PERSONAL, Permiso.GESTION_PERMISOS.name)),
        )
        val vm = HomeViewModel(FakeHomeDao(), PermisosRepository(rolDao), idAdministrador = 2L, ahora = { ahora })

        assertFalse(vm.uiState.acceso.esAdmin)
        assertFalse(vm.uiState.acceso.puede(Permiso.GESTION_PERMISOS))
    }

    @Test
    fun acceso_sinSesion_noPuedeNada() {
        val vm = HomeViewModel(FakeHomeDao(), PermisosRepository(FakeRolDao()), idAdministrador = null, ahora = { ahora })
        assertFalse(vm.uiState.acceso.esAdmin)
        assertTrue(vm.uiState.acceso.permisos.isEmpty())
    }
}
