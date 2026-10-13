package com.ien.prestamoscomputadoras.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ien.prestamoscomputadoras.data.Permiso
import com.ien.prestamoscomputadoras.data.repository.AccesoUsuario
import com.ien.prestamoscomputadoras.data.repository.HomeRepository
import com.ien.prestamoscomputadoras.data.repository.PermisosRepository
import com.ien.prestamoscomputadoras.data.repository.SupabaseHomeRepository
import com.ien.prestamoscomputadoras.data.repository.SupabasePermisosRepository
import com.ien.prestamoscomputadoras.data.repository.SupabaseUsuarioRepository
import com.ien.prestamoscomputadoras.data.repository.UsuarioRepository
import com.ien.prestamoscomputadoras.util.SesionActual
import com.ien.prestamoscomputadoras.util.enArgentina
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Cantidad máxima de ítems en "Actividad reciente". */
private const val LIMITE_ACTIVIDAD_RECIENTE = 5

/** Cada cuánto se revisa si cambió el día (para que "hoy" se renueve pasada la medianoche). */
private const val INTERVALO_CHEQUEO_DIA_MS = 60_000L

/**
 * Saludo del header de Home: "Bienvenido, <nombre_usuario>!", o solo "Bienvenido!" si no hay
 * nombre (así nunca aparece un nombre vacío ni "null").
 */
fun saludoHome(nombreUsuario: String?): String =
    nombreUsuario?.trim()?.takeIf { it.isNotEmpty() }?.let { "Bienvenido, $it!" } ?: "Bienvenido!"

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
    val hora: String get() = detalle.horaUltimaActividad

    /** Si [hora] es de la entrega (flecha arriba) o de la devolución (flecha abajo). */
    val evento: EventoHora get() = detalle.eventoUltimaActividad

    /** Estado actual del préstamo. */
    val estado: EstadoPrestamo get() = detalle.estado
}

/**
 * Estado de la pantalla de inicio.
 *
 * Es inmutable: el ViewModel genera un nuevo [HomeUiState] con [copy] en cada cambio.
 */
data class HomeUiState(
    /** Texto del header, ej: "Bienvenido, jperez!". */
    val saludo: String = saludoHome(null),
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
    /** `true` mientras se cargan los datos por primera vez. */
    val cargando: Boolean = true,
    /** Error al leer de la base (p. ej. sin internet). `null` = sin error. */
    val error: String? = null,
)

/**
 * ViewModel de [com.ien.prestamoscomputadoras.ui.screens.HomeScreen].
 *
 * Mientras Home está visible ([onPantallaVisible] / [onPantallaOculta]):
 * - Lee de Supabase "Prestados" y "Devueltos hoy" (función resumen_hoy) y la "Actividad
 *   reciente" (vista historial_prestamos).
 * - Escucha la tabla prestamo con Realtime: si alguien registra o devuelve un préstamo desde
 *   cualquier celular, vuelve a leer. Al ocultarse Home, la suscripción se cierra.
 * - Vuelve a leer pasada la medianoche (cambia el "hoy").
 * - Lee el acceso del usuario logueado: qué acciones rápidas ve y si ve Administración.
 */
