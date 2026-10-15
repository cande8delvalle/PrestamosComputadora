package com.ien.prestamoscomputadoras.data.remote

import com.ien.prestamoscomputadoras.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime

/**
 * Único cliente de Supabase de la app (reemplaza a `AppDatabase.getInstance`).
 *
 * - Auth: login, registro y la sesión (se guarda sola en el teléfono y se renueva).
 * - Postgrest: leer y escribir las tablas como si fuera una API REST.
 * - Realtime: avisos en vivo cuando cambia la tabla prestamo.
 *
 * `by lazy`: se crea la primera vez que alguien lo usa, una sola vez.
 */
object SupabaseProvider {

    val client: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_KEY,
        ) {
            install(Auth)
            install(Postgrest)
            install(Realtime)
        }
    }
}
