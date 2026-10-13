package com.ien.prestamoscomputadoras.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ien.prestamoscomputadoras.data.remote.ErrorDatos
import com.ien.prestamoscomputadoras.data.repository.SupabaseUsuarioRepository
import com.ien.prestamoscomputadoras.data.repository.UsuarioRepository
import com.ien.prestamoscomputadoras.util.SesionActual
import com.ien.prestamoscomputadoras.util.esCorreoValido
import kotlinx.coroutines.launch

const val ERROR_LOGIN_EMAIL_VACIO = "Ingresá tu correo electrónico."
const val ERROR_LOGIN_EMAIL_FORMATO = "El correo electrónico no tiene un formato válido."
const val ERROR_LOGIN_CONTRASENA_VACIA = "Ingresá tu contraseña."
const val ERROR_LOGIN_CREDENCIALES = "Correo o contraseña incorrectos."

/**
 * Estado local de la pantalla de inicio de sesión.
 *
 * Es inmutable: el ViewModel genera un nuevo [LoginUiState] con [copy] en cada cambio.
 */
data class LoginUiState(
    val email: String = "",
    val contrasena: String = "",
    val mostrarContrasena: Boolean = false,
    /** Mensaje de error a mostrar bajo el formulario. `null` = sin error. */
    val error: String? = null,
    /** `true` mientras se verifica la cuenta (evita logins duplicados). */
    val isLoading: Boolean = false,
)

/**
 * ViewModel de [com.ien.prestamoscomputadoras.ui.screens.LoginScreen].
 *
 * Valida el formulario e inicia sesión en Supabase Auth con email y contraseña. Supabase
 * guarda la sesión en el teléfono: la próxima vez que se abra la app, el Splash la restaura.
 */
class LoginViewModel(
    private val usuarioRepository: UsuarioRepository,
) : ViewModel() {

    var uiState by mutableStateOf(LoginUiState())
        private set

    fun onEmailChange(nuevoEmail: String) {
        // Al escribir se limpia el error anterior para no dejarlo "pegado".
        uiState = uiState.copy(email = nuevoEmail, error = null)
    }

    fun onContrasenaChange(nuevaContrasena: String) {
        uiState = uiState.copy(contrasena = nuevaContrasena, error = null)
    }

    fun onToggleMostrarContrasena() {
        uiState = uiState.copy(mostrarContrasena = !uiState.mostrarContrasena)
    }

    /**
     * Valida el formulario e inicia sesión. Si algo no cumple, escribe el mensaje en
     * [LoginUiState.error]. Si las credenciales son correctas, guarda el perfil en
     * [SesionActual] e invoca [onLoginExitoso] (la UI navega a Home).
     */
    fun onLoginClick(onLoginExitoso: () -> Unit) {
        if (uiState.isLoading) return
        val email = uiState.email.trim()
        val contrasena = uiState.contrasena

        val error = when {
            email.isEmpty() -> ERROR_LOGIN_EMAIL_VACIO
            !esCorreoValido(email) -> ERROR_LOGIN_EMAIL_FORMATO
            contrasena.isEmpty() -> ERROR_LOGIN_CONTRASENA_VACIA
            else -> null
        }

        if (error != null) {
            uiState = uiState.copy(error = error)
            return
        }

        uiState = uiState.copy(isLoading = true)
        viewModelScope.launch {
            usuarioRepository.iniciarSesion(email.lowercase(), contrasena)
                .onSuccess { perfil ->
                    uiState = uiState.copy(isLoading = false)
                    SesionActual.usuarioId = perfil.idPerfil
                    onLoginExitoso()
                }
                .onFailure { e ->
                    val mensaje = if (e is ErrorDatos.CredencialesInvalidas) ERROR_LOGIN_CREDENCIALES else e.message
                    uiState = uiState.copy(isLoading = false, error = mensaje)
                }
        }
    }

    companion object {
        /** Crea el ViewModel con su repositorio (Supabase). */
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { LoginViewModel(SupabaseUsuarioRepository()) }
        }
    }
}
