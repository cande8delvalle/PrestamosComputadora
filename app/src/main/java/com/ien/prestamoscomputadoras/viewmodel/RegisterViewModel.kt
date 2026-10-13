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
import com.ien.prestamoscomputadoras.data.repository.DatosRegistro
import com.ien.prestamoscomputadoras.data.repository.RESTRICCION_NOMBRE_USUARIO
import com.ien.prestamoscomputadoras.data.repository.SupabaseUsuarioRepository
import com.ien.prestamoscomputadoras.data.repository.UsuarioRepository
import kotlinx.coroutines.launch

/** Mensajes de error usados por la pantalla de registro. */
const val ERROR_CAMPO_OBLIGATORIO = "Este campo es obligatorio"
const val ERROR_CONTRASENAS_NO_COINCIDEN = "Las contraseñas no coinciden"
const val ERROR_DNI_FORMATO = "El DNI debe contener solo números, sin puntos"
const val ERROR_CORREO_FORMATO = "Ingresá un correo electrónico válido"
const val ERROR_NOMBRE_USUARIO_EN_USO = "Ese nombre de usuario ya está en uso"
const val ERROR_CORREO_EN_USO = "Ya existe una cuenta con este correo"

/** DNI: solo dígitos ASCII (sin puntos, guiones, espacios ni letras). */
private val DNI_REGEX = Regex("^[0-9]+$")

/** Correo: usuario@dominio.extensión (extensión de al menos 2 letras). */
private val CORREO_REGEX = Regex("""^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\.[A-Za-z0-9-]+)*\.[A-Za-z]{2,}$""")

/**
 * Errores por campo del formulario de registro.
 *
 * Cada propiedad es el mensaje a mostrar debajo de ESE campo (`null` = sin error).
 * Nunca se combinan dos mensajes en el mismo campo.
 */
data class RegisterFieldErrors(
    val nombre: String? = null,
    val apellido: String? = null,
    val nombreUsuario: String? = null,
    val dni: String? = null,
    val correo: String? = null,
    val contrasena: String? = null,
    val repetirContrasena: String? = null,
) {
    val hayAlguno: Boolean
        get() = listOf(nombre, apellido, nombreUsuario, dni, correo, contrasena, repetirContrasena)
            .any { it != null }
}

/** Estado local de la pantalla de registro. Inmutable: se regenera con [copy]. */
data class RegisterUiState(
    val nombre: String = "",
    val apellido: String = "",
    val nombreUsuario: String = "",
    val dni: String = "",
    val correo: String = "",
    val contrasena: String = "",
    val repetirContrasena: String = "",
    val mostrarContrasena: Boolean = false,
    val mostrarRepetirContrasena: Boolean = false,
    val errores: RegisterFieldErrors = RegisterFieldErrors(),
    /** Cuando es `true`, la pantalla muestra el modal de "Cuenta Creada Correctamente". */
    val mostrarModalExito: Boolean = false,
    /** `true` mientras se guarda en la base (evita altas duplicadas por doble toque). */
    val isLoading: Boolean = false,
    /** Error que no es de un campo puntual (sin internet, error del servidor). */
    val errorGeneral: String? = null,
)

/**
 * ViewModel de [com.ien.prestamoscomputadoras.ui.screens.RegisterScreen].
 *
 * Mantiene el estado del formulario, lo valida y crea la cuenta en Supabase Auth (el perfil
 * lo crea un trigger de la base).
 */
