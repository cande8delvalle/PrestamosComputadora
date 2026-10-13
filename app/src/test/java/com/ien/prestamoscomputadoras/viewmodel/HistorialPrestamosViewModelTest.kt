package com.ien.prestamoscomputadoras.viewmodel

import com.ien.prestamoscomputadoras.data.remote.ErrorDatos
import com.ien.prestamoscomputadoras.data.remote.MENSAJE_SIN_CONEXION
import kotlin.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HistorialPrestamosViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // Miércoles 30/09/2026 a las 18:00 (Argentina). El lunes de esa semana es el 28.
    private val ahora = fechaAr(9, 30, 18)

    private fun nuevoVm(repo: FakeHistorialRepository, reloj: Instant = ahora) =
        HistorialPrestamosViewModel(repo, ahora = { reloj })

    @Test
    fun rangoSemana_vaDelLunesAlFinDeHoy_enHoraArgentina() {
        val r = rangoSemana(ahora)
        assertEquals(fechaAr(9, 28, 0), r.inicioLunes)
        assertEquals(fechaAr(9, 30, 0), r.inicioHoy)
        assertEquals(fechaAr(10, 1, 0), r.finHoy)
    }

    @Test
    fun rangoSemana_enLunesYDomingo() {
        val lunes = rangoSemana(fechaAr(9, 28, 9))
        assertEquals(fechaAr(9, 28, 0), lunes.inicioLunes)
        assertEquals(fechaAr(9, 28, 0), lunes.inicioHoy)

        // Domingo 27/09: la semana arrancó el lunes 21.
        assertEquals(fechaAr(9, 21, 0), rangoSemana(fechaAr(9, 27, 9)).inicioLunes)
    }

    @Test
    fun rangoSemana_noDependeDeLaZonaDelServidor() {
        // 30/09 01:30 UTC todavía es 29/09 22:30 en Argentina: "hoy" es el 29.
        val r = rangoSemana(Instant.parse("2026-09-30T01:30:00Z"))
        assertEquals(fechaAr(9, 29, 0), r.inicioHoy)
    }

    @Test
    fun filtros_hoyYEstaSemana() {
        val repo = FakeHistorialRepository(
            listOf(
                filaHistorial(1, fechaAr(9, 25, 10)), // viernes de la semana anterior: nunca aparece
                filaHistorial(2, fechaAr(9, 28, 9)), // lunes
                filaHistorial(3, fechaAr(9, 30, 8)), // hoy
                filaHistorial(4, fechaAr(9, 30, 17)), // hoy, más tarde
            ),
        )
        val vm = nuevoVm(repo)

        assertEquals(fechaAr(9, 28, 0), repo.desde)
        assertFalse(vm.uiState.cargando)

        assertEquals(FiltroHistorial.HOY, vm.uiState.filtro)
        assertEquals(listOf(4L, 3L), vm.uiState.tarjetas.map { it.idPrestamo })

        vm.onFiltroSeleccionado(FiltroHistorial.ESTA_SEMANA)
        assertEquals(listOf(4L, 3L, 2L), vm.uiState.tarjetas.map { it.idPrestamo })

        vm.onFiltroSeleccionado(FiltroHistorial.HOY)
        assertEquals(2, vm.uiState.tarjetas.size)
    }

    @Test
    fun tarjeta_devueltaYActiva() {
        val repo = FakeHistorialRepository(
            listOf(
                filaHistorial(1, fechaAr(9, 30, 9, 30), fechaDevolucion = fechaAr(9, 30, 10, 45), codigo = "PC-7"),
                filaHistorial(2, fechaAr(9, 30, 17), prestadoPor = "Carlos Díaz"),
            ),
        )
        val (activa, devuelta) = nuevoVm(repo).uiState.tarjetas

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
    fun tarjeta_alertas_porSeparado() {
        val repo = FakeHistorialRepository(
            listOf(
                // Activo con observación al entregar: solo el chip ámbar.
                filaHistorial(1, fechaAr(9, 28, 9), observacionesIniciales = " Rayón en la tapa "),
                // Devuelto sin cargador y con observaciones: solo el chip rojo.
                filaHistorial(
                    2, fechaAr(9, 29, 9), fechaDevolucion = fechaAr(9, 29, 11),
                    cargador = false, observacionesDevolucion = "Tecla floja",
                ),
                // Devuelto OK: ningún chip.
                filaHistorial(3, fechaAr(9, 29, 12), fechaDevolucion = fechaAr(9, 29, 13)),
                // Las dos condiciones: los dos chips.
                filaHistorial(
                    4, fechaAr(9, 30, 9), fechaDevolucion = fechaAr(9, 30, 11),
                    observacionesIniciales = "Pantalla rayada", cargador = false,
                ),
                // Devuelto pero sin revisión guardada: ningún chip rojo.
                filaHistorial(
                    5, fechaAr(9, 30, 10), fechaDevolucion = fechaAr(9, 30, 12),
                    enciende = null, pantallaOk = null, cargador = null,
                ),
            ),
        )
        val vm = nuevoVm(repo)
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

        assertNull(porId.getValue(5).danioDevolucion)
    }

    @Test
    fun alerta_abrirYCerrarModal() {
        val vm = nuevoVm(FakeHistorialRepository(listOf(filaHistorial(1, fechaAr(9, 30, 9), observacionesIniciales = "Rayón"))))
        val tarjeta = vm.uiState.tarjetas.single()

        vm.onAlertaClick(tarjeta, TipoAlerta.OBSERVACION_ENTREGA)
        assertEquals(AlertaAbierta(tarjeta, TipoAlerta.OBSERVACION_ENTREGA), vm.uiState.alertaAbierta)

        vm.onCerrarAlerta()
        assertNull(vm.uiState.alertaAbierta)
    }

    @Test
    fun tarjeta_abreYCierraElDetalleSinTocarLaAlerta() {
        val vm = nuevoVm(FakeHistorialRepository(listOf(filaHistorial(1, fechaAr(9, 30, 9), observacionesIniciales = "Rayón"))))
        val tarjeta = vm.uiState.tarjetas.single()

        vm.onTarjetaClick(tarjeta)
        assertEquals(tarjeta, vm.uiState.detalleAbierto)
        assertNull(vm.uiState.alertaAbierta)

        vm.onCerrarDetalle()
        assertNull(vm.uiState.detalleAbierto)
    }

    @Test
    fun horas_siempreEnHoraArgentina_aunqueElCelularTengaOtraZona() {
        val original = java.util.TimeZone.getDefault()
        try {
            java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Europe/Madrid"))
            // 30/09/2026 20:46 UTC = 17:46 en Argentina (UTC-3).
            val utc = Instant.parse("2026-09-30T20:46:00Z")
            val repo = FakeHistorialRepository(
                listOf(filaHistorial(1, utc, fechaDevolucion = Instant.parse("2026-09-30T21:46:00Z"))),
            )
            val tarjeta = nuevoVm(repo, reloj = Instant.parse("2026-09-30T22:46:00Z")).uiState.tarjetas.single()

            assertEquals("17:46", tarjeta.horaPrestamo)
            assertEquals("18:46", tarjeta.horaDevolucion)
            assertEquals("30/09/2026", tarjeta.fechaPrestamo)
        } finally {
            java.util.TimeZone.setDefault(original)
        }
    }

    @Test
    fun sinInternet_muestraErrorYReintentar() {
        val repo = FakeHistorialRepository(listOf(filaHistorial(1, fechaAr(9, 30, 9)))).apply {
            falla = ErrorDatos.SinConexion()
        }
        val vm = nuevoVm(repo)
        assertEquals(MENSAJE_SIN_CONEXION, vm.uiState.error)
        assertFalse(vm.uiState.cargando)
        assertTrue(vm.uiState.tarjetas.isEmpty())

        repo.falla = null
        vm.cargar()
        assertNull(vm.uiState.error)
        assertEquals(1, vm.uiState.tarjetas.size)
    }
}
