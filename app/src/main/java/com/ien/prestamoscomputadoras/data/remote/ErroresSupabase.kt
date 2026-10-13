package com.ien.prestamoscomputadoras.data.remote

import io.github.jan.supabase.auth.exception.AuthErrorCode
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.exception.PostgrestRestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

const val MENSAJE_SIN_CONEXION = "No hay conexión a internet. Revisá el Wi-Fi o los datos e intentá de nuevo."
const val MENSAJE_SIN_PERMISO = "No tenés permiso para hacer esto. Solo el Admin puede."
const val MENSAJE_SESION_VENCIDA = "Tu sesión venció. Volvé a iniciar sesión."
const val MENSAJE_ERROR_INESPERADO = "Ocurrió un error inesperado. Intentá de nuevo."

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
    class Duplicado(val restriccion: String?, causa: Throwable? = null) :
        ErrorDatos("Ese dato ya está registrado.", causa)

    /** Email o contraseña incorrectos. */
    class CredencialesInvalidas(causa: Throwable? = null) :
        ErrorDatos("Correo o contraseña incorrectos.", causa)

    /** Al registrarse: ya hay una cuenta con ese email. */
    class EmailEnUso(causa: Throwable? = null) : ErrorDatos("Ya existe una cuenta con este correo", causa)

    /** Al registrarse: Supabase pide una contraseña más larga/fuerte. */
    class ContrasenaDebil(causa: Throwable? = null) :
        ErrorDatos("La contraseña es muy débil. Usá al menos 6 caracteres.", causa)

    /** La cuenta existe pero todavía no se confirmó el email (si el proyecto lo exige). */
    class EmailSinConfirmar(causa: Throwable? = null) :
        ErrorDatos("Tenés que confirmar tu correo antes de iniciar sesión. Revisá tu bandeja de entrada.", causa)

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
 * Los códigos (42501, 23505) son los códigos estándar de error de PostgreSQL.
 */
fun traducirError(e: Throwable): ErrorDatos = when (e) {
    is ErrorDatos -> e
    // HttpRequestException (de supabase-kt) y UnknownHostException también son IOException.
    is IOException, is HttpRequestTimeoutException -> ErrorDatos.SinConexion(e)
    is PostgrestRestException -> when (e.code) {
        "42501" -> ErrorDatos.SinPermiso(e) // insufficient_privilege: lo bloqueó una política RLS
        "23505" -> ErrorDatos.Duplicado(NOMBRE_RESTRICCION.find(e.error)?.groupValues?.get(1), e)
        "PGRST301", "PGRST303" -> ErrorDatos.SesionVencida(e) // JWT inválido o vencido
        else -> ErrorDatos.Otro(e)
    }
    is AuthRestException -> when (e.errorCode) {
        AuthErrorCode.InvalidCredentials -> ErrorDatos.CredencialesInvalidas(e)
        AuthErrorCode.UserAlreadyExists, AuthErrorCode.EmailExists -> ErrorDatos.EmailEnUso(e)
        AuthErrorCode.WeakPassword -> ErrorDatos.ContrasenaDebil(e)
        AuthErrorCode.EmailNotConfirmed -> ErrorDatos.EmailSinConfirmar(e)
        AuthErrorCode.SessionNotFound, AuthErrorCode.SessionExpired,
        AuthErrorCode.RefreshTokenNotFound, AuthErrorCode.BadJwt,
        -> ErrorDatos.SesionVencida(e)
        else -> ErrorDatos.Otro(e)
    }
    is RestException -> if (e.statusCode == 401) ErrorDatos.SesionVencida(e) else ErrorDatos.Otro(e)
    else -> ErrorDatos.Otro(e)
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
