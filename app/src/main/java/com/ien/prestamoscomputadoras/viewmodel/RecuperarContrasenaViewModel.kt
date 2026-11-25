package com.ien.prestamoscomputadoras.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ien.prestamoscomputadoras.data.repository.RecuperacionContrasenaRepository
import com.ien.prestamoscomputadoras.data.repository.SupabaseRecuperacionContrasenaRepository
import com.ien.prestamoscomputadoras.util.SesionActual
import com.ien.prestamoscomputadoras.util.esCorreoValido
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

const val ERROR_RECUPERAR_EMAIL_VACIO = "Ingresá tu correo electrónico."
const val ERROR_RECUPERAR_EMAIL_FORMATO = "El correo electrónico no tiene un formato válido."
const val ERROR_RECUPERAR_CODIGO_INCOMPLETO = "Ingresá los 6 dígitos del código."
const val ERROR_RECUPERAR_CONTRASENA_CORTA = "La contraseña debe tener al menos 8 caracteres."
const val ERROR_RECUPERAR_CONTRASENAS_DISTINTAS = "Las contraseñas no coinciden."
const val AVISO_CODIGO_ENVIADO = "Si el correo está registrado, te enviamos un código."
const val MENSAJE_CONTRASENA_ACTUALIZADA = "Contraseña actualizada. Ya podés iniciar sesión."

const val LARGO_CODIGO = 6
const val LARGO_MINIMO_CONTRASENA = 8
const val ESPERA_REENVIO_SEGUNDOS = 60

/**
 * Estado del flujo "Olvidé mi contraseña" (3 pantallas que comparten este estado, por eso el
 * correo se conserva al ir y volver).
 */
data class RecuperarContrasenaUiState(
    val email: String = "",
    val codigo: String = "",
    val nuevaContrasena: String = "",
    val repetirContrasena: String = "",
    val mostrarContrasena: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    /** Mensaje informativo (no es un error): "Si el correo está registrado, ...". */
    val aviso: String? = null,
    /** Segundos que faltan para poder pedir otro código. 0 = ya se puede. */
    val segundosEspera: Int = 0,
    /** Terminó bien: la contraseña se cambió y la sesión se cerró. */
    val exito: Boolean = false,
)

/**
 * ViewModel del flujo de recuperación de contraseña: correo → código → contraseña nueva.
 *
 * IMPORTANTE: verificar el código abre una sesión en Supabase. La app solo entra a Home desde
 * el login (y desde el Splash al reabrir con una sesión guardada), nunca por observar la
 * sesión, así que no navega sola. Si el usuario abandona el flujo con el código ya verificado
 * (y sin cambiar la contraseña), esa sesión se cierra en [descartarSesion] / [onCleared].
 *
 * [reloj] es la pausa de 1 segundo del contador y [scopeDescarte] el scope donde se cierra la
 * sesión al abandonar el flujo (los dos se reemplazan en los tests).
 */
