package com.ien.prestamoscomputadoras.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ien.prestamoscomputadoras.data.AppDatabase
import com.ien.prestamoscomputadoras.data.Permiso
import com.ien.prestamoscomputadoras.data.repository.AccesoUsuario
import com.ien.prestamoscomputadoras.data.repository.HomeRepository
import com.ien.prestamoscomputadoras.data.repository.PermisosRepository
import com.ien.prestamoscomputadoras.util.SesionActual
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

/** Estado de un préstamo (lo usan la tarjeta del historial y "Actividad reciente"). */
enum class EstadoPrestamo { ACTIVO, DEVUELTO }

/**
 * Un ítem de la lista "Actividad reciente" de la pantalla de inicio: un préstamo. Guarda la
 * misma [TarjetaHistorial] que el Historial, así al tocarlo se abre el detalle sin volver a
 * consultar la base.
 */
data class ActividadReciente(val detalle: TarjetaHistorial) {
    val nombreAlumno: String get() = detalle.nombreAlumno
    val codigoComputadora: String get() = detalle.codigoComputadora

    /** Hora de lo último que pasó (devolución o entrega), ya formateada, ej: "08:30". */
    val hora: String get() = detalle.horaDevolucion ?: detalle.horaPrestamo

    /** Estado actual del préstamo. */
    val estado: EstadoPrestamo get() = detalle.estado
}

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
    /** Préstamo con el modal de detalle abierto, o `null` si no hay ninguno. */
    val detalleAbierto: TarjetaHistorial? = null,
)

/**
 * ViewModel de [com.ien.prestamoscomputadoras.ui.screens.HomeScreen].
 *
 * Expone [uiState] como [StateFlow] y escucha la base (Room) mientras Home está en la pila:
 * - "Prestados": préstamos ACTIVO en este momento, de cualquier fecha (baja al devolverlos).
 * - "Devueltos hoy": préstamos con fecha de devolución de hoy.
 * - "Actividad reciente": últimos préstamos, uno por fila con su estado actual, ordenados por
 *   lo último que pasó con cada uno (la devolución o, si sigue activo, la entrega). Al tocar
 *   uno se abre el mismo detalle que en el Historial (quién lo gestionó, observaciones, etc.).
 * - El acceso del usuario logueado: qué acciones rápidas ve y si ve la sección Administración.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val homeRepository: HomeRepository,
    private val permisosRepository: PermisosRepository,
    /** Usuario de la sesión actual (`null` si no hay sesión). */
    private val idUsuario: Long?,
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
                        homeRepository.prestamosRecientes(LIMITE_ACTIVIDAD_RECIENTE),
                    ) { activos, devueltosHoy, recientes ->
                        Triple(activos, devueltosHoy, recientes.map { ActividadReciente(it.aTarjeta()) })
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
            permisosRepository.accesoDe(idUsuario).collect { acceso ->
                _uiState.update { it.copy(acceso = acceso) }
            }
        }
    }

    /** Toque en un ítem de "Actividad reciente": abre el detalle de ese préstamo. */
    fun onActividadClick(actividad: ActividadReciente) {
        _uiState.update { it.copy(detalleAbierto = actividad.detalle) }
    }

    fun onCerrarDetalle() {
        _uiState.update { it.copy(detalleAbierto = null) }
    }

    /** Olvida al usuario logueado. La navegación a Login la hace la UI. */
    fun cerrarSesion() {
        SesionActual.usuarioId = null
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
                    idUsuario = SesionActual.usuarioId,
                )
            }
        }

        /** Datos de ejemplo para el @Preview de la pantalla. */
        fun datosDePrueba() = HomeUiState(
            cantidadPrestados = 2,
            cantidadDevueltosHoy = 4,
            actividadReciente = listOf(
                actividadDePrueba("Juan Pérez", "PC-7", "08:30", devuelta = null),
                actividadDePrueba("María González", "PC-3", "07:40", devuelta = "08:15"),
                actividadDePrueba("Lucas Fernández", "PC-12", "07:50", devuelta = null),
                actividadDePrueba("Sofía Romero", "PC-5", "07:10", devuelta = "07:45"),
            ),
            acceso = AccesoUsuario(esAdmin = true, permisos = Permiso.entries.toSet()),
        )

        /** Un ítem de ejemplo para los @Preview. [devuelta] es la hora de devolución, si hubo. */
        fun actividadDePrueba(alumno: String, pc: String, prestada: String, devuelta: String?) =
            ActividadReciente(
                TarjetaHistorial(
                    idPrestamo = 0, nombreAlumno = alumno, dniAlumno = "46.111.222",
                    codigoComputadora = pc, fechaPrestamo = "30/09/2026",
                    horaPrestamo = prestada, horaDevolucion = devuelta,
                    estado = if (devuelta != null) EstadoPrestamo.DEVUELTO else EstadoPrestamo.ACTIVO,
                    observacionEntrega = null, danioDevolucion = null,
                    prestadoPor = "Ana López", devueltoPor = devuelta?.let { "Carlos Díaz" },
                    fechaPrestamoMs = 0,
                ),
            )
    }
}
