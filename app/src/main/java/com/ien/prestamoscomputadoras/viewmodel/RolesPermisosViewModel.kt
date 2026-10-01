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
import com.ien.prestamoscomputadoras.data.Permiso
import com.ien.prestamoscomputadoras.data.repository.PermisosRepository
import com.ien.prestamoscomputadoras.util.SesionActual
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/** Tabs de la pantalla. */
enum class RolTab(val titulo: String) {
    ADMIN("Admin"),
    PERSONAL("Personal Administrativo"),
}

/** Una tarjeta de permiso tal como se muestra en el tab activo. */
data class PermisoItem(
    val permiso: Permiso,
    val habilitado: Boolean,
    /** `false` en el tab Admin: todo tildado y bloqueado. */
    val editable: Boolean,
)

/** Estado de la pantalla "Roles y Permisos". Inmutable: se regenera con [copy]. */
data class RolesPermisosUiState(
    val tab: RolTab = RolTab.ADMIN,
    /** `true` hasta saber si el usuario es Admin y tener los permisos cargados. */
    val cargando: Boolean = true,
    /** Solo el Admin puede ver y editar esta pantalla. */
    val esAdmin: Boolean = false,
    val permisosPersonal: Set<Permiso> = emptySet(),
) {
    val items: List<PermisoItem>
        get() = when (tab) {
            // El Admin tiene todos los permisos, incluidos los exclusivos, y no se edita.
            RolTab.ADMIN -> Permiso.entries.map { PermisoItem(it, habilitado = true, editable = false) }
            // Los exclusivos del Admin (Gestión de Permisos) ni se ofrecen.
            RolTab.PERSONAL -> Permiso.editables.map {
                PermisoItem(it, habilitado = it in permisosPersonal, editable = true)
            }
        }
}

/**
 * ViewModel de [com.ien.prestamoscomputadoras.ui.screens.RolesPermisosScreen].
 *
 * Además de que Home solo le muestra el acceso al Admin, acá se vuelve a verificar el rol del
 * usuario logueado contra la base: si no es Admin, no se muestra ni se guarda nada.
 */
class RolesPermisosViewModel(
    private val permisosRepository: PermisosRepository,
    private val idAdministrador: Long?,
) : ViewModel() {

    var uiState by mutableStateOf(RolesPermisosUiState())
        private set

    init {
        viewModelScope.launch {
            combine(
                permisosRepository.accesoDe(idAdministrador),
                permisosRepository.permisosPersonal(),
            ) { acceso, permisosPersonal ->
                uiState.copy(cargando = false, esAdmin = acceso.esAdmin, permisosPersonal = permisosPersonal)
            }.collect { uiState = it }
        }
    }

    fun onTabSeleccionado(tab: RolTab) {
        uiState = uiState.copy(tab = tab)
    }

    /**
     * Toque en el círculo de un permiso: lo tilda/destilda para Personal Administrativo y lo
     * guarda. En el tab Admin, con permisos exclusivos del Admin o si el usuario no es Admin,
     * no hace nada. La pantalla se actualiza sola cuando la base vuelve a emitir.
     */
    fun onPermisoClick(permiso: Permiso) {
        val s = uiState
        if (!s.esAdmin || s.tab != RolTab.PERSONAL || permiso.soloAdmin) return
        val habilitar = permiso !in s.permisosPersonal
        viewModelScope.launch {
            permisosRepository.cambiarPermisoPersonal(permiso, habilitar)
        }
    }

    companion object {
        /** Crea el ViewModel con su repositorio (Room) y el usuario de la sesión actual. */
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                RolesPermisosViewModel(
                    PermisosRepository(AppDatabase.getInstance(app).rolDao()),
                    idAdministrador = SesionActual.administradorId,
                )
            }
        }
    }
}
