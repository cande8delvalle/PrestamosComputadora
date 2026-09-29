package com.ien.prestamoscomputadoras.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

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
)

/**
 * ViewModel de [com.ien.prestamoscomputadoras.ui.screens.LoginScreen].
 *
 * Por ahora solo mantiene el estado del formulario y hace validaciones locales.
 * No hay conexión a backend ni a Room todavía.
 */
class LoginViewModel : ViewModel() {

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
     * Valida el formulario. Si algo no cumple, escribe el mensaje en [LoginUiState.error].
     * Si todo está OK, invoca [onLoginExitoso] (la UI navega a Home).
     */
    fun onLoginClick(onLoginExitoso: () -> Unit) {
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

        // TODO: conectar con backend
        // Acá va la llamada real de autenticación (servicio remoto / Room).
        // Debería exponer estados de carga y de resultado en LoginUiState y
        // llamar a onLoginExitoso solo si la autenticación es exitosa.
        onLoginExitoso()
    }
}
