package com.ien.prestamoscomputadoras.viewmodel

import com.ien.prestamoscomputadoras.data.Permiso
import com.ien.prestamoscomputadoras.data.model.Rol
import com.ien.prestamoscomputadoras.data.model.RolPermiso
import com.ien.prestamoscomputadoras.data.remote.ErrorDatos
import com.ien.prestamoscomputadoras.data.remote.MENSAJE_ERROR_CARGA
import com.ien.prestamoscomputadoras.data.remote.MENSAJE_SIN_CONEXION
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // Miércoles 30/09/2026 a las 18:00 (hora de Argentina).
    private val ahora = fechaAr(9, 30, 18)

    private val creados = mutableListOf<HomeViewModel>()

    /** Crea el ViewModel y simula que Home está visible (como hace HomeScreen). */
    private fun nuevoVm(
        home: FakeHomeRepository = FakeHomeRepository(),
        permisos: FakePermisosRepository = FakePermisosRepository(),
        idUsuario: Long? = null,
        usuarios: FakeUsuarioRepository? = null,
        nombreUsuario: String? = null,
    ) = HomeViewModel(home, permisos, idUsuario, usuarios, ahora = { ahora }, nombreUsuario = nombreUsuario)
        .also { creados += it }
        .apply { onPantallaVisible() }

    @After
    fun tearDown() {
        // Corta la escucha (tiene un loop que revisa el cambio de día).
        creados.forEach { it.onPantallaOculta() }
    }

    /** Lo que muestra cada ítem de "Actividad reciente". */
    private data class Item(val alumno: String, val pc: String, val hora: String, val estado: EstadoPrestamo)

    private fun HomeViewModel.items() =
        uiState.value.actividadReciente.map { Item(it.nombreAlumno, it.codigoComputadora, it.hora, it.estado) }

    @Test
    fun saludo_conNombreDeUsuario() {
        assertEquals("Bienvenido, jperez!", nuevoVm(nombreUsuario = "jperez").uiState.value.saludo)
    }

    @Test
    fun saludo_sinNombre_soloBienvenido() {
        // Sin sesión cargada o con nombre vacío: nunca "Bienvenido, null!" ni "Bienvenido, !".
        listOf(null, "", "   ").forEach { nombre ->
            assertEquals("Bienvenido!", nuevoVm(nombreUsuario = nombre).uiState.value.saludo)
        }
    }

    @Test
    fun sinPrestamos_todoEnCero() {
        val s = nuevoVm().uiState.value
        assertEquals(0, s.cantidadPrestados)
        assertEquals(0, s.cantidadDevueltosHoy)
        assertTrue(s.actividadReciente.isEmpty())
        assertFalse(s.cargando)
        assertNull(s.error)
    }

    @Test
    fun contadores_salenDeResumenHoy() {
        val home = FakeHomeRepository().apply {
            filas = listOf(
                filaHistorial(1, fechaAr(9, 28, 9)), // activo desde el lunes: cuenta como prestado
                filaHistorial(2, fechaAr(9, 30, 8)), // activo de hoy
                filaHistorial(3, fechaAr(9, 29, 9), fechaDevolucion = fechaAr(9, 30, 10)), // devuelto hoy
                filaHistorial(4, fechaAr(9, 30, 9), fechaDevolucion = fechaAr(9, 30, 11)), // devuelto hoy
                filaHistorial(5, fechaAr(9, 28, 9), fechaDevolucion = fechaAr(9, 29, 10)), // ayer: no cuenta
            )
        }
        val s = nuevoVm(home).uiState.value

        assertEquals(2, s.cantidadPrestados)
        assertEquals(2, s.cantidadDevueltosHoy)
    }

    @Test
    fun actividadReciente_unaFilaPorPrestamoConSuEstadoActual() {
        val home = FakeHomeRepository().apply {
            filas = listOf(
                filaHistorial(2, fechaAr(9, 30, 8, 30)),
                filaHistorial(3, fechaAr(9, 29, 9), fechaDevolucion = fechaAr(9, 30, 10, 15)),
                filaHistorial(4, fechaAr(9, 30, 9), fechaDevolucion = fechaAr(9, 30, 17, 45)),
                filaHistorial(5, fechaAr(9, 30, 12)),
            )
        }
        val vm = nuevoVm(home)

        // Ordena por lo último que pasó (ultima_actividad): devolución si la hubo, si no la entrega.
        assertEquals(
            listOf(
                Item("Juan Pérez", "PC-4", "17:45", EstadoPrestamo.DEVUELTO),
                Item("Juan Pérez", "PC-5", "12:00", EstadoPrestamo.ACTIVO),
                Item("Juan Pérez", "PC-3", "10:15", EstadoPrestamo.DEVUELTO),
                Item("Juan Pérez", "PC-2", "08:30", EstadoPrestamo.ACTIVO),
            ),
            vm.items(),
        )
        // La flecha de cada fila corresponde al evento de esa hora.
        assertEquals(
            listOf(EventoHora.DEVOLUCION, EventoHora.ENTREGA, EventoHora.DEVOLUCION, EventoHora.ENTREGA),
            vm.uiState.value.actividadReciente.map { it.evento },
        )
    }

    @Test
    fun tocarActividad_abreElDetalleDeEsePrestamoYSeCierra() {
        val home = FakeHomeRepository().apply {
            filas = listOf(
                filaHistorial(2, fechaAr(9, 30, 8, 30)),
                filaHistorial(3, fechaAr(9, 29, 9), fechaDevolucion = fechaAr(9, 30, 10, 15)),
            )
        }
        val vm = nuevoVm(home)
        assertNull(vm.uiState.value.detalleAbierto)

        val devuelto = vm.uiState.value.actividadReciente.single { it.codigoComputadora == "PC-3" }
        vm.onActividadClick(devuelto)

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
        val home = FakeHomeRepository().apply {
            filas = (1L..7L).map { filaHistorial(it, fechaAr(9, 30, 8 + it.toInt())) }
        }
        assertEquals(
            listOf("PC-7", "PC-6", "PC-5", "PC-4", "PC-3"),
            nuevoVm(home).uiState.value.actividadReciente.map { it.codigoComputadora },
        )
    }

    @Test
    fun cambioEnRealtime_vuelveALeer() = runTest {
        val home = FakeHomeRepository()
        val vm = nuevoVm(home)
        assertEquals(0, vm.uiState.value.cantidadPrestados)

        // Otro celular registra un préstamo: llega el aviso de Realtime.
        home.filas = listOf(filaHistorial(1, fechaAr(9, 30, 9)))
        home.avisarCambio()
        assertEquals(1, vm.uiState.value.cantidadPrestados)
        assertEquals(1, vm.uiState.value.actividadReciente.size)

        // Se devuelve: deja de estar activo y la misma fila pasa a DEVUELTO (no se duplica).
        home.filas = listOf(filaHistorial(1, fechaAr(9, 30, 9), fechaDevolucion = fechaAr(9, 30, 12)))
        home.avisarCambio()
        assertEquals(0, vm.uiState.value.cantidadPrestados)
        assertEquals(1, vm.uiState.value.cantidadDevueltosHoy)
        assertEquals(listOf(Item("Juan Pérez", "PC-1", "12:00", EstadoPrestamo.DEVUELTO)), vm.items())

        // runTest comparte el reloj virtual con Main: si la escucha sigue viva, al terminar
        // avanzaría para siempre el chequeo de cambio de día.
        vm.onPantallaOculta()
    }

    @Test
    fun suscripcionRealtime_seCierraAlOcultarseYSeReabreAlVolver() {
        val home = FakeHomeRepository()
        val vm = nuevoVm(home)
        assertEquals(1, home.suscriptos.value)

        vm.onPantallaOculta()
        assertEquals(0, home.suscriptos.value)

        // Al volver a Home se relee todo (aparece lo registrado en otra pantalla).
        home.filas = listOf(filaHistorial(1, fechaAr(9, 30, 9)))
        vm.onPantallaVisible()
        assertEquals(1, home.suscriptos.value)
        assertEquals(1, vm.uiState.value.cantidadPrestados)
    }

    @Test
    fun sinInternet_muestraErrorYReintentarLoLimpia() {
        val home = FakeHomeRepository().apply { falla = ErrorDatos.SinConexion() }
        val vm = nuevoVm(home)
        assertEquals(MENSAJE_SIN_CONEXION, vm.uiState.value.error)
        assertFalse(vm.uiState.value.cargando)

        home.falla = null
        home.filas = listOf(filaHistorial(1, fechaAr(9, 30, 9)))
        vm.reintentar()
        assertNull(vm.uiState.value.error)
        assertEquals(1, vm.uiState.value.cantidadPrestados)
    }

    @Test
    fun acceso_adminVeTodoYAdministracion() {
        val permisos = FakePermisosRepository(rolesPorUsuario = mapOf(1L to Rol.ID_ADMIN))
        val s = nuevoVm(permisos = permisos, idUsuario = 1L).uiState.value

        assertTrue(s.acceso.esAdmin)
        assertEquals(Permiso.entries.toSet(), s.acceso.permisos)
    }

    @Test
    fun acceso_personalSoloSusPermisosYSeActualiza() {
        val permisos = FakePermisosRepository(
            rolesPorUsuario = mapOf(2L to Rol.ID_PERSONAL),
            permisosIniciales = Permiso.inicialesPersonal.map { RolPermiso(Rol.ID_PERSONAL, it.name) }.toSet(),
        )
        val vm = nuevoVm(permisos = permisos, idUsuario = 2L)

        assertFalse(vm.uiState.value.acceso.esAdmin)
        assertEquals(Permiso.inicialesPersonal, vm.uiState.value.acceso.permisos)
        assertFalse(vm.uiState.value.acceso.puede(Permiso.REGISTRAR_ALUMNO))

        // El Admin le habilita Registrar Alumnos: Home lo ve al volver a leer (al volver a la
        // pantalla, con un cambio en Realtime o con Reintentar).
        permisos.permisos.value = permisos.permisos.value + RolPermiso(Rol.ID_PERSONAL, Permiso.REGISTRAR_ALUMNO.name)
        vm.onPantallaOculta()
        vm.onPantallaVisible()
        assertTrue(vm.uiState.value.acceso.puede(Permiso.REGISTRAR_ALUMNO))
    }

    // --- Bug: se perdían los permisos al cambiar de app y volver ---------------------------

    private fun personalConPermisos() = FakePermisosRepository(
        rolesPorUsuario = mapOf(2L to Rol.ID_PERSONAL),
        permisosIniciales = Permiso.inicialesPersonal.map { RolPermiso(Rol.ID_PERSONAL, it.name) }.toSet(),
    )

    @Test
    fun errorDeLectura_noVaciaLosPermisos() {
        val permisos = personalConPermisos()
        val vm = nuevoVm(permisos = permisos, idUsuario = 2L)
        assertEquals(Permiso.inicialesPersonal, vm.uiState.value.acceso.permisos)

        // Sale y vuelve a la app; la lectura falla (sin red / sesión renovándose).
        listOf(ErrorDatos.SinConexion(), ErrorDatos.SesionVencida(), ErrorDatos.Otro()).forEach { error ->
            permisos.fallaLectura = error
            vm.onPantallaOculta()
            vm.onPantallaVisible()

            assertEquals(Permiso.inicialesPersonal, vm.uiState.value.acceso.permisos)
            assertTrue(vm.uiState.value.error != null)
        }
    }

    @Test
    fun reintentar_vuelveAPedirLosPermisosYLimpiaElAviso() {
        val permisos = personalConPermisos().apply { fallaLectura = ErrorDatos.SinConexion() }
        val vm = nuevoVm(permisos = permisos, idUsuario = 2L)
        assertEquals(MENSAJE_SIN_CONEXION, vm.uiState.value.error)
        val lecturasAntes = permisos.lecturas

        permisos.fallaLectura = null
        vm.reintentar()

        assertTrue(permisos.lecturas > lecturasAntes)
        assertNull(vm.uiState.value.error)
        assertEquals(Permiso.inicialesPersonal, vm.uiState.value.acceso.permisos)
    }

    @Test
    fun cargaAutomatica_nuncaDiceSoloElAdminPuede() {
        val permisos = personalConPermisos().apply { fallaLectura = ErrorDatos.SinPermiso() }
        val vm = nuevoVm(permisos = permisos, idUsuario = 2L)
        assertEquals(MENSAJE_ERROR_CARGA, vm.uiState.value.error)
    }

    @Test
    fun home_nuncaEscribeEnRolPermiso() = runTest {
        val permisos = personalConPermisos()
        val home = FakeHomeRepository()
        val vm = nuevoVm(home = home, permisos = permisos, idUsuario = 2L)

        // Todo lo que puede pasar en Home: volver a la app, Realtime, Reintentar, errores.
        repeat(3) {
            vm.onPantallaOculta()
            vm.onPantallaVisible()
        }
        home.avisarCambio()
        permisos.fallaLectura = ErrorDatos.SesionVencida()
        vm.reintentar()
        permisos.fallaLectura = null
        vm.reintentar()
        vm.onPantallaOculta()

        assertEquals(0, permisos.escrituras)
        assertTrue(permisos.lecturas > 0)
    }

    @Test
    fun acceso_personalNuncaTieneGestionDePermisos() {
        val permisos = FakePermisosRepository(
            rolesPorUsuario = mapOf(2L to Rol.ID_PERSONAL),
            permisosIniciales = setOf(RolPermiso(Rol.ID_PERSONAL, Permiso.GESTION_PERMISOS.name)),
        )
        val s = nuevoVm(permisos = permisos, idUsuario = 2L).uiState.value

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
