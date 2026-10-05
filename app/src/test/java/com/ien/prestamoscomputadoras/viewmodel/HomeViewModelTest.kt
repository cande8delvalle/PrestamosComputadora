package com.ien.prestamoscomputadoras.viewmodel

import com.ien.prestamoscomputadoras.data.Permiso
import com.ien.prestamoscomputadoras.data.entity.Prestamo
import com.ien.prestamoscomputadoras.data.entity.Rol
import com.ien.prestamoscomputadoras.data.entity.RolPermiso
import com.ien.prestamoscomputadoras.data.repository.HomeRepository
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

    /** Lo entrega el administrador 1; si se devolvió, lo recibe el 2. */
    private fun prestamo(id: Long, prestado: Long, devuelto: Long? = null) = Prestamo(
        idPrestamo = id,
        idAlumno = 1,
        idComputadora = id,
        idAdministradorPrestamo = 1,
        fechaPrestamo = prestado,
        fechaDevolucion = devuelto,
        idAdministradorDevolucion = devuelto?.let { 2 },
        estado = if (devuelto != null) Prestamo.ESTADO_DEVUELTO else Prestamo.ESTADO_ACTIVO,
    )

    private fun nuevoVm(
        dao: FakeHomeDao = FakeHomeDao(),
        rolDao: FakeRolDao = FakeRolDao(),
        idAdministrador: Long? = null,
    ) = HomeViewModel(HomeRepository(dao), PermisosRepository(rolDao), idAdministrador, ahora = { ahora })

    @Test
    fun sinPrestamos_todoEnCero() {
        val s = nuevoVm().uiState.value
        assertEquals(0, s.cantidadPrestados)
        assertEquals(0, s.cantidadDevueltosHoy)
        assertTrue(s.actividadReciente.isEmpty())
    }

    @Test
    fun contadores_activosDeCualquierFechaYDevueltosHoy() {
        val dao = FakeHomeDao()
        dao.prestamos.value = listOf(
            prestamo(1, fecha(28, 9)), // activo desde el lunes: cuenta como prestado
            prestamo(2, fecha(30, 8)), // activo de hoy
            prestamo(3, fecha(29, 9), devuelto = fecha(30, 10)), // prestado ayer, devuelto hoy
            prestamo(4, fecha(30, 9), devuelto = fecha(30, 11)), // prestado y devuelto hoy
            prestamo(5, fecha(28, 9), devuelto = fecha(29, 10)), // devuelto ayer: no cuenta
        )
        val s = nuevoVm(dao).uiState.value

        assertEquals(2, s.cantidadPrestados)
        assertEquals(2, s.cantidadDevueltosHoy)
    }

    @Test
    fun actividadReciente_mezclaPrestamosYDevolucionesDelMasRecienteAlMasAntiguo() {
        val dao = FakeHomeDao()
        dao.prestamos.value = listOf(
            prestamo(2, fecha(30, 8, 30)),
            prestamo(3, fecha(29, 9), devuelto = fecha(30, 10, 15)),
            prestamo(4, fecha(30, 9), devuelto = fecha(30, 17, 45)),
        )
        val vm = nuevoVm(dao)

        assertEquals(
            listOf(
                ActividadReciente("Juan Pérez", "PC-4", "17:45", TipoMovimiento.DEVOLUCION),
                ActividadReciente("Juan Pérez", "PC-3", "10:15", TipoMovimiento.DEVOLUCION),
                ActividadReciente("Juan Pérez", "PC-4", "09:00", TipoMovimiento.PRESTAMO),
                ActividadReciente("Juan Pérez", "PC-2", "08:30", TipoMovimiento.PRESTAMO),
                ActividadReciente("Juan Pérez", "PC-3", "09:00", TipoMovimiento.PRESTAMO),
            ),
            vm.uiState.value.actividadReciente,
        )
    }

    @Test
    fun actividadReciente_maximoCinco() {
        val dao = FakeHomeDao()
        dao.prestamos.value = (1L..7L).map { prestamo(it, fecha(30, 8 + it.toInt())) }
        val vm = nuevoVm(dao)

        assertEquals(
            listOf("PC-7", "PC-6", "PC-5", "PC-4", "PC-3"),
            vm.uiState.value.actividadReciente.map { it.codigoComputadora },
        )
    }

    @Test
    fun cambiosEnLaBase_seReflejanSolos() {
        val dao = FakeHomeDao()
        val vm = nuevoVm(dao)
        assertEquals(0, vm.uiState.value.cantidadPrestados)

        // Se registra un préstamo (Room volvería a emitir).
        dao.prestamos.value = listOf(prestamo(1, fecha(30, 9)))
        assertEquals(1, vm.uiState.value.cantidadPrestados)
        assertEquals(1, vm.uiState.value.actividadReciente.size)

        // Se devuelve: deja de estar activo y aparece la devolución arriba.
        dao.prestamos.value = listOf(prestamo(1, fecha(30, 9), devuelto = fecha(30, 12)))
        assertEquals(0, vm.uiState.value.cantidadPrestados)
        assertEquals(1, vm.uiState.value.cantidadDevueltosHoy)
        assertEquals(
            listOf(TipoMovimiento.DEVOLUCION, TipoMovimiento.PRESTAMO),
            vm.uiState.value.actividadReciente.map { it.tipo },
        )
    }

    @Test
    fun prestados_bajaAlDevolverUnPrestamoDeOtroDia() {
        val dao = FakeHomeDao()
        dao.prestamos.value = listOf(prestamo(1, fecha(28, 9)), prestamo(2, fecha(30, 9)))
        val vm = nuevoVm(dao)
        assertEquals(2, vm.uiState.value.cantidadPrestados)
        assertEquals(0, vm.uiState.value.cantidadDevueltosHoy)

        // Se devuelve hoy el del lunes.
        dao.prestamos.value = listOf(prestamo(1, fecha(28, 9), devuelto = fecha(30, 15)), prestamo(2, fecha(30, 9)))
        assertEquals(1, vm.uiState.value.cantidadPrestados)
        assertEquals(1, vm.uiState.value.cantidadDevueltosHoy)

        // Se registra uno nuevo: sube Prestados y Devueltos hoy no cambia.
        dao.prestamos.value = dao.prestamos.value + prestamo(3, fecha(30, 16))
        assertEquals(2, vm.uiState.value.cantidadPrestados)
        assertEquals(1, vm.uiState.value.cantidadDevueltosHoy)
    }

    @Test
    fun acceso_adminVeTodoYAdministracion() {
        val rolDao = FakeRolDao(rolesPorAdministrador = mapOf(1L to Rol.ID_ADMIN))
        val s = nuevoVm(rolDao = rolDao, idAdministrador = 1L).uiState.value

        assertTrue(s.acceso.esAdmin)
        assertEquals(Permiso.entries.toSet(), s.acceso.permisos)
    }

    @Test
    fun acceso_personalSoloSusPermisosYSeActualiza() {
        val rolDao = FakeRolDao(
            rolesPorAdministrador = mapOf(2L to Rol.ID_PERSONAL),
            permisosIniciales = Permiso.inicialesPersonal.map { RolPermiso(Rol.ID_PERSONAL, it.name) }.toSet(),
        )
        val vm = nuevoVm(rolDao = rolDao, idAdministrador = 2L)

        assertFalse(vm.uiState.value.acceso.esAdmin)
        assertEquals(Permiso.inicialesPersonal, vm.uiState.value.acceso.permisos)
        assertFalse(vm.uiState.value.acceso.puede(Permiso.REGISTRAR_ALUMNO))

        // El Admin le habilita Registrar Alumnos: Home lo refleja sola.
        rolDao.permisos.value = rolDao.permisos.value + RolPermiso(Rol.ID_PERSONAL, Permiso.REGISTRAR_ALUMNO.name)
        assertTrue(vm.uiState.value.acceso.puede(Permiso.REGISTRAR_ALUMNO))
    }

    @Test
    fun acceso_personalNuncaTieneGestionDePermisos() {
        // Aunque la base tuviera la fila (no debería), no se le otorga.
        val rolDao = FakeRolDao(
            rolesPorAdministrador = mapOf(2L to Rol.ID_PERSONAL),
            permisosIniciales = setOf(RolPermiso(Rol.ID_PERSONAL, Permiso.GESTION_PERMISOS.name)),
        )
        val s = nuevoVm(rolDao = rolDao, idAdministrador = 2L).uiState.value

        assertFalse(s.acceso.esAdmin)
        assertFalse(s.acceso.puede(Permiso.GESTION_PERMISOS))
    }

    @Test
    fun acceso_sinSesion_noPuedeNada() {
        val s = nuevoVm().uiState.value
        assertFalse(s.acceso.esAdmin)
        assertTrue(s.acceso.permisos.isEmpty())
    }
}
