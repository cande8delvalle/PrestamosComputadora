package com.ien.prestamoscomputadoras.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

/** Estado de un préstamo mostrado en la actividad reciente. */
enum class EstadoPrestamo { ACTIVO, DEVUELTO }

/** Un ítem de la lista "Actividad reciente" de la pantalla de inicio. */
data class ActividadReciente(
    val nombreAlumno: String,
    val codigoComputadora: String,
    /** Hora ya formateada para mostrar, ej: "08:30". */
    val hora: String,
    val estado: EstadoPrestamo,
)

/**
 * Estado local de la pantalla de inicio.
 *
 * Es inmutable: el ViewModel genera un nuevo [HomeUiState] con [copy] en cada cambio.
 */
data class HomeUiState(
    val cantidadPrestados: Int = 0,
    val cantidadDevueltosHoy: Int = 0,
    val actividadReciente: List<ActividadReciente> = emptyList(),
)

/**
 * ViewModel de [com.ien.prestamoscomputadoras.ui.screens.HomeScreen].
 *
 * Por ahora expone datos de prueba hardcodeados. No hay conexión a backend ni a Room todavía.
 */
class HomeViewModel : ViewModel() {

    // TODO: conectar con backend
    // Reemplazar estos datos de prueba por los reales (Room): contar préstamos activos,
    // devoluciones del día y traer los últimos movimientos ordenados por hora.
    var uiState by mutableStateOf(datosDePrueba())
        private set

    companion object {
        /** Datos de ejemplo, también usados por el @Preview de la pantalla. */
        fun datosDePrueba() = HomeUiState(
            cantidadPrestados = 2,
            cantidadDevueltosHoy = 4,
            actividadReciente = listOf(
                ActividadReciente("Juan Pérez", "PC-7", "08:30", EstadoPrestamo.ACTIVO),
                ActividadReciente("María González", "PC-3", "08:15", EstadoPrestamo.DEVUELTO),
                ActividadReciente("Lucas Fernández", "PC-12", "07:50", EstadoPrestamo.ACTIVO),
                ActividadReciente("Sofía Romero", "PC-5", "07:45", EstadoPrestamo.DEVUELTO),
            ),
        )
    }
}
