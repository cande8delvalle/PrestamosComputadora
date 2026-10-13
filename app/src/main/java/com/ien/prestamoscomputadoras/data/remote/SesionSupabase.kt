package com.ien.prestamoscomputadoras.data.remote

import com.ien.prestamoscomputadoras.util.SesionActual
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Cuánto se espera, como máximo, a que la sesión esté lista antes de consultar la base.
 * Al volver de segundo plano supabase-kt tarda unos milisegundos en recargarla; si hay que
 * pedir un token nuevo y la red anda lenta, un poco más.
 */
private const val ESPERA_SESION_MS = 10_000L

/** Lo que hace falta de la sesión para consultar la base sin que la llamada salga "anónima". */
interface GuardiaSesion {

    /**
     * Vuelve cuando la sesión está lista (Authenticated). Si se está recargando o renovando el
     * token, espera. Lanza [ErrorDatos.SesionVencida] si no hay sesión y
     * [ErrorDatos.SinConexion] si no se pudo renovar por falta de red.
     */
    suspend fun esperarSesion()

    /** Pide un token nuevo a Supabase (con el refresh token guardado). */
    suspend fun refrescar()
}

/**
 * Implementación con supabase-kt.
 *
 * Por qué hace falta: cuando la app pasa a segundo plano, supabase-kt deja el estado de la
 * sesión en `Initializing`, y al volver la recarga en paralelo. Si en ese momento sale una
 * consulta, va sin el token del usuario (con la clave pública = rol `anon`) y la base la
 * rechaza con "permission denied" (42501), porque a `anon` se le revocó todo.
 */
class SupabaseGuardiaSesion(private val supabase: SupabaseClient) : GuardiaSesion {

    override suspend fun esperarSesion() {
        val estado = withTimeoutOrNull(ESPERA_SESION_MS) {
            supabase.auth.sessionStatus.first {
                it is SessionStatus.Authenticated || it is SessionStatus.NotAuthenticated
            }
        }
        when (estado) {
            is SessionStatus.Authenticated -> Unit
            is SessionStatus.NotAuthenticated -> throw ErrorDatos.SesionVencida()
            // Se cumplió la espera: si fue por no poder renovar el token, es la red.
            else -> if (supabase.auth.sessionStatus.value is SessionStatus.RefreshFailure) {
                throw ErrorDatos.SinConexion()
            } else {
                throw ErrorDatos.SesionVencida()
            }
        }
    }

    override suspend fun refrescar() {
        supabase.auth.refreshCurrentSession()
    }
}

/**
 * ¿Vale la pena refrescar el token y reintentar? Sí si la base dijo "sesión vencida" (401) o
 * "permiso denegado" (42501): este último también puede venir de una consulta que salió sin
 * token. Si después de refrescar vuelve a dar 42501, ya es un permiso de rol de verdad.
 */
private fun esErrorDeSesionOPermiso(e: ErrorDatos) = e is ErrorDatos.SesionVencida || e is ErrorDatos.SinPermiso

/**
 * Como [llamarSupabase], pero para consultas que necesitan el usuario logueado:
 * 1. Espera a que la sesión esté lista (nunca consulta "anónimo").
 * 2. Si la base responde sesión vencida / permiso denegado, refresca el token UNA vez y
 *    repite la llamada.
 * 3. Si sigue fallando por sesión, devuelve [ErrorDatos.SesionVencida] y avisa a la app (que
 *    lleva al login). Si sigue con 42501 con la sesión válida, es [ErrorDatos.SinPermiso].
 */
suspend fun <T> llamarConSesion(guardia: GuardiaSesion, bloque: suspend () -> T): Result<T> =
    llamarSupabase {
        guardia.esperarSesion()
        try {
            bloque()
        } catch (e: CancellationException) {
            throw e
        } catch (e: ErrorDeNegocio) {
            throw e
        } catch (e: Exception) {
            val error = traducirError(e)
            if (!esErrorDeSesionOPermiso(error)) throw error
            guardia.refrescar()
            bloque()
        }
    }.onFailure { error ->
        if (error is ErrorDatos.SesionVencida) SesionActual.avisarSesionVencida()
    }
