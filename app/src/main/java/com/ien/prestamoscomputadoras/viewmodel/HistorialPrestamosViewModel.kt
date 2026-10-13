package com.ien.prestamoscomputadoras.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ien.prestamoscomputadoras.data.model.Prestamo
import com.ien.prestamoscomputadoras.data.model.PrestamoHistorial
import com.ien.prestamoscomputadoras.data.repository.HistorialRepository
import com.ien.prestamoscomputadoras.data.repository.SupabaseHistorialRepository
import com.ien.prestamoscomputadoras.util.ZONA_ARGENTINA
import com.ien.prestamoscomputadoras.util.enArgentina
import com.ien.prestamoscomputadoras.util.fechaArgentina
import com.ien.prestamoscomputadoras.util.horaArgentina
import com.ien.prestamoscomputadoras.util.tieneDanioEnDevolucion
import com.ien.prestamoscomputadoras.util.tieneObservacionAlEntregar
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/** Chips de filtro del historial. Se aplican sobre la fecha del préstamo. */
enum class FiltroHistorial { HOY, ESTA_SEMANA }

/** Revisión de un préstamo devuelto, para el modal "Daño en la devolución". */
data class RevisionDevolucion(
    val enciende: Boolean,
    val pantallaOk: Boolean,
    val cargador: Boolean,
    /** Observaciones de la devolución, o `null` si no se cargaron. */
    val observaciones: String?,
)

/** Los dos chips/modales de alerta que puede tener una tarjeta. */
enum class TipoAlerta { OBSERVACION_ENTREGA, DANIO_DEVOLUCION }

/** Modal de alerta abierto: de qué tarjeta y de qué tipo. */
data class AlertaAbierta(val tarjeta: TarjetaHistorial, val tipo: TipoAlerta)

/** Una tarjeta del historial, con los textos ya listos para mostrar. */
data class TarjetaHistorial(
    val idPrestamo: Long,
    val nombreAlumno: String,
    /** DNI ya formateado, ej: "46.111.222". */
    val dniAlumno: String,
    val codigoComputadora: String,
    /** "24/06/2026". */
    val fechaPrestamo: String,
    /** "09:30". */
    val horaPrestamo: String,
    /** "10:45", o `null` si el préstamo sigue activo. */
    val horaDevolucion: String?,
    val estado: EstadoPrestamo,
    /**
     * Texto de las observaciones al entregar. Si no es `null` se muestra el chip
     * "Observación al entregar" (ver [tieneObservacionAlEntregar]).
     */
    val observacionEntrega: String?,
    /**
     * Revisión de la devolución, solo si marcó algún problema: si no es `null` se muestra el
     * chip "Daño en la devolución" (ver [tieneDanioEnDevolucion]).
     */
    val danioDevolucion: RevisionDevolucion?,
    /** Usuario que entregó la computadora (no el de la sesión actual). */
    val prestadoPor: String,
    /**
     * Usuario que recibió la devolución. `null` si el préstamo sigue activo (o si se
     * devolvió antes de que se guardara este dato).
     */
    val devueltoPor: String?,
    /** Epoch ms del préstamo, para aplicar el filtro. */
    val fechaPrestamoMs: Long,
)

/** Estado de la pantalla "Historial de Préstamos". Inmutable: se regenera con [copy]. */
data class HistorialPrestamosUiState(
    val filtro: FiltroHistorial = FiltroHistorial.HOY,
    val cargando: Boolean = true,
    /** Error al leer de la base (p. ej. sin internet). `null` = sin error. */
    val error: String? = null,
    /** Todos los préstamos de la semana (lunes → hoy), del más reciente al más antiguo. */
    val tarjetasSemana: List<TarjetaHistorial> = emptyList(),
    /** Inicio del día de hoy (epoch ms), límite del filtro "Hoy". */
    val inicioHoy: Long = 0L,
    /** Modal de alerta abierto, o `null` si no hay ninguno. */
    val alertaAbierta: AlertaAbierta? = null,
    /** Tarjeta con el modal de detalle abierto, o `null` si no hay ninguno. */
    val detalleAbierto: TarjetaHistorial? = null,
) {
    /** Tarjetas del filtro activo. Su tamaño es el contador "X Registros". */
    val tarjetas: List<TarjetaHistorial>
        get() = when (filtro) {
            FiltroHistorial.HOY -> tarjetasSemana.filter { it.fechaPrestamoMs >= inicioHoy }
            FiltroHistorial.ESTA_SEMANA -> tarjetasSemana
        }
}

/**
 * ViewModel de [com.ien.prestamoscomputadoras.ui.screens.HistorialPrestamosScreen].
 *
 * Carga una sola vez los préstamos de la semana: "Hoy" es un subconjunto (hoy siempre cae
 * entre el lunes y hoy), así que cambiar de chip filtra en memoria sin volver a la base.
 */
