package com.ien.prestamoscomputadoras.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ien.prestamoscomputadoras.data.repository.SupabaseUsuarioRepository
import com.ien.prestamoscomputadoras.data.repository.UsuarioRepository
import com.ien.prestamoscomputadoras.util.SesionActual
import kotlinx.coroutines.launch

/** A dónde ir cuando termina el Splash. */
enum class DestinoInicial { HOME, BIENVENIDA }

/**
 * ViewModel del Splash. Mientras se ve la animación, se fija si quedó guardada una sesión de
 * Supabase (la app se cerró sin cerrar sesión): si la hay, se va directo a Home.
 */
class SplashViewModel(
    private val usuarioRepository: UsuarioRepository,
) : ViewModel() {

    /** `null` mientras se está verificando la sesión. */
    var destino by mutableStateOf<DestinoInicial?>(null)
        private set

    init {
        viewModelScope.launch {
            // Si falla (p. ej. sin internet) se va a Bienvenida, como si no hubiera sesión.
            val perfil = usuarioRepository.restaurarSesion().getOrNull()
            SesionActual.usuarioId = perfil?.idPerfil
            SesionActual.nombreUsuario = perfil?.nombreUsuario
            destino = if (perfil != null) DestinoInicial.HOME else DestinoInicial.BIENVENIDA
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { SplashViewModel(SupabaseUsuarioRepository()) }
        }
    }
}
