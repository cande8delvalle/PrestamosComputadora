package com.ien.prestamoscomputadoras.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ien.prestamoscomputadoras.data.Permiso
import com.ien.prestamoscomputadoras.data.model.Aula
import com.ien.prestamoscomputadoras.data.remote.ErrorDatos
import com.ien.prestamoscomputadoras.data.repository.AulaRepository
import com.ien.prestamoscomputadoras.data.repository.PermisosRepository
import com.ien.prestamoscomputadoras.data.repository.SupabaseAulaRepository
import com.ien.prestamoscomputadoras.data.repository.SupabasePermisosRepository
import com.ien.prestamoscomputadoras.util.SesionActual
import kotlinx.coroutines.launch

/** Estado de la pantalla "Gestionar Aulas". Inmutable: se regenera con [copy]. */
data class GestionarAulasUiState(
    val aulas: List<Aula> = emptyList(),
    /** `true` hasta saber si el usuario es Admin y tener las aulas cargadas. */
    val cargando: Boolean = true,
    /** Solo el Admin puede ver y usar esta pantalla. */
    val puedeGestionar: Boolean = false,
    /** Lo que se está escribiendo en el campo "Nombre del aula". */
    val nombre: String = "",
    val errorNombre: String? = null,
    /** `true` mientras se guarda un aula nueva. */
    val guardando: Boolean = false,
    /** Aula que se está por eliminar (diálogo de confirmación abierto), o `null`. */
    val aulaPorEliminar: Aula? = null,
    /** Error de carga o de eliminar (sin internet, aula con préstamos...). */
    val error: String? = null,
    /** `true` si el error es de la carga inicial: se ofrece "Reintentar". */
    val errorDeCarga: Boolean = false,
)

/**
 * ViewModel de [com.ien.prestamoscomputadoras.ui.screens.GestionarAulasScreen].
 *
 * Además de que Home solo le muestra el acceso al Admin, acá se vuelve a verificar el permiso
 * ([Permiso.GESTIONAR_AULAS], exclusivo del Admin) contra la base antes de dejar usar nada.
 */
class GestionarAulasViewModel(
    private val aulaRepository: AulaRepository,
    private val permisosRepository: PermisosRepository,
    private val idUsuario: Long?,
) : ViewModel() {

    var uiState by mutableStateOf(GestionarAulasUiState())
        private set

    init {
        cargar()
    }

    /** Lee el permiso del usuario y las aulas. También lo usa "Reintentar". */
    fun cargar() {
        uiState = uiState.copy(error = null, errorDeCarga = false)
        viewModelScope.launch {
            val acceso = permisosRepository.accesoDe(idUsuario)
            val puede = acceso.getOrNull()?.puede(Permiso.GESTIONAR_AULAS) ?: uiState.puedeGestionar
            // A quien no puede gestionar no se le piden las aulas (la pantalla no se las muestra).
            val aulas = if (puede) aulaRepository.listar() else null
            val fallo = acceso.exceptionOrNull() ?: aulas?.exceptionOrNull()
            uiState = uiState.copy(
                cargando = false,
                puedeGestionar = puede,
                aulas = aulas?.getOrNull() ?: uiState.aulas,
                error = fallo?.let(::mensajeDeCargaAutomatica),
                errorDeCarga = fallo != null,
            )
        }
    }

    fun onNombreChange(valor: String) {
        uiState = uiState.copy(nombre = valor, errorNombre = null)
    }

    /**
     * Valida y da de alta el aula. Nombre vacío -> [ERROR_CAMPO_OBLIGATORIO]. Si la base dice
     * que el nombre ya existe, el mensaje sale debajo del campo; cualquier otro error, arriba.
     */
    fun onAgregarClick() {
        val s = uiState
        if (!s.puedeGestionar || s.guardando) return

        val nombre = s.nombre.trim()
        if (nombre.isEmpty()) {
            uiState = s.copy(errorNombre = ERROR_CAMPO_OBLIGATORIO)
            return
        }

        uiState = s.copy(guardando = true, errorNombre = null, error = null, errorDeCarga = false)
        viewModelScope.launch {
            aulaRepository.insertar(nombre)
                .onSuccess {
                    uiState = uiState.copy(guardando = false, nombre = "")
                    recargarAulas()
                }
                .onFailure { e ->
                    uiState = if (e is ErrorDatos.Duplicado) {
                        uiState.copy(guardando = false, errorNombre = e.message)
                    } else {
                        uiState.copy(guardando = false, error = e.message)
                    }
                }
        }
    }

    /** Toque en el tacho de [aula]: pide confirmación antes de borrar. */
    fun onEliminarClick(aula: Aula) {
        if (!uiState.puedeGestionar) return
        uiState = uiState.copy(aulaPorEliminar = aula, error = null, errorDeCarga = false)
    }

    fun onCancelarEliminar() {
        uiState = uiState.copy(aulaPorEliminar = null)
    }

    /** "Eliminar" del diálogo. Si el aula ya tuvo préstamos, la base lo impide y se avisa. */
    fun onConfirmarEliminar() {
        val aula = uiState.aulaPorEliminar ?: return
        if (!uiState.puedeGestionar) return
        uiState = uiState.copy(aulaPorEliminar = null)
        viewModelScope.launch {
            aulaRepository.eliminar(aula.idAula)
                .onSuccess { recargarAulas() }
                .onFailure { e -> uiState = uiState.copy(error = e.message, errorDeCarga = false) }
        }
    }

    private suspend fun recargarAulas() {
        aulaRepository.listar()
            .onSuccess { uiState = uiState.copy(aulas = it) }
            .onFailure { e ->
                uiState = uiState.copy(error = mensajeDeCargaAutomatica(e), errorDeCarga = true)
            }
    }

    companion object {
        /** Crea el ViewModel con sus repositorios (Supabase) y el usuario de la sesión actual. */
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                GestionarAulasViewModel(
                    SupabaseAulaRepository(),
                    SupabasePermisosRepository(),
                    idUsuario = SesionActual.usuarioId,
                )
            }
        }
    }
}