class HomeViewModel(
    private val homeRepository: HomeRepository,
    private val permisosRepository: PermisosRepository,
    /** Perfil de la sesión actual (`null` si no hay sesión). */
    private val idUsuario: Long?,
    private val usuarioRepository: UsuarioRepository? = null,
    /** Reloj inyectable para los tests. */
    private val ahora: () -> Instant = Clock.System::now,
    /**
     * nombre_usuario del perfil logueado. Ya está en memoria desde el login (o el Splash), así
     * el saludo aparece completo desde el primer cuadro, sin parpadear.
     */
    nombreUsuario: String? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState(saludo = saludoHome(nombreUsuario)))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    /** Lo que corre solo mientras la pantalla está visible (incluye la suscripción Realtime). */
    private var escucha: Job? = null

    /** Home pasó a estar visible: lee todo y se suscribe a los cambios. */
    fun onPantallaVisible() {
        if (escucha?.isActive == true) return
        escucha = viewModelScope.launch {
            launch {
                permisosRepository.accesoDe(idUsuario).collect { acceso ->
                    _uiState.update { it.copy(acceso = acceso) }
                }
            }
            // flowOf(Unit): la primera lectura. Después, una por cada cambio o cambio de día.
            merge(flowOf(Unit), homeRepository.cambiosEnPrestamos(), cambioDeDia())
                .collectLatest { cargar() }
        }
    }

    /** Home dejó de verse (se abrió otra pantalla o la app pasó a segundo plano). */
    fun onPantallaOculta() {
        escucha?.cancel() // cancela la escucha y con ella la suscripción a Realtime
        escucha = null
    }

    /** Botón "Reintentar" del aviso de error. */
    fun reintentar() {
        viewModelScope.launch { cargar() }
    }

    private suspend fun cargar() {
        val resumen = homeRepository.resumenHoy()
        val recientes = homeRepository.prestamosRecientes(LIMITE_ACTIVIDAD_RECIENTE)
        val error = (resumen.exceptionOrNull() ?: recientes.exceptionOrNull())?.message
        _uiState.update { estado ->
            estado.copy(
                cargando = false,
                error = error,
                cantidadPrestados = resumen.getOrNull()?.prestados?.toInt() ?: estado.cantidadPrestados,
                cantidadDevueltosHoy = resumen.getOrNull()?.devueltosHoy?.toInt() ?: estado.cantidadDevueltosHoy,
                actividadReciente = recientes.getOrNull()?.map { ActividadReciente(it.aTarjeta()) }
                    ?: estado.actividadReciente,
            )
        }
    }

    /** Toque en un ítem de "Actividad reciente": abre el detalle de ese préstamo. */
    fun onActividadClick(actividad: ActividadReciente) {
        _uiState.update { it.copy(detalleAbierto = actividad.detalle) }
    }

    fun onCerrarDetalle() {
        _uiState.update { it.copy(detalleAbierto = null) }
    }

    /** Cierra la sesión (local y en Supabase). La navegación a Login la hace la UI. */
    fun cerrarSesion() {
        SesionActual.usuarioId = null
        SesionActual.nombreUsuario = null
        val repositorio = usuarioRepository ?: return
        // viewModelScope se cancela al salir de Home: el cierre corre en un scope propio.
        SesionActual.scope.launch { repositorio.cerrarSesion() }
    }

    /** Emite (sin valor inicial) cada vez que cambia el día en Argentina. */
    private fun cambioDeDia(): Flow<Unit> = flow {
        while (true) {
            emit(ahora().enArgentina().date)
            delay(INTERVALO_CHEQUEO_DIA_MS)
        }
    }.distinctUntilChanged().drop(1).map { }

    companion object {
        /** Crea el ViewModel con sus repositorios (Supabase). */
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                HomeViewModel(
                    homeRepository = SupabaseHomeRepository(),
                    permisosRepository = SupabasePermisosRepository(),
                    idUsuario = SesionActual.usuarioId,
                    usuarioRepository = SupabaseUsuarioRepository(),
                    nombreUsuario = SesionActual.nombreUsuario,
                )
            }
        }

        /** Datos de ejemplo para el @Preview de la pantalla. */
        fun datosDePrueba() = HomeUiState(
            saludo = saludoHome("jperez"),
            cantidadPrestados = 2,
            cantidadDevueltosHoy = 4,
            actividadReciente = listOf(
                actividadDePrueba("Juan Pérez", "PC-7", "08:30", devuelta = null),
                actividadDePrueba("María González", "PC-3", "07:40", devuelta = "08:15"),
                actividadDePrueba("Lucas Fernández", "PC-12", "07:50", devuelta = null),
                actividadDePrueba("Sofía Romero", "PC-5", "07:10", devuelta = "07:45"),
            ),
            acceso = AccesoUsuario(esAdmin = true, permisos = Permiso.entries.toSet()),
            cargando = false,
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
