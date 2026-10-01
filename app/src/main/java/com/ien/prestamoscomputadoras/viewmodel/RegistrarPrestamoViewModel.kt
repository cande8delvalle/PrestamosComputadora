package com.ien.prestamoscomputadoras.viewmodel

import android.database.sqlite.SQLiteConstraintException
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ien.prestamoscomputadoras.data.AppDatabase
import com.ien.prestamoscomputadoras.data.entity.Alumno
import com.ien.prestamoscomputadoras.data.entity.Computadora
import com.ien.prestamoscomputadoras.data.entity.Prestamo
import com.ien.prestamoscomputadoras.data.repository.PrestamoRepository
import com.ien.prestamoscomputadoras.util.SesionActual
import kotlinx.coroutines.launch

const val ERROR_SELECCIONAR_ALUMNO = "Debés seleccionar un alumno"
const val ERROR_SELECCIONAR_COMPUTADORA = "Debés seleccionar una computadora"
const val ERROR_SIN_SESION = "No hay una sesión iniciada. Volvé a iniciar sesión."
const val ERROR_GUARDAR_PRESTAMO = "No se pudo registrar el préstamo. Intentá de nuevo."

/** Estado de la pantalla "Nuevo Préstamo". Inmutable: se regenera con [copy]. */
data class RegistrarPrestamoUiState(
    val alumnos: List<Alumno> = emptyList(),
    val computadorasDisponibles: List<Computadora> = emptyList(),
    /** `true` hasta que terminan de cargarse las listas (evita mostrar "no hay" antes de tiempo). */
    val cargandoListas: Boolean = true,
    val alumnoSeleccionado: Alumno? = null,
    val computadoraSeleccionada: Computadora? = null,
    val observacionesIniciales: String = "",
    val errorAlumno: String? = null,
    val errorComputadora: String? = null,
    /** Error que no es de un campo puntual (sin sesión, fallo al guardar). */
    val errorGeneral: String? = null,
    val isLoading: Boolean = false,
    val mostrarModalExito: Boolean = false,
) {
    val sinComputadorasDisponibles: Boolean
        get() = !cargandoListas && computadorasDisponibles.isEmpty()
}

/** ViewModel de [com.ien.prestamoscomputadoras.ui.screens.RegistrarPrestamoScreen]. */
class RegistrarPrestamoViewModel(
    private val prestamoRepository: PrestamoRepository,
    /** Reloj del dispositivo (epoch ms) para la fecha del préstamo. Inyectable para los tests. */
    private val ahora: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    var uiState by mutableStateOf(RegistrarPrestamoUiState())
        private set

    init {
        cargarListas()
    }

    private fun cargarListas() {
        viewModelScope.launch {
            val alumnos = prestamoRepository.listarAlumnos()
            val computadoras = prestamoRepository.listarComputadorasDisponibles()
            uiState = uiState.copy(
                alumnos = alumnos,
                computadorasDisponibles = computadoras,
                cargandoListas = false,
            )
        }
    }

    fun onAlumnoSeleccionado(alumno: Alumno) {
        uiState = uiState.copy(alumnoSeleccionado = alumno, errorAlumno = null)
    }

    fun onComputadoraSeleccionada(computadora: Computadora) {
        uiState = uiState.copy(computadoraSeleccionada = computadora, errorComputadora = null)
    }

    fun onObservacionesChange(valor: String) {
        uiState = uiState.copy(observacionesIniciales = valor)
    }

    /**
     * Valida y guarda. Reglas:
     * 1. Sin alumno -> [ERROR_SELECCIONAR_ALUMNO]; sin computadora -> [ERROR_SELECCIONAR_COMPUTADORA].
     *    Se muestran los dos a la vez si faltan ambos.
     * 2. Sin administrador en [SesionActual] -> [ERROR_SIN_SESION] (no se puede guardar el FK).
     * 3. Si todo pasa, se inserta el préstamo ACTIVO con la fecha/hora del dispositivo en
     *    este momento (no es un campo del formulario) y se muestra el modal de éxito.
     */
    fun onRegistrarClick() {
        val s = uiState
        if (s.isLoading || s.sinComputadorasDisponibles) return

        val errorAlumno = if (s.alumnoSeleccionado == null) ERROR_SELECCIONAR_ALUMNO else null
        val errorComputadora =
            if (s.computadoraSeleccionada == null) ERROR_SELECCIONAR_COMPUTADORA else null
        if (errorAlumno != null || errorComputadora != null) {
            uiState = s.copy(errorAlumno = errorAlumno, errorComputadora = errorComputadora, errorGeneral = null)
            return
        }

        val administradorId = SesionActual.administradorId
        if (administradorId == null) {
            uiState = s.copy(errorGeneral = ERROR_SIN_SESION)
            return
        }

        val prestamo = Prestamo(
            idAlumno = s.alumnoSeleccionado!!.idAlumno,
            idComputadora = s.computadoraSeleccionada!!.idComputadora,
            idAdministrador = administradorId,
            fechaPrestamo = ahora(),
            fechaDevolucion = null,
            estado = Prestamo.ESTADO_ACTIVO,
            observacionesIniciales = s.observacionesIniciales.trim().ifEmpty { null },
        )

        uiState = s.copy(isLoading = true, errorGeneral = null)
        viewModelScope.launch {
            try {
                prestamoRepository.insertar(prestamo)
                uiState = uiState.copy(mostrarModalExito = true)
            } catch (e: SQLiteConstraintException) {
                // Algún FK ya no existe (p. ej. se borró la base con el admin logueado).
                uiState = uiState.copy(errorGeneral = ERROR_GUARDAR_PRESTAMO)
            } finally {
                uiState = uiState.copy(isLoading = false)
            }
        }
    }

    /**
     * "Realizar otro Préstamo": cierra el modal, limpia la selección y las observaciones y
     * vuelve a consultar las listas (la computadora recién prestada ya no aparece entre las
     * disponibles).
     */
    fun onRealizarOtroClick() {
        uiState = RegistrarPrestamoUiState()
        cargarListas()
    }

    /** "Volver al inicio": cierra el modal. La navegación a Home la hace la UI. */
    fun onCerrarModalExito() {
        uiState = uiState.copy(mostrarModalExito = false)
    }

    companion object {
        /** Crea el ViewModel con su repositorio (Room) a partir del Application. */
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                val db = AppDatabase.getInstance(app)
                RegistrarPrestamoViewModel(
                    PrestamoRepository(db.prestamoDao(), db.computadoraDao(), db.alumnoDao()),
                )
            }
        }
    }
}

/** "46111222" -> "46.111.222". Solo para mostrar: el dato guardado sigue siendo solo dígitos. */
fun formatearDni(dni: String): String =
    dni.reversed().chunked(3).joinToString(".").reversed()