class HistorialPrestamosViewModel(
    private val historialRepository: HistorialRepository,
    /** Reloj inyectable para los tests. */
    private val ahora: () -> Instant = Clock.System::now,
) : ViewModel() {

    var uiState by mutableStateOf(HistorialPrestamosUiState())
        private set

    init {
        cargar()
    }

    /** Lee de la base los préstamos de la semana. También lo usa el botón "Reintentar". */
    fun cargar() {
        val rango = rangoSemana(ahora())
        uiState = uiState.copy(inicioHoy = rango.inicioHoy.toEpochMilliseconds(), cargando = true, error = null)
        viewModelScope.launch {
            historialRepository.listarEntre(rango.inicioLunes, rango.finHoy)
                .onSuccess { filas ->
                    uiState = uiState.copy(cargando = false, tarjetasSemana = filas.map { it.aTarjeta() })
                }
                .onFailure { e ->
                    uiState = uiState.copy(cargando = false, error = e.message)
                }
        }
    }

    fun onFiltroSeleccionado(filtro: FiltroHistorial) {
        uiState = uiState.copy(filtro = filtro)
    }

    /** Toque en un chip de alerta de [tarjeta]: abre su modal. */
    fun onAlertaClick(tarjeta: TarjetaHistorial, tipo: TipoAlerta) {
        uiState = uiState.copy(alertaAbierta = AlertaAbierta(tarjeta, tipo))
    }

    /** "Entendido", deslizar la hoja hacia abajo o tocar afuera. */
    fun onCerrarAlerta() {
        uiState = uiState.copy(alertaAbierta = null)
    }

    /** Toque en la tarjeta (fuera de los chips): abre el detalle completo del préstamo. */
    fun onTarjetaClick(tarjeta: TarjetaHistorial) {
        uiState = uiState.copy(detalleAbierto = tarjeta)
    }

    fun onCerrarDetalle() {
        uiState = uiState.copy(detalleAbierto = null)
    }

    companion object {
        /** Crea el ViewModel con su repositorio (Supabase). */
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { HistorialPrestamosViewModel(SupabaseHistorialRepository()) }
        }
    }
}

/** Límites de los filtros. "Esta semana" = [inicioLunes, finHoy). */
internal data class RangoSemana(val inicioLunes: Instant, val inicioHoy: Instant, val finHoy: Instant)

/**
 * Calcula los límites en hora de Argentina (no en la del celular ni la del servidor).
 * La semana arranca el lunes.
 */
internal fun rangoSemana(ahora: Instant): RangoSemana {
    val hoy = ahora.enArgentina().date
    // Días desde el lunes: lunes -> 0, ..., domingo -> 6 (isoDayNumber: lunes = 1).
    val lunes = hoy.minus(DatePeriod(days = hoy.dayOfWeek.isoDayNumber - 1))
    return RangoSemana(
        inicioLunes = lunes.atStartOfDayIn(ZONA_ARGENTINA),
        inicioHoy = hoy.atStartOfDayIn(ZONA_ARGENTINA),
        finHoy = hoy.plus(DatePeriod(days = 1)).atStartOfDayIn(ZONA_ARGENTINA),
    )
}

/** Arma la tarjeta (y el detalle) de un préstamo. La usan el Historial y "Actividad reciente". */
internal fun PrestamoHistorial.aTarjeta(): TarjetaHistorial {
    val devuelto = estado == Prestamo.ESTADO_DEVUELTO
    return TarjetaHistorial(
        idPrestamo = idPrestamo,
        nombreAlumno = "$alumnoNombre $alumnoApellido",
        dniAlumno = formatearDni(alumnoDni),
        codigoComputadora = computadoraCodigo,
        fechaPrestamo = fechaPrestamo.fechaArgentina(),
        horaPrestamo = fechaPrestamo.horaArgentina(),
        horaDevolucion = fechaDevolucion?.takeIf { devuelto }?.horaArgentina(),
        estado = if (devuelto) EstadoPrestamo.DEVUELTO else EstadoPrestamo.ACTIVO,
        observacionEntrega = observacionesIniciales
            ?.takeIf { tieneObservacionAlEntregar(this) }
            ?.trim(),
        danioDevolucion = if (tieneDanioEnDevolucion(this)) {
            RevisionDevolucion(
                enciende = enciende ?: true,
                pantallaOk = pantallaOk ?: true,
                cargador = cargador ?: true,
                observaciones = observacionesDevolucion?.trim()?.ifEmpty { null },
            )
        } else {
            null
        },
        prestadoPor = prestadoPor,
        devueltoPor = devueltoPor?.takeIf { devuelto }?.trim(),
        fechaPrestamoMs = fechaPrestamo.toEpochMilliseconds(),
    )
}