class RecuperarContrasenaViewModel(
    private val repo: RecuperacionContrasenaRepository,
    private val reloj: suspend () -> Unit = { delay(1_000) },
    private val scopeDescarte: CoroutineScope = SesionActual.scope,
) : ViewModel() {

    var uiState by mutableStateOf(RecuperarContrasenaUiState())
        private set

    private var contador: Job? = null

    /** Correo al que se pidió el último código (para no reenviar si no cambió). */
    private var correoEnviado: String? = null

    /** `true` entre verificar el código y cambiar la contraseña: hay una sesión abierta. */
    private var sesionDeRecuperacionAbierta = false

    fun onEmailChange(valor: String) {
        uiState = uiState.copy(email = valor, error = null)
    }

    fun onCodigoChange(valor: String) {
        // Solo dígitos, hasta 6 (el teclado numérico igual deja pegar texto).
        uiState = uiState.copy(codigo = valor.filter { it.isDigit() }.take(LARGO_CODIGO), error = null)
    }

    fun onNuevaContrasenaChange(valor: String) {
        uiState = uiState.copy(nuevaContrasena = valor, error = null)
    }

    fun onRepetirContrasenaChange(valor: String) {
        uiState = uiState.copy(repetirContrasena = valor, error = null)
    }

    fun onToggleMostrarContrasena() {
        uiState = uiState.copy(mostrarContrasena = !uiState.mostrarContrasena)
    }

    /**
     * Pantalla 1: valida el correo y pide el código. Si ya se pidió uno a este mismo correo
     * hace menos de 60 s, no se pide otro: se pasa directo a la pantalla del código.
     */
    fun onContinuarCorreo(onCodigoEnviado: () -> Unit) {
        if (uiState.isLoading) return
        val email = uiState.email.trim()
        val error = when {
            email.isEmpty() -> ERROR_RECUPERAR_EMAIL_VACIO
            !esCorreoValido(email) -> ERROR_RECUPERAR_EMAIL_FORMATO
            else -> null
        }
        if (error != null) {
            uiState = uiState.copy(error = error)
            return
        }
        if (uiState.segundosEspera > 0 && correoEnviado == email.lowercase()) {
            onCodigoEnviado()
            return
        }
        pedirCodigo(email.lowercase(), onCodigoEnviado)
    }

    /** Pantalla 2: "Obtener un código nuevo" (solo cuando terminó la espera). */
    fun onReenviarCodigo() {
        if (uiState.isLoading || uiState.segundosEspera > 0) return
        pedirCodigo(uiState.email.trim().lowercase()) {}
    }

    private fun pedirCodigo(email: String, onEnviado: () -> Unit) {
        uiState = uiState.copy(isLoading = true, error = null)
        viewModelScope.launch {
            repo.solicitarCodigo(email)
                .onSuccess {
                    correoEnviado = email
                    uiState = uiState.copy(isLoading = false, aviso = AVISO_CODIGO_ENVIADO, codigo = "")
                    iniciarContador()
                    onEnviado()
                }
                .onFailure { e -> uiState = uiState.copy(isLoading = false, error = e.message) }
        }
    }

    private fun iniciarContador() {
        contador?.cancel()
        uiState = uiState.copy(segundosEspera = ESPERA_REENVIO_SEGUNDOS)
        contador = viewModelScope.launch {
            while (uiState.segundosEspera > 0) {
                reloj()
                uiState = uiState.copy(segundosEspera = uiState.segundosEspera - 1)
            }
        }
    }

    /** Pantalla 2: verifica el código de 6 dígitos. */
    fun onContinuarCodigo(onCodigoVerificado: () -> Unit) {
        if (uiState.isLoading) return
        if (uiState.codigo.length != LARGO_CODIGO) {
            uiState = uiState.copy(error = ERROR_RECUPERAR_CODIGO_INCOMPLETO)
            return
        }
        uiState = uiState.copy(isLoading = true, error = null)
        viewModelScope.launch {
            repo.verificarCodigo(uiState.email.trim().lowercase(), uiState.codigo)
                .onSuccess {
                    sesionDeRecuperacionAbierta = true
                    uiState = uiState.copy(isLoading = false, aviso = null)
                    onCodigoVerificado()
                }
                .onFailure { e -> uiState = uiState.copy(isLoading = false, error = e.message) }
        }
    }

    /** Pantalla 3: valida las contraseñas, la cambia y cierra la sesión de recuperación. */
    fun onGuardarContrasena(onTerminado: () -> Unit) {
        if (uiState.isLoading) return
        val nueva = uiState.nuevaContrasena
        val error = when {
            nueva.length < LARGO_MINIMO_CONTRASENA -> ERROR_RECUPERAR_CONTRASENA_CORTA
            nueva != uiState.repetirContrasena -> ERROR_RECUPERAR_CONTRASENAS_DISTINTAS
            else -> null
        }
        if (error != null) {
            uiState = uiState.copy(error = error)
            return
        }
        uiState = uiState.copy(isLoading = true, error = null)
        viewModelScope.launch {
            repo.cambiarContrasena(nueva)
                .onSuccess {
                    sesionDeRecuperacionAbierta = false
                    // Que el login muestre "Contraseña actualizada" (una sola vez).
                    SesionActual.avisarContrasenaActualizada()
                    uiState = uiState.copy(
                        isLoading = false,
                        exito = true,
                        nuevaContrasena = "",
                        repetirContrasena = "",
                    )
                    onTerminado()
                }
                .onFailure { e -> uiState = uiState.copy(isLoading = false, error = e.message) }
        }
    }

    /** El usuario abandona el flujo (flecha atrás en la pantalla 3, etc.). */
    fun descartarSesion() {
        if (!sesionDeRecuperacionAbierta) return
        sesionDeRecuperacionAbierta = false
        // En el scope de la app: el ViewModel puede estar destruyéndose.
        scopeDescarte.launch { repo.descartarSesion() }
    }

    override fun onCleared() {
        descartarSesion()
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { RecuperarContrasenaViewModel(SupabaseRecuperacionContrasenaRepository()) }
        }
    }
}
