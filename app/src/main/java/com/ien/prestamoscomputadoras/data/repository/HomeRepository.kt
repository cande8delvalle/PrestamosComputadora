package com.ien.prestamoscomputadoras.data.repository

import com.ien.prestamoscomputadoras.data.model.PrestamoHistorial
import com.ien.prestamoscomputadoras.data.model.ResumenHoy
import com.ien.prestamoscomputadoras.data.remote.SupabaseProvider
import com.ien.prestamoscomputadoras.data.remote.llamarSupabase
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/** Resumen del día para la pantalla de inicio. */
interface HomeRepository {

    /** "Prestados" (ACTIVO ahora) y "Devueltos hoy" (en hora de Argentina), vía resumen_hoy(). */
    suspend fun resumenHoy(): Result<ResumenHoy>

    /** Últimos [limite] préstamos (uno por fila, con todo su detalle), más reciente primero. */
    suspend fun prestamosRecientes(limite: Int): Result<List<PrestamoHistorial>>

    /**
     * Emite cada vez que alguien (desde cualquier celular) crea, devuelve o borra un préstamo.
     * Mientras se está escuchando hay una suscripción abierta a Supabase Realtime; al dejar de
     * escuchar (se cancela quien la recolecta) la suscripción se cierra.
     * Si no se puede conectar, no emite nada (no es un error para la pantalla).
     */
    fun cambiosEnPrestamos(): Flow<Unit>
}

/** Para que cada suscripción tenga un canal con nombre propio. */
private val contadorCanales = AtomicInteger()

class SupabaseHomeRepository(
    private val supabase: SupabaseClient = SupabaseProvider.client,
) : HomeRepository {

    override suspend fun resumenHoy(): Result<ResumenHoy> = llamarSupabase {
        // La función devuelve una tabla de una sola fila.
        supabase.postgrest.rpc("resumen_hoy").decodeSingle<ResumenHoy>()
    }

    override suspend fun prestamosRecientes(limite: Int): Result<List<PrestamoHistorial>> = llamarSupabase {
        supabase.from("historial_prestamos")
            .select {
                order("ultima_actividad", Order.DESCENDING)
                order("id_prestamo", Order.DESCENDING)
                limit(limite.toLong())
            }
            .decodeList<PrestamoHistorial>()
    }

    override fun cambiosEnPrestamos(): Flow<Unit> = flow {
        val canal = supabase.channel("prestamos-${contadorCanales.incrementAndGet()}")
        // Hay que pedir el flow ANTES de subscribe(): así el canal sabe qué cambios escuchar.
        val cambios = canal.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "prestamo"
        }
        try {
            canal.subscribe()
            emitAll(cambios.map { })
        } finally {
            // Se cierra la suscripción aunque se esté cancelando (NonCancellable).
            withContext(NonCancellable) { supabase.realtime.removeChannel(canal) }
        }
    }.catch {
        // Sin realtime (p. ej. sin internet) la pantalla sigue mostrando lo último cargado.
    }
}
