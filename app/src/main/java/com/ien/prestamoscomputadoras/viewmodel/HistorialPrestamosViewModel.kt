package com.ien.prestamoscomputadoras.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ien.prestamoscomputadoras.data.AppDatabase
import com.ien.prestamoscomputadoras.data.dao.PrestamoHistorial
import com.ien.prestamoscomputadoras.data.entity.Prestamo
import com.ien.prestamoscomputadoras.data.repository.HistorialRepository
import com.ien.prestamoscomputadoras.util.tieneDanioEnDevolucion
import com.ien.prestamoscomputadoras.util.tieneObservacionAlEntregar
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.launch

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
    /** Administrador que entregó la computadora (no el de la sesión actual). */
    val prestadoPor: String,
    /**
     * Administrador que recibió la devolución. `null` si el préstamo sigue activo (o si se
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
    /** Todos los préstamos de la semana (lunes → hoy), del más reciente al más antiguo. */
    val tarjetasSemana: List<TarjetaHistorial> = emptyList(),
    /** Inicio del día de hoy (epoch ms), límite del filtro "Hoy". */
    val inicioHoy: Long = 0L,
    /** Modal de alerta abierto, o `null` si no hay ninguno. */
    val alertaAbierta: AlertaAbierta? = null,
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
    private val ahora: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    var uiState by mutableStateOf(HistorialPrestamosUiState())
        private set

    init {
        val rango = rangoSemana(ahora())
        uiState = uiState.copy(inicioHoy = rango.inicioHoy)
        viewModelScope.launch {
            val filas = historialRepository.listarEntre(rango.inicioLunes, rango.finHoy)
            uiState = uiState.copy(cargando = false, tarjetasSemana = filas.map { it.aTarjeta() })
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

    companion object {
        /** Crea el ViewModel con su repositorio (Room) a partir del Application. */
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                HistorialPrestamosViewModel(HistorialRepository(AppDatabase.getInstance(app).historialDao()))
            }
        }
    }
}

/** Límites (epoch ms) de los filtros. "Esta semana" = [inicioLunes, finHoy). */
internal data class RangoSemana(val inicioLunes: Long, val inicioHoy: Long, val finHoy: Long)

/** Calcula los límites en la zona horaria del dispositivo. La semana arranca el lunes. */
internal fun rangoSemana(ahoraMs: Long): RangoSemana {
    val cal = Calendar.getInstance().apply {
        timeInMillis = ahoraMs
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val inicioHoy = cal.timeInMillis
    // Días desde el lunes: lunes -> 0, ..., domingo -> 6 (Calendar.SUNDAY = 1, MONDAY = 2).
    val diasDesdeLunes = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7
    cal.add(Calendar.DAY_OF_MONTH, 1)
    val finHoy = cal.timeInMillis
    cal.add(Calendar.DAY_OF_MONTH, -1 - diasDesdeLunes)
    return RangoSemana(inicioLunes = cal.timeInMillis, inicioHoy = inicioHoy, finHoy = finHoy)
}

/**
 * Formatter en la zona horaria del dispositivo. Se crea en cada uso (SimpleDateFormat no es
 * thread-safe) y así además toma la zona vigente si el usuario la cambia.
 */
private fun formatoLocal(patron: String) =
    SimpleDateFormat(patron, Locale.US).apply { timeZone = TimeZone.getDefault() }

private fun PrestamoHistorial.aTarjeta(): TarjetaHistorial {
    val formatoFecha = formatoLocal("dd/MM/yyyy")
    val formatoHora = formatoLocal("HH:mm")
    val devuelto = prestamo.estado == Prestamo.ESTADO_DEVUELTO
    return TarjetaHistorial(
        idPrestamo = prestamo.idPrestamo,
        nombreAlumno = "$alumnoNombre $alumnoApellido",
        dniAlumno = formatearDni(alumnoDni),
        codigoComputadora = codigoComputadora,
        fechaPrestamo = formatoFecha.format(Date(prestamo.fechaPrestamo)),
        horaPrestamo = formatoHora.format(Date(prestamo.fechaPrestamo)),
        horaDevolucion = prestamo.fechaDevolucion
            ?.takeIf { devuelto }
            ?.let { formatoHora.format(Date(it)) },
        estado = if (devuelto) EstadoPrestamo.DEVUELTO else EstadoPrestamo.ACTIVO,
        observacionEntrega = prestamo.observacionesIniciales
            ?.takeIf { tieneObservacionAlEntregar(prestamo) }
            ?.trim(),
        danioDevolucion = revision
            ?.takeIf { tieneDanioEnDevolucion(prestamo, it) }
            ?.let {
                RevisionDevolucion(
                    enciende = it.enciende,
                    pantallaOk = it.pantallaOk,
                    cargador = it.cargador,
                    observaciones = it.observaciones?.trim()?.ifEmpty { null },
                )
            },
        prestadoPor = "$adminPrestamoNombre $adminPrestamoApellido",
        devueltoPor = adminDevolucionNombre
            ?.takeIf { devuelto }
            ?.let { "$it ${adminDevolucionApellido.orEmpty()}".trim() },
        fechaPrestamoMs = prestamo.fechaPrestamo,
    )
}
