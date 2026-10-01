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
import com.ien.prestamoscomputadoras.data.repository.AdministradorRepository
import com.ien.prestamoscomputadoras.util.SesionActual
import kotlinx.coroutines.launch

const val ERROR_LOGIN_CREDENCIALES = "Usuario o contraseña incorrectos."

/**
 * Estado local de la pantalla de inicio de sesión.
 *
 * Es inmutable: el ViewModel genera un nuevo [LoginUiState] con [copy] en cada cambio.
 */
data class LoginUiState(
    val usuario: String = "",
    val contrasena: String = "",
    val mostrarContrasena: Boolean = false,
    /** Mensaje de error a mostrar bajo el formulario. `null` = sin error. */
    val error: String? = null,
    /** `true` mientras se verifica el usuario en la base (evita logins duplicados). */
    val isLoading: Boolean = false,
)

/**
 * ViewModel de [com.ien.prestamoscomputadoras.ui.screens.LoginScreen].
 *
 * Valida el formulario y autentica contra Room (nombre de usuario o email + hash de la
 * contraseña).
 */
class LoginViewModel(
    private val administradorRepository: AdministradorRepository,
) : ViewModel() {

    var uiState by mutableStateOf(LoginUiState())
        private set

    fun onUsuarioChange(nuevoUsuario: String) {
        // Al escribir se limpia el error anterior para no dejarlo "pegado".
        uiState = uiState.copy(usuario = nuevoUsuario, error = null)
    }

    fun onContrasenaChange(nuevaContrasena: String) {
        uiState = uiState.copy(contrasena = nuevaContrasena, error = null)
    }

    fun onToggleMostrarContrasena() {
        uiState = uiState.copy(mostrarContrasena = !uiState.mostrarContrasena)
    }

    /**
     * Valida el formulario y autentica. Si algo no cumple, escribe el mensaje en
     * [LoginUiState.error]. Si las credenciales son correctas, guarda el administrador en
     * [SesionActual] e invoca [onLoginExitoso] (la UI navega a Home).
     */
    fun onLoginClick(onLoginExitoso: () -> Unit) {
        if (uiState.isLoading) return
        val usuario = uiState.usuario.trim()
        val contrasena = uiState.contrasena

        val error = when {
            usuario.isEmpty() || contrasena.isEmpty() ->
                "Completá el usuario y la contraseña."
            else -> null
        }

        if (error != null) {
            uiState = uiState.copy(error = error)
            return
        }

        uiState = uiState.copy(isLoading = true)
        viewModelScope.launch {
            try {
                val administrador = administradorRepository.autenticar(usuario, contrasena)
                if (administrador == null) {
                    uiState = uiState.copy(error = ERROR_LOGIN_CREDENCIALES)
                    return@launch
                }
                SesionActual.administradorId = administrador.idAdministrador
                onLoginExitoso()
            } finally {
                uiState = uiState.copy(isLoading = false)
            }
        }
    }

    companion object {
        /** Crea el ViewModel con su repositorio (Room) a partir del Application. */
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                LoginViewModel(AdministradorRepository(AppDatabase.getInstance(app).administradorDao()))
            }
        }
    }
}