class RegisterViewModel(
    private val usuarioRepository: UsuarioRepository,
) : ViewModel() {

    var uiState by mutableStateOf(RegisterUiState())
        private set

    fun onNombreChange(valor: String) {
        uiState = uiState.copy(nombre = valor, errores = uiState.errores.copy(nombre = null))
    }

    fun onApellidoChange(valor: String) {
        uiState = uiState.copy(apellido = valor, errores = uiState.errores.copy(apellido = null))
    }

    fun onNombreUsuarioChange(valor: String) {
        uiState = uiState.copy(
            nombreUsuario = valor,
            errores = uiState.errores.copy(nombreUsuario = null),
        )
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
     * 2. DNI con algo que no sea dígitos -> [ERROR_DNI_FORMATO]; correo sin formato
     *    usuario@dominio.extensión -> [ERROR_CORREO_FORMATO]. Solo si el campo tiene contenido.
     * 3. Si ambas contraseñas tienen contenido y no coinciden -> [ERROR_CONTRASENAS_NO_COINCIDEN]
     *    debajo de "Repetir Contraseña" (nunca combinado con el de campo obligatorio).
     * 4. Si hay campos vacíos, se prioriza el error de obligatorio; los chequeos de formato
     *    y de coincidencia solo corren sobre campos con contenido.
     * 5. Se evalúan TODOS los campos y se muestran todos los errores a la vez.
     * 6. Si todo pasa, se crea la cuenta. Si el nombre de usuario o el correo ya están en uso,
     *    lo avisa la base (sin sesión no se pueden consultar antes) y se muestra debajo del
     *    campo. Si sale bien, se muestra el modal.
     */
    fun onCrearCuentaClick() {
        val s = uiState
        if (s.isLoading) return

        fun obligatorio(valor: String) = if (valor.isBlank()) ERROR_CAMPO_OBLIGATORIO else null

        // Causa del bug: acá solo existían el chequeo de obligatorio y el de coincidencia de
        // contraseñas; las validaciones de formato de DNI y de correo nunca se habían
        // implementado, así que "40.123.456" o "juan" pasaban sin error. Ahora cada campo
        // se evalúa con obligatorio primero y, si tiene contenido, con su formato.
        fun dni(valor: String) = obligatorio(valor)
            ?: if (DNI_REGEX.matches(valor)) null else ERROR_DNI_FORMATO

        fun correo(valor: String) = obligatorio(valor)
            ?: if (CORREO_REGEX.matches(valor.trim())) null else ERROR_CORREO_FORMATO

        var errores = RegisterFieldErrors(
            nombre = obligatorio(s.nombre),
            apellido = obligatorio(s.apellido),
            nombreUsuario = obligatorio(s.nombreUsuario),
            dni = dni(s.dni),
            correo = correo(s.correo),
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

        val nombreUsuario = s.nombreUsuario.trim()
        // El correo se guarda en minúscula: el login por email lo busca así.
        val correo = s.correo.trim().lowercase()

        uiState = s.copy(errores = RegisterFieldErrors(), errorGeneral = null, isLoading = true)
        viewModelScope.launch {
            val datos = DatosRegistro(
                nombre = s.nombre.trim(),
                apellido = s.apellido.trim(),
                nombreUsuario = nombreUsuario,
                dni = s.dni,
                email = correo,
            )
            usuarioRepository.registrar(datos, s.contrasena)
                .onSuccess { uiState = uiState.copy(isLoading = false, mostrarModalExito = true) }
                .onFailure { e ->
                    uiState = when {
                        e is ErrorDatos.Duplicado && e.restriccion == RESTRICCION_NOMBRE_USUARIO ->
                            uiState.copy(errores = RegisterFieldErrors(nombreUsuario = ERROR_NOMBRE_USUARIO_EN_USO))
                        e is ErrorDatos.EmailEnUso ->
                            uiState.copy(errores = RegisterFieldErrors(correo = ERROR_CORREO_EN_USO))
                        e is ErrorDatos.ContrasenaDebil ->
                            uiState.copy(errores = RegisterFieldErrors(contrasena = e.message))
                        else -> uiState.copy(errorGeneral = e.message)
                    }.copy(isLoading = false)
                }
        }
    }

    /**
     * "Aceptar" en el modal de éxito: cierra el modal. La navegación a Login la hace
     * la UI a través de `onCuentaCreada` (ver AppNavigation).
     */
    fun onAceptarModalExito() {
        uiState = uiState.copy(mostrarModalExito = false)
    }

    companion object {
        /** Crea el ViewModel con su repositorio (Supabase). */
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { RegisterViewModel(SupabaseUsuarioRepository()) }
        }
    }
}
