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
import kotlinx.coroutines.launch

const val ERROR_LOGIN_CREDENCIALES = "Usuario o contraseña incorrectos."
const val ERROR_LOGIN_EMAIL = "Ingresá tu correo electrónico (con el nombre de usuario ya no se puede entrar)."

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
 * Valida el formulario e inicia sesión en Supabase Auth con email y contraseña. Supabase
 * guarda la sesión en el teléfono: la próxima vez que se abra la app, el Splash la restaura.
 */
class LoginViewModel(
    private val usuarioRepository: UsuarioRepository,
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
     * [LoginUiState.error]. Si las credenciales son correctas, guarda el perfil en
     * [SesionActual] e invoca [onLoginExitoso] (la UI navega a Home).
     */
    fun onLoginClick(onLoginExitoso: () -> Unit) {
        if (uiState.isLoading) return
        val usuario = uiState.usuario.trim()
        val contrasena = uiState.contrasena

        val error = when {
            usuario.isEmpty() || contrasena.isEmpty() ->
                "Completá el usuario y la contraseña."
            // Supabase Auth entra con email. Buscar el email a partir del nombre de usuario
            // no se puede: sin sesión, la base no deja leer la tabla perfil (RLS).
            "@" !in usuario -> ERROR_LOGIN_EMAIL
            else -> null
        }

        if (error != null) {
            uiState = uiState.copy(error = error)
            return
        }

        uiState = uiState.copy(isLoading = true)
        viewModelScope.launch {
            usuarioRepository.iniciarSesion(usuario.lowercase(), contrasena)
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
