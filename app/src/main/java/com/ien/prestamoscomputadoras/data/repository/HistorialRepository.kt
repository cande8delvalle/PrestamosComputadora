package com.ien.prestamoscomputadoras.data.repository

import com.ien.prestamoscomputadoras.data.model.PrestamoHistorial
import com.ien.prestamoscomputadoras.data.remote.SupabaseProvider
import com.ien.prestamoscomputadoras.data.remote.GuardiaSesion
import com.ien.prestamoscomputadoras.data.remote.SupabaseGuardiaSesion
import com.ien.prestamoscomputadoras.data.remote.llamarConSesion
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlin.time.Instant

/** Préstamos para la pantalla "Historial de Préstamos". */
interface HistorialRepository {

    /**
     * Préstamos con fecha de préstamo en [desde, hasta), ordenados por lo último que pasó con
     * cada uno (ultima_actividad), del más reciente al más antiguo.
     */
    suspend fun listarEntre(desde: Instant, hasta: Instant): Result<List<PrestamoHistorial>>
}

class SupabaseHistorialRepository(
    private val supabase: SupabaseClient = SupabaseProvider.client,
    /** Espera la sesión antes de consultar y refresca el token si venció (ver SesionSupabase.kt). */
    private val guardia: GuardiaSesion = SupabaseGuardiaSesion(supabase),
) : HistorialRepository {

    override suspend fun listarEntre(desde: Instant, hasta: Instant): Result<List<PrestamoHistorial>> =
        llamarConSesion(guardia) {
            supabase.from("historial_prestamos")
                .select {
                    filter {
                        // Instant.toString() es ISO-8601 en UTC ("2026-10-12T03:00:00Z"):
                        // Postgres lo compara bien contra timestamptz.
                        gte("fecha_prestamo", desde.toString())
                        lt("fecha_prestamo", hasta.toString())
                    }
                    order("ultima_actividad", Order.DESCENDING)
                    order("id_prestamo", Order.DESCENDING)
                }
                .decodeList<PrestamoHistorial>()
        }
}
