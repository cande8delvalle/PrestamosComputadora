package com.ien.prestamoscomputadoras.data.remote

import io.github.jan.supabase.auth.exception.AuthErrorCode
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.auth.exception.AuthSessionMissingException
import io.github.jan.supabase.auth.exception.SessionRequiredException
import io.github.jan.supabase.auth.exception.TokenExpiredException
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.exception.PostgrestRestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

const val MENSAJE_SIN_CONEXION = "No hay conexión a internet. Revisá el Wi-Fi o los datos e intentá de nuevo."
const val MENSAJE_SIN_PERMISO = "No tenés permiso para hacer esto. Solo el Admin puede."
const val MENSAJE_SESION_VENCIDA = "Tu sesión venció. Volvé a ingresar."
/** Para cargas automáticas (Home, listas): nunca dice "Solo el Admin puede". */
const val MENSAJE_ERROR_CARGA = "No se pudieron cargar los datos. Intentá de nuevo."
const val MENSAJE_ERROR_INESPERADO = "Ocurrió un error inesperado. Intentá de nuevo."
const val MENSAJE_EMAIL_SIN_CONFIRMAR = "Revisá tu correo y confirmá tu cuenta antes de ingresar"
const val MENSAJE_CODIGO_INVALIDO = "El código es incorrecto o venció. Revisalo o pedí uno nuevo."
const val MENSAJE_DEMASIADOS_INTENTOS = "Demasiados intentos. Esperá unos minutos antes de volver a intentar."
const val MENSAJE_CONTRASENA_IGUAL_ANTERIOR = "La nueva contraseña no puede ser igual a la anterior."

/**
 * Errores de la capa de datos, ya traducidos a un mensaje que se puede mostrar tal cual.
 * Los Repositories devuelven `Result.failure(ErrorDatos)`; el ViewModel decide si muestra
 * [message] o un texto propio (p. ej. "Ya existe un alumno con este DNI" para [Duplicado]).
 */
sealed class ErrorDatos(mensaje: String, causa: Throwable? = null) : Exception(mensaje, causa) {

    /** Sin internet, el servidor no responde o se cortó la conexión. */
    class SinConexion(causa: Throwable? = null) : ErrorDatos(MENSAJE_SIN_CONEXION, causa)

    /** La base lo rechazó por las reglas de seguridad (RLS): p. ej. un no-Admin creando un alumno. */
    class SinPermiso(causa: Throwable? = null) : ErrorDatos(MENSAJE_SIN_PERMISO, causa)

    /** Se violó una restricción única. [restriccion] es su nombre en el esquema, si se conoce. */
    class Duplicado(
        val restriccion: String?,
        causa: Throwable? = null,
        mensaje: String = "Ese dato ya está registrado.",
    ) : ErrorDatos(mensaje, causa)

    /**
     * Se violó una clave foránea (23503): p. ej. borrar algo a lo que todavía hacen referencia
     * otros registros. [restriccion] es su nombre en el esquema, si se conoce.
     */
    class ReferenciaEnUso(
        val restriccion: String?,
        causa: Throwable? = null,
        mensaje: String = "No se puede eliminar: está en uso.",
    ) : ErrorDatos(mensaje, causa)

    /** Email o contraseña incorrectos. */
    class CredencialesInvalidas(causa: Throwable? = null) :
        ErrorDatos("Correo o contraseña incorrectos.", causa)

    /** Al registrarse: ya hay una cuenta con ese email. */
    class EmailEnUso(causa: Throwable? = null) : ErrorDatos("Ya existe una cuenta con este correo", causa)

    /** Al registrarse: Supabase pide una contraseña más larga/fuerte. */
    class ContrasenaDebil(causa: Throwable? = null) :
        ErrorDatos("La contraseña es muy débil. Elegí una más larga o más difícil de adivinar.", causa)

    /** La cuenta existe pero todavía no se confirmó el email (si el proyecto lo exige). */
    class EmailSinConfirmar(causa: Throwable? = null) :
        ErrorDatos(MENSAJE_EMAIL_SIN_CONFIRMAR, causa)

    /** Recuperación de contraseña: el código de 6 dígitos es incorrecto o ya venció. */
    class CodigoInvalido(causa: Throwable? = null) : ErrorDatos(MENSAJE_CODIGO_INVALIDO, causa)

    /** Se pidieron demasiados códigos o hubo demasiados intentos (límite de Supabase). */
    class DemasiadosIntentos(causa: Throwable? = null) : ErrorDatos(MENSAJE_DEMASIADOS_INTENTOS, causa)

    /** Al cambiar la contraseña: la nueva es igual a la actual. */
    class ContrasenaIgualAnterior(causa: Throwable? = null) :
        ErrorDatos(MENSAJE_CONTRASENA_IGUAL_ANTERIOR, causa)

    /** No hay sesión o se venció (token inválido). */
    class SesionVencida(causa: Throwable? = null) : ErrorDatos(MENSAJE_SESION_VENCIDA, causa)

    /** Cualquier otro error: se muestra un mensaje genérico (el detalle queda en [cause]). */
    class Otro(causa: Throwable? = null, mensaje: String = MENSAJE_ERROR_INESPERADO) : ErrorDatos(mensaje, causa)
}

