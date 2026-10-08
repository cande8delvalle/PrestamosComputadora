package com.ien.prestamoscomputadoras.viewmodel

import com.ien.prestamoscomputadoras.data.dao.HistorialDao
import com.ien.prestamoscomputadoras.data.dao.PrestamoHistorial
import com.ien.prestamoscomputadoras.data.entity.EstadoComputadora
import com.ien.prestamoscomputadoras.data.entity.Prestamo
import com.ien.prestamoscomputadoras.data.repository.HistorialRepository
import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class HistorialPrestamosViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    /** Epoch ms en la zona horaria del dispositivo (la misma que usa el ViewModel). */
    private fun fecha(dia: Int, hora: Int, minuto: Int = 0): Long =
        Calendar.getInstance().apply {
            clear()
            set(2026, Calendar.SEPTEMBER, dia, hora, minuto)
        }.timeInMillis

    // Miércoles 30/09/2026 a las 18:00. El lunes de esa semana es el 28.
    private val ahora = fecha(30, 18)

    /** Devuelve las filas dentro del rango pedido, ordenadas como la query real. */
    private class FakeHistorialDao(private val filas: List<PrestamoHistorial>) : HistorialDao {
        var desde = 0L
        var hasta = 0L

        override suspend fun listarEntre(desde: Long, hasta: Long): List<PrestamoHistorial> {
            this.desde = desde
            this.hasta = hasta
            return filas
                .filter { it.prestamo.fechaPrestamo in desde until hasta }
                .sortedByDescending { it.prestamo.fechaPrestamo }
        }
    }

    private fun fila(
        id: Long,
        fechaPrestamo: Long,
        fechaDevolucion: Long? = null,
        observacionesIniciales: String? = null,
        revision: EstadoComputadora? = null,
        admin: Pair<String, String> = "Ana" to "López",
        adminDevolucion: Pair<String, String>? = if (fechaDevolucion != null) "Marta" to "Ruiz" else null,
    ) = PrestamoHistorial(
        prestamo = Prestamo(
            idPrestamo = id,
            idAlumno = 1,
            idComputadora = 7,
            idAdministradorPrestamo = 1,
            fechaPrestamo = fechaPrestamo,
            fechaDevolucion = fechaDevolucion,
            idAdministradorDevolucion = if (adminDevolucion != null) 2 else null,
            estado = if (fechaDevolucion != null) Prestamo.ESTADO_DEVUELTO else Prestamo.ESTADO_ACTIVO,
            observacionesIniciales = observacionesIniciales,
        ),
        revision = revision,
        alumnoNombre = "Juan",
        alumnoApellido = "Pérez",
        alumnoDni = "46111222",
        codigoComputadora = "PC-7",
        adminPrestamoNombre = admin.first,
        adminPrestamoApellido = admin.second,
        adminDevolucionNombre = adminDevolucion?.first,
        adminDevolucionApellido = adminDevolucion?.second,
    )

    private fun revision(idPrestamo: Long, cargador: Boolean = true, observaciones: String? = null) =
        EstadoComputadora(
            idPrestamo = idPrestamo,
            enciende = true,
            pantallaOk = true,
            cargador = cargador,
            observaciones = observaciones,
            fechaRevision = 0,
        )

    @Test
    fun rangoSemana_vaDelLunesAlFinDeHoy() {
        val r = rangoSemana(ahora)
        assertEquals(fecha(28, 0), r.inicioLunes)
        assertEquals(fecha(30, 0), r.inicioHoy)
        // Calendar es lenient: "31 de septiembre" = 1 de octubre a las 00:00.
        assertEquals(fecha(31, 0), r.finHoy)
    }

    @Test
    fun rangoSemana_enLunesYDomingo() {
        val lunes = rangoSemana(fecha(28, 9))
        assertEquals(fecha(28, 0), lunes.inicioLunes)
        assertEquals(fecha(28, 0), lunes.inicioHoy)

        // Domingo 27/09: la semana arrancó el lunes 21.
        val domingo = rangoSemana(fecha(27, 9))
        assertEquals(fecha(21, 0), domingo.inicioLunes)
    }

    @Test
    fun filtros_hoyYEstaSemana() {
        val dao = FakeHistorialDao(
            listOf(
                fila(1, fecha(25, 10)), // viernes de la semana anterior: nunca aparece
                fila(2, fecha(28, 9)), // lunes
                fila(3, fecha(30, 8)), // hoy
                fila(4, fecha(30, 17)), // hoy, más tarde
            ),
        )
        val vm = HistorialPrestamosViewModel(HistorialRepository(dao), ahora = { ahora })

        assertEquals(fecha(28, 0), dao.desde)
        assertFalse(vm.uiState.cargando)

        // Por defecto "Hoy", del más reciente al más antiguo.
        assertEquals(FiltroHistorial.HOY, vm.uiState.filtro)
        assertEquals(listOf(4L, 3L), vm.uiState.tarjetas.map { it.idPrestamo })

        vm.onFiltroSeleccionado(FiltroHistorial.ESTA_SEMANA)
        assertEquals(listOf(4L, 3L, 2L), vm.uiState.tarjetas.map { it.idPrestamo })

        vm.onFiltroSeleccionado(FiltroHistorial.HOY)
        assertEquals(2, vm.uiState.tarjetas.size)
    }

    @Test
    fun tarjeta_devueltaYActiva() {
        val dao = FakeHistorialDao(
            listOf(
                fila(1, fecha(30, 9, 30), fechaDevolucion = fecha(30, 10, 45), revision = revision(1)),
                fila(2, fecha(30, 17), admin = "Carlos" to "Díaz"),
            ),
        )
        val vm = HistorialPrestamosViewModel(HistorialRepository(dao), ahora = { ahora })
        val (activa, devuelta) = vm.uiState.tarjetas

        assertEquals(EstadoPrestamo.DEVUELTO, devuelta.estado)
        assertEquals("Juan Pérez", devuelta.nombreAlumno)
        assertEquals("46.111.222", devuelta.dniAlumno)
        assertEquals("PC-7", devuelta.codigoComputadora)
        assertEquals("30/09/2026", devuelta.fechaPrestamo)
        assertEquals("09:30", devuelta.horaPrestamo)
        assertEquals("10:45", devuelta.horaDevolucion)
        assertEquals("Ana López", devuelta.prestadoPor)
        assertEquals("Marta Ruiz", devuelta.devueltoPor)

        assertEquals(EstadoPrestamo.ACTIVO, activa.estado)
        assertEquals("17:00", activa.horaPrestamo)
        assertNull(activa.horaDevolucion)
        assertEquals("Carlos Díaz", activa.prestadoPor)
        assertNull(activa.devueltoPor)
    }

    @Test
    fun tarjeta_devueltaAntesDeV6_sinDevueltoPor() {
        // Devuelta antes de que se guardara quién la recibió: solo se sabe quién la prestó.
        val dao = FakeHistorialDao(
            listOf(fila(1, fecha(30, 9), fechaDevolucion = fecha(30, 10), adminDevolucion = null)),
        )
        val vm = HistorialPrestamosViewModel(HistorialRepository(dao), ahora = { ahora })
        val tarjeta = vm.uiState.tarjetas.single()

        assertEquals(EstadoPrestamo.DEVUELTO, tarjeta.estado)
        assertEquals("Ana López", tarjeta.prestadoPor)
        assertNull(tarjeta.devueltoPor)
    }

    @Test
    fun tarjeta_alertas_porSeparado() {
        val dao = FakeHistorialDao(
            listOf(
                // Activo con observación al entregar: solo el chip ámbar.
                fila(1, fecha(28, 9), observacionesIniciales = " Rayón en la tapa "),
                // Devuelto sin cargador y con observaciones: solo el chip rojo.
                fila(
                    2, fecha(29, 9), fechaDevolucion = fecha(29, 11),
                    revision = revision(2, cargador = false, observaciones = "Tecla floja"),
                ),
                // Devuelto OK: ningún chip.
                fila(3, fecha(29, 12), fechaDevolucion = fecha(29, 13), revision = revision(3)),
                // Las dos condiciones: los dos chips.
                fila(
                    4, fecha(30, 9), fechaDevolucion = fecha(30, 11),
                    observacionesIniciales = "Pantalla rayada", revision = revision(4, cargador = false),
                ),
            ),
        )
        val vm = HistorialPrestamosViewModel(HistorialRepository(dao), ahora = { ahora })
        vm.onFiltroSeleccionado(FiltroHistorial.ESTA_SEMANA)
        val porId = vm.uiState.tarjetas.associateBy { it.idPrestamo }

        assertEquals("Rayón en la tapa", porId.getValue(1).observacionEntrega)
        assertNull(porId.getValue(1).danioDevolucion)

        assertNull(porId.getValue(2).observacionEntrega)
        assertEquals(
            RevisionDevolucion(enciende = true, pantallaOk = true, cargador = false, observaciones = "Tecla floja"),
            porId.getValue(2).danioDevolucion,
        )

        assertNull(porId.getValue(3).observacionEntrega)
        assertNull(porId.getValue(3).danioDevolucion)

        assertEquals("Pantalla rayada", porId.getValue(4).observacionEntrega)
        assertEquals(false, porId.getValue(4).danioDevolucion?.cargador)
        assertNull(porId.getValue(4).danioDevolucion?.observaciones)
    }

    @Test
    fun alerta_abrirYCerrarModal() {
        val dao = FakeHistorialDao(listOf(fila(1, fecha(30, 9), observacionesIniciales = "Rayón")))
        val vm = HistorialPrestamosViewModel(HistorialRepository(dao), ahora = { ahora })
        val tarjeta = vm.uiState.tarjetas.single()

        vm.onAlertaClick(tarjeta, TipoAlerta.OBSERVACION_ENTREGA)
        assertEquals(AlertaAbierta(tarjeta, TipoAlerta.OBSERVACION_ENTREGA), vm.uiState.alertaAbierta)

        vm.onCerrarAlerta()
        assertNull(vm.uiState.alertaAbierta)
    }

    @Test
    fun horas_enZonaHorariaDelDispositivo() {
        val original = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("America/Argentina/Buenos_Aires"))
            // 30/09/2026 20:46 UTC = 17:46 en Argentina (UTC-3).
            val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                clear()
                set(2026, Calendar.SEPTEMBER, 30, 20, 46)
            }.timeInMillis
            val dao = FakeHistorialDao(listOf(fila(1, utc, fechaDevolucion = utc + 60 * 60 * 1000L)))
            val vm = HistorialPrestamosViewModel(HistorialRepository(dao), ahora = { utc + 2 * 60 * 60 * 1000L })

            val tarjeta = vm.uiState.tarjetas.single()
            assertEquals("17:46", tarjeta.horaPrestamo)
            assertEquals("18:46", tarjeta.horaDevolucion)
            assertEquals("30/09/2026", tarjeta.fechaPrestamo)
        } finally {
            TimeZone.setDefault(original)
        }
    }
}
