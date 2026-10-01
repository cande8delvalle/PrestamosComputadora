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
import com.ien.prestamoscomputadoras.data.repository.AlumnoRepository
import kotlinx.coroutines.launch

const val ERROR_DNI_ALUMNO_DUPLICADO = "Ya existe un alumno registrado con este DNI"

/** DNI: solo dígitos ASCII (sin puntos, guiones, espacios ni letras). */
private val DNI_ALUMNO_REGEX = Regex("^[0-9]+$")

/** Estado de la pantalla "Registrar Alumno". Inmutable: se regenera con [copy]. */
data class RegistrarAlumnoUiState(
    val nombre: String = "",
    val apellido: String = "",
    val dni: String = "",
    /** Mensaje a mostrar debajo de cada campo (`null` = sin error). */
    val errorNombre: String? = null,
    val errorApellido: String? = null,
    val errorDni: String? = null,
    /** `true` mientras se consulta/guarda en la base: el botón queda deshabilitado. */
    val isLoading: Boolean = false,
    /** Cuando es `true`, la pantalla muestra el modal de "Alumno Registrado Correctamente". */
    val mostrarModalExito: Boolean = false,
)

/** ViewModel de [com.ien.prestamoscomputadoras.ui.screens.RegistrarAlumnoScreen]. */
class RegistrarAlumnoViewModel(
    private val alumnoRepository: AlumnoRepository,
) : ViewModel() {

    var uiState by mutableStateOf(RegistrarAlumnoUiState())
        private set

    fun onNombreChange(valor: String) {
        uiState = uiState.copy(nombre = valor, errorNombre = null)
    }

    fun onApellidoChange(valor: String) {
        uiState = uiState.copy(apellido = valor, errorApellido = null)
    }

    fun onDniChange(valor: String) {
        uiState = uiState.copy(dni = valor, errorDni = null)
    }

    /**
     * Valida y guarda. Reglas:
     * 1. Cada campo vacío -> [ERROR_CAMPO_OBLIGATORIO] debajo de ese campo.
     * 2. DNI con algo que no sea dígitos -> [ERROR_DNI_FORMATO] (solo si no está vacío).
     * 3. Si los campos son válidos, se busca el DNI en la base; si ya existe ->
     *    [ERROR_DNI_ALUMNO_DUPLICADO] y no se guarda.
     * 4. Si todo pasa, se inserta el alumno y se muestra el modal de éxito.
     */
    fun onRegistrarClick() {
        val s = uiState
        if (s.isLoading) return

        fun obligatorio(valor: String) = if (valor.isBlank()) ERROR_CAMPO_OBLIGATORIO else null

        val errorNombre = obligatorio(s.nombre)
        val errorApellido = obligatorio(s.apellido)
        val errorDni = obligatorio(s.dni)
            ?: if (DNI_ALUMNO_REGEX.matches(s.dni)) null else ERROR_DNI_FORMATO

        if (errorNombre != null || errorApellido != null || errorDni != null) {
            uiState = s.copy(
                errorNombre = errorNombre,
                errorApellido = errorApellido,
                errorDni = errorDni,
            )
            return
        }

        uiState = s.copy(isLoading = true)
        viewModelScope.launch {
            try {
                if (alumnoRepository.buscarPorDni(s.dni) != null) {
                    uiState = uiState.copy(errorDni = ERROR_DNI_ALUMNO_DUPLICADO)
                    return@launch
                }
                alumnoRepository.insertar(
                    Alumno(nombre = s.nombre.trim(), apellido = s.apellido.trim(), dni = s.dni),
                )
                uiState = uiState.copy(mostrarModalExito = true)
            } catch (e: SQLiteConstraintException) {
                // Solo ocurre si la tabla tiene un índice único sobre dni y otro alta se
                // coló entre la búsqueda y el insert.
                uiState = uiState.copy(errorDni = ERROR_DNI_ALUMNO_DUPLICADO)
            } finally {
                uiState = uiState.copy(isLoading = false)
            }
        }
    }

    /** "Registrar otro alumno": cierra el modal y deja el formulario vacío y sin errores. */
    fun onRegistrarOtroClick() {
        uiState = RegistrarAlumnoUiState()
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
                RegistrarAlumnoViewModel(AlumnoRepository(AppDatabase.getInstance(app).alumnoDao()))
            }
        }
    }
}
