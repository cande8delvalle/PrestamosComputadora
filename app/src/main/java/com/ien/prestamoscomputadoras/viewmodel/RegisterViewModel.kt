package com.ien.prestamoscomputadoras.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

/** Mensajes de error usados por la pantalla de registro. */
const val ERROR_CAMPO_OBLIGATORIO = "Este campo es obligatorio"
const val ERROR_CONTRASENAS_NO_COINCIDEN = "Las contraseñas no coinciden"

/**
 * Errores por campo del formulario de registro.
 *
 * Cada propiedad es el mensaje a mostrar debajo de ESE campo (`null` = sin error).
 * Nunca se combinan dos mensajes en el mismo campo.
 */
data class RegisterFieldErrors(
    val nombre: String? = null,
    val apellido: String? = null,
    val dni: String? = null,
    val correo: String? = null,
    val contrasena: String? = null,
    val repetirContrasena: String? = null,
) {
    val hayAlguno: Boolean
        get() = listOf(nombre, apellido, dni, correo, contrasena, repetirContrasena)
            .any { it != null }
}

/** Estado local de la pantalla de registro. Inmutable: se regenera con [copy]. */
data class RegisterUiState(
    val nombre: String = "",
    val apellido: String = "",
    val dni: String = "",
    val correo: String = "",
    val contrasena: String = "",
    val repetirContrasena: String = "",
    val mostrarContrasena: Boolean = false,
    val mostrarRepetirContrasena: Boolean = false,
    val errores: RegisterFieldErrors = RegisterFieldErrors(),
    /** Cuando es `true`, la pantalla muestra el modal de "Cuenta Creada Correctamente". */
    val mostrarModalExito: Boolean = false,
)

/**
 * ViewModel de [com.ien.prestamoscomputadoras.ui.screens.RegisterScreen].
 *
 * Solo mantiene el estado del formulario y corre validaciones locales.
 * No hay conexión a backend ni a Room todavía.
 */
class RegisterViewModel : ViewModel() {

    var uiState by mutableStateOf(RegisterUiState())
        private set

    fun onNombreChange(valor: String) {
        uiState = uiState.copy(nombre = valor, errores = uiState.errores.copy(nombre = null))
    }

    fun onApellidoChange(valor: String) {
        uiState = uiState.copy(apellido = valor, errores = uiState.errores.copy(apellido = null))
    }

    fun onDniChange(valor: String) {
        uiState = uiState.copy(dni = valor, errores = uiState.errores.copy(dni = null))
    }

    fun onCorreoChange(valor: String) {
        uiState = uiState.copy(correo = valor, errores = uiState.errores.copy(correo = null))
    }

    fun onContrasenaChange(valor: String) {
        uiState = uiState.copy(
            contrasena = valor,
            errores = uiState.errores.copy(
                contrasena = null,
                repetirContrasena = errorCoincidencia(valor, uiState.repetirContrasena),
            ),
        )
    }

    fun onRepetirContrasenaChange(valor: String) {
        uiState = uiState.copy(
            repetirContrasena = valor,
            errores = uiState.errores.copy(
                contrasena = null,
                repetirContrasena = errorCoincidencia(uiState.contrasena, valor),
            ),
        )
    }

    /**
     * Error a mostrar bajo "Repetir Contraseña" mientras se escribe: solo se marca
     * "no coinciden" cuando ambos campos ya tienen contenido y difieren. Si todavía falta
     * completar alguno, no se muestra nada (ese caso lo cubre el obligatorio al enviar).
     */
    private fun errorCoincidencia(contrasena: String, repetir: String): String? =
        if (contrasena.isNotBlank() && repetir.isNotBlank() && contrasena != repetir) {
            ERROR_CONTRASENAS_NO_COINCIDEN
        } else {
            null
        }

    fun onToggleMostrarContrasena() {
        uiState = uiState.copy(mostrarContrasena = !uiState.mostrarContrasena)
    }

    fun onToggleMostrarRepetirContrasena() {
        uiState = uiState.copy(mostrarRepetirContrasena = !uiState.mostrarRepetirContrasena)
    }

    /**
     * Valida el formulario. Reglas:
     * 1. Cada campo vacío -> [ERROR_CAMPO_OBLIGATORIO] debajo de ese campo.
     * 2. Si ambas contraseñas tienen contenido y no coinciden -> [ERROR_CONTRASENAS_NO_COINCIDEN]
     *    debajo de "Repetir Contraseña" (nunca combinado con el de campo obligatorio).
     * 3. Si hay campos vacíos, se prioriza el error de obligatorio; el chequeo de
     *    coincidencia solo corre cuando ambos campos de contraseña tienen contenido.
     * 4. Si todo pasa, se muestra el modal de éxito.
     */
    fun onCrearCuentaClick() {
        val s = uiState

        fun obligatorio(valor: String) = if (valor.isBlank()) ERROR_CAMPO_OBLIGATORIO else null

        var errores = RegisterFieldErrors(
            nombre = obligatorio(s.nombre),
            apellido = obligatorio(s.apellido),
            dni = obligatorio(s.dni),
            correo = obligatorio(s.correo),
            contrasena = obligatorio(s.contrasena),
            repetirContrasena = obligatorio(s.repetirContrasena),
        )

        errorCoincidencia(s.contrasena, s.repetirContrasena)?.let {
            errores = errores.copy(repetirContrasena = it)
        }

        if (errores.hayAlguno) {
            uiState = s.copy(errores = errores)
            return
        }

        // TODO: conectar con backend
        // Acá se guardará el usuario real (servicio remoto / Room). Debería manejar
        // estados de carga/error y, si el alta es exitosa, recién ahí mostrar el modal.
        uiState = s.copy(errores = RegisterFieldErrors(), mostrarModalExito = true)
    }

    /**
     * "Aceptar" en el modal de éxito: cierra el modal. La navegación a Login la hace
     * la UI a través de `onCuentaCreada` (ver AppNavigation).
     */
    fun onAceptarModalExito() {
        uiState = uiState.copy(mostrarModalExito = false)
    }
}
