package com.ien.prestamoscomputadoras.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ien.prestamoscomputadoras.data.AppDatabase
import com.ien.prestamoscomputadoras.data.Permiso
import com.ien.prestamoscomputadoras.data.dao.MovimientoReciente
import com.ien.prestamoscomputadoras.data.repository.AccesoUsuario
import com.ien.prestamoscomputadoras.data.repository.HomeRepository
import com.ien.prestamoscomputadoras.data.repository.PermisosRepository
import com.ien.prestamoscomputadoras.util.SesionActual
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Cantidad máxima de ítems en "Actividad reciente". */
private const val LIMITE_ACTIVIDAD_RECIENTE = 5

/** Cada cuánto se revisa si cambió el día (para que "hoy" se renueve pasada la medianoche). */
private const val INTERVALO_CHEQUEO_DIA_MS = 60_000L

/** Estado de un préstamo (lo usa la tarjeta del historial). */
enum class EstadoPrestamo { ACTIVO, DEVUELTO }

/** Qué fue un movimiento de "Actividad reciente": la entrega o la devolución de un equipo. */
enum class TipoMovimiento { PRESTAMO, DEVOLUCION }

/** Un ítem de la lista "Actividad reciente" de la pantalla de inicio. */
data class ActividadReciente(
    val nombreAlumno: String,
    val codigoComputadora: String,
    /** Hora ya formateada para mostrar, ej: "08:30". */
    val hora: String,
    val tipo: TipoMovimiento,
)

/**
 * Estado de la pantalla de inicio.
 *
 * Es inmutable: el ViewModel genera un nuevo [HomeUiState] con [copy] en cada cambio.
 */
data class HomeUiState(
    /** Préstamos en curso (todavía no devueltos), de cualquier fecha. */
    val cantidadPrestados: Int = 0,
    /** Devoluciones registradas hoy. */
    val cantidadDevueltosHoy: Int = 0,
    val actividadReciente: List<ActividadReciente> = emptyList(),
    /**
     * Qué puede hacer el usuario logueado. Hasta que se cargue no tiene permisos: así no
     * aparecen por un instante acciones que después se ocultan.
     */
    val acceso: AccesoUsuario = AccesoUsuario.SIN_ACCESO,
)

/**
 * ViewModel de [com.ien.prestamoscomputadoras.ui.screens.HomeScreen].
 *
 * Expone [uiState] como [StateFlow] y escucha la base (Room) mientras Home está en la pila:
 * - "Prestados": préstamos ACTIVO en este momento, de cualquier fecha (baja al devolverlos).
 * - "Devueltos hoy": préstamos con fecha de devolución de hoy.
 * - "Actividad reciente": últimos préstamos y devoluciones, mezclados, más reciente primero.
 *   Sin quién los gestionó: esa trazabilidad se ve solo en el Historial.
 * - El acceso del usuario logueado: qué acciones rápidas ve y si ve la sección Administración.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val homeRepository: HomeRepository,
    private val permisosRepository: PermisosRepository,
    /** Administrador de la sesión actual (`null` si no hay sesión). */
    private val idAdministrador: Long?,
    /** Reloj inyectable para los tests. */
    private val ahora: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            inicioDeHoy()
                .flatMapLatest { inicioHoy ->
                    val finHoy = rangoSemana(inicioHoy).finHoy
                    combine(
                        homeRepository.activos(),
                        homeRepository.devueltosEntre(inicioHoy, finHoy),
                        homeRepository.movimientosRecientes(LIMITE_ACTIVIDAD_RECIENTE),
                    ) { activos, devueltosHoy, movimientos ->
                        Triple(activos, devueltosHoy, movimientos.map { it.aActividad() })
                    }
                }
                .collect { (activos, devueltosHoy, actividad) ->
                    _uiState.update {
                        it.copy(
                            cantidadPrestados = activos,
                            cantidadDevueltosHoy = devueltosHoy,
                            actividadReciente = actividad,
                        )
                    }
                }
        }
        viewModelScope.launch {
            permisosRepository.accesoDe(idAdministrador).collect { acceso ->
                _uiState.update { it.copy(acceso = acceso) }
            }
        }
    }

    /** Olvida al administrador logueado. La navegación a Login la hace la UI. */
    fun cerrarSesion() {
        SesionActual.administradorId = null
    }

    /** Emite el inicio del día actual, y de nuevo solo cuando cambia (pasada la medianoche). */
    private fun inicioDeHoy(): Flow<Long> = flow {
        while (true) {
            emit(rangoSemana(ahora()).inicioHoy)
            delay(INTERVALO_CHEQUEO_DIA_MS)
        }
    }.distinctUntilChanged()

    companion object {
        /** Crea el ViewModel con sus repositorios (Room) a partir del Application. */
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                val db = AppDatabase.getInstance(app)
                HomeViewModel(
                    homeRepository = HomeRepository(db.homeDao()),
                    permisosRepository = PermisosRepository(db.rolDao()),
                    idAdministrador = SesionActual.administradorId,
                )
            }
        }

        /** Datos de ejemplo para el @Preview de la pantalla. */
        fun datosDePrueba() = HomeUiState(
            cantidadPrestados = 2,
            cantidadDevueltosHoy = 4,
            actividadReciente = listOf(
                ActividadReciente("Juan Pérez", "PC-7", "08:30", TipoMovimiento.PRESTAMO),
                ActividadReciente("María González", "PC-3", "08:15", TipoMovimiento.DEVOLUCION),
                ActividadReciente("Lucas Fernández", "PC-12", "07:50", TipoMovimiento.PRESTAMO),
                ActividadReciente("Sofía Romero", "PC-5", "07:45", TipoMovimiento.DEVOLUCION),
            ),
            acceso = AccesoUsuario(esAdmin = true, permisos = Permiso.entries.toSet()),
        )
    }
}

private fun MovimientoReciente.aActividad() = ActividadReciente(
    nombreAlumno = "$alumnoNombre $alumnoApellido",
    codigoComputadora = codigoComputadora,
    hora = SimpleDateFormat("HH:mm", Locale.US)
        .apply { timeZone = TimeZone.getDefault() }
        .format(Date(fechaMovimiento)),
    tipo = if (tipo == MovimientoReciente.TIPO_DEVOLUCION) TipoMovimiento.DEVOLUCION else TipoMovimiento.PRESTAMO,
)
