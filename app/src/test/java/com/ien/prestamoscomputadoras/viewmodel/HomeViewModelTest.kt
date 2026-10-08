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
import org.junit.Assert.assertNull
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

    /** Lo entrega el usuario 1; si se devolvió, lo recibe el 2. */
    private fun prestamo(id: Long, prestado: Long, devuelto: Long? = null) = Prestamo(
        idPrestamo = id,
        idAlumno = 1,
        idComputadora = id,
        idUsuarioPrestamo = 1,
        fechaPrestamo = prestado,
        fechaDevolucion = devuelto,
        idUsuarioDevolucion = devuelto?.let { 2 },
        estado = if (devuelto != null) Prestamo.ESTADO_DEVUELTO else Prestamo.ESTADO_ACTIVO,
    )

    private fun nuevoVm(
        dao: FakeHomeDao = FakeHomeDao(),
        rolDao: FakeRolDao = FakeRolDao(),
        idUsuario: Long? = null,
    ) = HomeViewModel(HomeRepository(dao), PermisosRepository(rolDao), idUsuario, ahora = { ahora })

    /** Lo que muestra cada ítem de "Actividad reciente". */
    private data class Item(val alumno: String, val pc: String, val hora: String, val estado: EstadoPrestamo)

    private fun HomeViewModel.items() =
        uiState.value.actividadReciente.map { Item(it.nombreAlumno, it.codigoComputadora, it.hora, it.estado) }

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
    fun actividadReciente_unaFilaPorPrestamoConSuEstadoActual() {
        val dao = FakeHomeDao()
        dao.prestamos.value = listOf(
            prestamo(2, fecha(30, 8, 30)),
            prestamo(3, fecha(29, 9), devuelto = fecha(30, 10, 15)),
            prestamo(4, fecha(30, 9), devuelto = fecha(30, 17, 45)),
            prestamo(5, fecha(30, 12)),
        )
        val vm = nuevoVm(dao)

        // Ordena por lo último que pasó: la devolución si ya se devolvió, si no la entrega.
        assertEquals(
            listOf(
                Item("Juan Pérez", "PC-4", "17:45", EstadoPrestamo.DEVUELTO),
                Item("Juan Pérez", "PC-5", "12:00", EstadoPrestamo.ACTIVO),
                Item("Juan Pérez", "PC-3", "10:15", EstadoPrestamo.DEVUELTO),
                Item("Juan Pérez", "PC-2", "08:30", EstadoPrestamo.ACTIVO),
            ),
            vm.items(),
        )
    }

    @Test
    fun tocarActividad_abreElDetalleDeEsePrestamoYSeCierra() {
        val dao = FakeHomeDao()
        dao.prestamos.value = listOf(
            prestamo(2, fecha(30, 8, 30)),
            prestamo(3, fecha(29, 9), devuelto = fecha(30, 10, 15)),
        )
        val vm = nuevoVm(dao)
        assertNull(vm.uiState.value.detalleAbierto)

        val devuelto = vm.uiState.value.actividadReciente.single { it.codigoComputadora == "PC-3" }
        vm.onActividadClick(devuelto)

        // El mismo detalle que el Historial, armado con lo que Home ya tenía cargado.
        val detalle = vm.uiState.value.detalleAbierto!!
        assertEquals(3L, detalle.idPrestamo)
        assertEquals("46.111.222", detalle.dniAlumno)
        assertEquals("29/09/2026", detalle.fechaPrestamo)
        assertEquals("09:00", detalle.horaPrestamo)
        assertEquals("10:15", detalle.horaDevolucion)
        assertEquals(EstadoPrestamo.DEVUELTO, detalle.estado)
        assertEquals("Ana López", detalle.prestadoPor)
        assertEquals("Marta Ruiz", detalle.devueltoPor)

        vm.onCerrarDetalle()
        assertNull(vm.uiState.value.detalleAbierto)
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

        // Se devuelve: deja de estar activo y la misma fila pasa a DEVUELTO (no se duplica).
        dao.prestamos.value = listOf(prestamo(1, fecha(30, 9), devuelto = fecha(30, 12)))
        assertEquals(0, vm.uiState.value.cantidadPrestados)
        assertEquals(1, vm.uiState.value.cantidadDevueltosHoy)
        assertEquals(listOf(Item("Juan Pérez", "PC-1", "12:00", EstadoPrestamo.DEVUELTO)), vm.items())
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
        val rolDao = FakeRolDao(rolesPorUsuario = mapOf(1L to Rol.ID_ADMIN))
        val s = nuevoVm(rolDao = rolDao, idUsuario = 1L).uiState.value

        assertTrue(s.acceso.esAdmin)
        assertEquals(Permiso.entries.toSet(), s.acceso.permisos)
    }

    @Test
    fun acceso_personalSoloSusPermisosYSeActualiza() {
        val rolDao = FakeRolDao(
            rolesPorUsuario = mapOf(2L to Rol.ID_PERSONAL),
            permisosIniciales = Permiso.inicialesPersonal.map { RolPermiso(Rol.ID_PERSONAL, it.name) }.toSet(),
        )
        val vm = nuevoVm(rolDao = rolDao, idUsuario = 2L)

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
            rolesPorUsuario = mapOf(2L to Rol.ID_PERSONAL),
            permisosIniciales = setOf(RolPermiso(Rol.ID_PERSONAL, Permiso.GESTION_PERMISOS.name)),
        )
        val s = nuevoVm(rolDao = rolDao, idUsuario = 2L).uiState.value

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