/**
 * Error de una regla de la app (no de la red ni de la base), p. ej. "el préstamo ya estaba
 * devuelto". [llamarSupabase] lo devuelve tal cual, sin traducirlo.
 */
abstract class ErrorDeNegocio(mensaje: String, causa: Throwable? = null) : Exception(mensaje, causa)

/** Nombre de la restricción en mensajes como `... unique constraint "alumno_pkey"`. */
private val NOMBRE_RESTRICCION = Regex("constraint \"([^\"]+)\"")

/**
 * Traduce cualquier excepción de supabase-kt / Ktor a un [ErrorDatos].
 * Los códigos (42501, 23505, 23503) son los códigos estándar de error de PostgreSQL.
 */
fun traducirError(e: Throwable): ErrorDatos = when (e) {
    is ErrorDatos -> e
    // HttpRequestException (de supabase-kt) y UnknownHostException también son IOException.
    is IOException, is HttpRequestTimeoutException -> ErrorDatos.SinConexion(e)
    is TokenExpiredException, is AuthSessionMissingException, is SessionRequiredException ->
        ErrorDatos.SesionVencida(e)
    is PostgrestRestException -> when (e.code) {
        // insufficient_privilege. Con 401 la consulta salió sin un usuario válido (rol anon):
        // es la sesión. Con 403 la sesión es válida pero el rol no puede (RLS): es permiso.
        "42501" -> if (e.statusCode == 401) ErrorDatos.SesionVencida(e) else ErrorDatos.SinPermiso(e)
        "23505" -> ErrorDatos.Duplicado(NOMBRE_RESTRICCION.find(e.error)?.groupValues?.get(1), e)
        "23503" -> ErrorDatos.ReferenciaEnUso(NOMBRE_RESTRICCION.find(e.error)?.groupValues?.get(1), e)
        "PGRST301", "PGRST303" -> ErrorDatos.SesionVencida(e) // JWT inválido o vencido
        else -> ErrorDatos.Otro(e)
    }
    is AuthRestException -> traducirErrorAuth(e.errorCode, e.statusCode, e.error, e)
    is RestException -> if (e.statusCode == 401) ErrorDatos.SesionVencida(e) else ErrorDatos.Otro(e)
    else -> ErrorDatos.Otro(e)
}

/**
 * Traduce un error de Supabase Auth (login, registro, recuperación de contraseña). Separada de
 * [traducirError] para poder probarla sin armar una respuesta HTTP.
 *
 * @param codigo el `error_code` que manda Supabase (`null` si no es uno conocido)
 * @param estado el código HTTP
 * @param error el texto crudo del error (se usa como respaldo si falta el código)
 */
internal fun traducirErrorAuth(
    codigo: AuthErrorCode?,
    estado: Int,
    error: String,
    causa: Throwable? = null,
): ErrorDatos = when (codigo) {
    AuthErrorCode.InvalidCredentials -> ErrorDatos.CredencialesInvalidas(causa)
    AuthErrorCode.UserAlreadyExists, AuthErrorCode.EmailExists -> ErrorDatos.EmailEnUso(causa)
    AuthErrorCode.WeakPassword -> ErrorDatos.ContrasenaDebil(causa)
    AuthErrorCode.SamePassword -> ErrorDatos.ContrasenaIgualAnterior(causa)
    AuthErrorCode.OtpExpired -> ErrorDatos.CodigoInvalido(causa)
    AuthErrorCode.OverEmailSendRateLimit, AuthErrorCode.OverRequestRateLimit ->
        ErrorDatos.DemasiadosIntentos(causa)
    AuthErrorCode.EmailNotConfirmed -> ErrorDatos.EmailSinConfirmar(causa)
    AuthErrorCode.SessionNotFound, AuthErrorCode.SessionExpired,
    AuthErrorCode.RefreshTokenNotFound, AuthErrorCode.BadJwt,
    -> ErrorDatos.SesionVencida(causa)
    // Respaldo si Supabase no manda un código conocido: se reconoce por el estado HTTP o el texto.
    else -> when {
        estado == 429 -> ErrorDatos.DemasiadosIntentos(causa)
        error.contains("Email not confirmed", ignoreCase = true) -> ErrorDatos.EmailSinConfirmar(causa)
        else -> ErrorDatos.Otro(causa)
    }
}

/**
 * Corre una llamada a Supabase y nunca lanza: devuelve `Result.success` o `Result.failure`
 * con un [ErrorDatos]. Así la app no se cierra si no hay internet.
 *
 * `withContext(Dispatchers.IO)`: la llamada corre fuera del hilo principal (el de la UI).
 * La [CancellationException] se vuelve a lanzar: significa "se cerró la pantalla, cortá",
 * no es un error.
 */
suspend fun <T> llamarSupabase(bloque: suspend () -> T): Result<T> =
    try {
        Result.success(withContext(Dispatchers.IO) { bloque() })
    } catch (e: CancellationException) {
        throw e
    } catch (e: ErrorDeNegocio) {
        Result.failure(e)
    } catch (e: Exception) {
        Result.failure(traducirError(e))
    }
