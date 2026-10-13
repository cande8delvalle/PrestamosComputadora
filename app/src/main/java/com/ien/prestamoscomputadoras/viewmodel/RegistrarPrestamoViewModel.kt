package com.ien.prestamoscomputadoras.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ien.prestamoscomputadoras.data.model.Alumno
import com.ien.prestamoscomputadoras.data.model.Computadora
import com.ien.prestamoscomputadoras.data.model.NuevoPrestamo
import com.ien.prestamoscomputadoras.data.remote.ErrorDatos
import com.ien.prestamoscomputadoras.data.repository.PrestamoRepository
import com.ien.prestamoscomputadoras.data.repository.RESTRICCION_UN_ACTIVO_POR_COMPUTADORA
import com.ien.prestamoscomputadoras.data.repository.SupabasePrestamoRepository
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
    /** `true` si no se pudieron cargar las listas (p. ej. sin internet). */
    val errorListas: Boolean = false,
    /** Aviso en diálogo centrado (p. ej. la computadora la prestó otro celular recién). */
    val aviso: AvisoUi? = null,
) {
    /** Solo si las listas se cargaron bien: un error de red no es "no hay computadoras". */
    val sinComputadorasDisponibles: Boolean
        get() = !cargandoListas && !errorListas && computadorasDisponibles.isEmpty()
}

/** ViewModel de [com.ien.prestamoscomputadoras.ui.screens.RegistrarPrestamoScreen]. */
class RegistrarPrestamoViewModel(
    private val prestamoRepository: PrestamoRepository,
) : ViewModel() {

    var uiState by mutableStateOf(RegistrarPrestamoUiState())
        private set

    init {
        cargarListas()
    }

    /** Lee alumnos y computadoras libres. También lo usa el botón "Reintentar". */
    fun cargarListas() {
        uiState = uiState.copy(cargandoListas = true, errorListas = false, errorGeneral = null)
        viewModelScope.launch {
            val alumnos = prestamoRepository.listarAlumnos()
            val computadoras = prestamoRepository.listarComputadorasDisponibles()
            val error = alumnos.exceptionOrNull() ?: computadoras.exceptionOrNull()
            uiState = uiState.copy(
                alumnos = alumnos.getOrDefault(emptyList()),
                computadorasDisponibles = computadoras.getOrDefault(emptyList()),
                cargandoListas = false,
                errorListas = error != null,
                errorGeneral = error?.message,
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
     * 2. Sin usuario en [SesionActual] -> [ERROR_SIN_SESION] (no se puede guardar el FK).
     * 3. Si todo pasa, se inserta el préstamo ACTIVO (la fecha/hora la pone el servidor) y se
     *    muestra el modal de éxito. Si la base avisa que la computadora ya está prestada (otro
     *    celular la prestó recién), se muestra el diálogo [avisoComputadoraYaPrestada] y se
     *    recargan las listas.
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

        val usuarioId = SesionActual.usuarioId
        if (usuarioId == null) {
            uiState = s.copy(errorGeneral = ERROR_SIN_SESION)
            return
        }

        // Para el aviso, por si otro celular la presta mientras tanto (ej: "PC-07").
        val codigoElegido = s.computadoraSeleccionada!!.codigo
        val prestamo = NuevoPrestamo(
            idAlumno = s.alumnoSeleccionado!!.idAlumno,
            idComputadora = s.computadoraSeleccionada!!.idComputadora,
            idUsuarioPrestamo = usuarioId,
            observacionesIniciales = s.observacionesIniciales.trim().ifEmpty { null },
        )

        uiState = s.copy(isLoading = true, errorGeneral = null)
        viewModelScope.launch {
            prestamoRepository.insertar(prestamo)
                .onSuccess { uiState = uiState.copy(isLoading = false, mostrarModalExito = true) }
                .onFailure { e ->
                    if (e is ErrorDatos.Duplicado && e.restriccion == RESTRICCION_UN_ACTIVO_POR_COMPUTADORA) {
                        uiState = uiState.copy(isLoading = false, computadoraSeleccionada = null)
                        cargarListas()
                        uiState = uiState.copy(aviso = avisoComputadoraYaPrestada(codigoElegido))
                    } else {
                        uiState = uiState.copy(
                            isLoading = false,
                            errorGeneral = if (e is ErrorDatos.Otro) ERROR_GUARDAR_PRESTAMO else e.message,
                        )
                    }
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

    /** "Entendido" del diálogo de aviso. */
    fun onAvisoAceptado() {
        uiState = uiState.copy(aviso = null)
    }

    /** "Volver al inicio": cierra el modal. La navegación a Home la hace la UI. */
    fun onCerrarModalExito() {
        uiState = uiState.copy(mostrarModalExito = false)
    }

    companion object {
        /** Crea el ViewModel con su repositorio (Supabase). */
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { RegistrarPrestamoViewModel(SupabasePrestamoRepository()) }
        }
    }
}

/** "46111222" -> "46.111.222". Solo para mostrar: el dato guardado sigue siendo solo dígitos. */
fun formatearDni(dni: String): String =
    dni.reversed().chunked(3).joinToString(".").reversed()
