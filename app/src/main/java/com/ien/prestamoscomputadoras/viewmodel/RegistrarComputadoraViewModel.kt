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
import com.ien.prestamoscomputadoras.data.entity.Computadora
import com.ien.prestamoscomputadoras.data.repository.ComputadoraRepository
import kotlinx.coroutines.launch

const val ERROR_CODIGO_FORMATO = "El código debe tener el formato LETRAS-NÚMERO, ej: PC-11"
const val ERROR_CODIGO_DUPLICADO = "Ya existe una computadora con ese código"

/** Error cuando el número del código ya lo usa otra computadora (ej. PC-11 y LAB-11). */
fun errorNumeroEnUso(numero: Long, codigoExistente: String) =
    "El número $numero ya lo usa la computadora $codigoExistente"

/**
 * Código de identificación: letras, guion y número (ej. "PC-11"). El número es el id de la
 * computadora, así que tiene que ser distinto para cada una.
 */
private val CODIGO_REGEX = Regex("^[A-Za-z]+-([0-9]+)$")

/** Estado de la pantalla "Registrar Computadora". Inmutable: se regenera con [copy]. */
data class RegistrarComputadoraUiState(
    val codigo: String = "",
    val modelo: String = "",
    /** Mensaje a mostrar debajo del campo código (`null` = sin error). */
    val errorCodigo: String? = null,
    /** `true` mientras se consulta/guarda en la base: el botón queda deshabilitado. */
    val isLoading: Boolean = false,
    /** Cuando es `true`, la pantalla muestra el modal de "Computadora Registrada Correctamente". */
    val mostrarModalExito: Boolean = false,
)

/** ViewModel de [com.ien.prestamoscomputadoras.ui.screens.RegistrarComputadoraScreen]. */
class RegistrarComputadoraViewModel(
    private val computadoraRepository: ComputadoraRepository,
) : ViewModel() {

    var uiState by mutableStateOf(RegistrarComputadoraUiState())
        private set

    fun onCodigoChange(valor: String) {
        uiState = uiState.copy(codigo = valor, errorCodigo = null)
    }

    fun onModeloChange(valor: String) {
        uiState = uiState.copy(modelo = valor)
    }

    /**
     * Valida y guarda. Reglas:
     * 1. Código vacío -> [ERROR_CAMPO_OBLIGATORIO].
     * 2. Código sin formato LETRAS-NÚMERO -> [ERROR_CODIGO_FORMATO].
     * 3. Código ya registrado -> [ERROR_CODIGO_DUPLICADO].
     * 4. Número del código ya usado por otra computadora -> [errorNumeroEnUso].
     * 5. Si todo pasa, se inserta con id = número del código y se muestra el modal.
     */
    fun onRegistrarClick() {
        val s = uiState
        if (s.isLoading) return

        val codigo = s.codigo.trim()
        val numero = CODIGO_REGEX.matchEntire(codigo)?.groupValues?.get(1)?.toLongOrNull()
        val error = when {
            codigo.isEmpty() -> ERROR_CAMPO_OBLIGATORIO
            numero == null -> ERROR_CODIGO_FORMATO
            else -> null
        }
        if (error != null || numero == null) {
            uiState = s.copy(errorCodigo = error)
            return
        }

        uiState = s.copy(isLoading = true)
        viewModelScope.launch {
            try {
                if (computadoraRepository.buscarPorCodigo(codigo) != null) {
                    uiState = uiState.copy(errorCodigo = ERROR_CODIGO_DUPLICADO)
                    return@launch
                }
                computadoraRepository.buscarPorId(numero)?.let { existente ->
                    uiState = uiState.copy(errorCodigo = errorNumeroEnUso(numero, existente.codigo))
                    return@launch
                }
                computadoraRepository.insertar(
                    Computadora(
                        idComputadora = numero,
                        codigo = codigo,
                        modelo = s.modelo.trim().ifEmpty { null },
                    ),
                )
                uiState = uiState.copy(mostrarModalExito = true)
            } catch (e: SQLiteConstraintException) {
                // Otra alta con el mismo código/id se coló entre la búsqueda y el insert.
                uiState = uiState.copy(errorCodigo = ERROR_CODIGO_DUPLICADO)
            } finally {
                uiState = uiState.copy(isLoading = false)
            }
        }
    }

    /** "Registrar otra computadora": cierra el modal y deja el formulario vacío y sin errores. */
    fun onRegistrarOtraClick() {
        uiState = RegistrarComputadoraUiState()
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
                RegistrarComputadoraViewModel(
                    ComputadoraRepository(AppDatabase.getInstance(app).computadoraDao()),
                )
            }
        }
    }
}
