package com.ien.prestamoscomputadoras.data.repository

import com.ien.prestamoscomputadoras.data.model.Aula
import com.ien.prestamoscomputadoras.data.model.NuevaAula
import com.ien.prestamoscomputadoras.data.remote.ErrorDatos
import com.ien.prestamoscomputadoras.data.remote.GuardiaSesion
import com.ien.prestamoscomputadoras.data.remote.SupabaseGuardiaSesion
import com.ien.prestamoscomputadoras.data.remote.SupabaseProvider
import com.ien.prestamoscomputadoras.data.remote.llamarConSesion
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order

const val MENSAJE_AULA_DUPLICADA = "Ya existe un aula con ese nombre"
const val MENSAJE_AULA_CON_PRESTAMOS = "No se puede eliminar: tiene préstamos registrados"

/** Acceso a las aulas. Los ViewModels usan esta interfaz, no Supabase directo. */
interface AulaRepository {

    /** Todas las aulas, ordenadas por nombre. */
    suspend fun listar(): Result<List<Aula>>

    /**
     * Da de alta un aula y devuelve la fila creada. Solo el Admin puede (RLS).
     * Si ya hay otra con ese nombre (sin distinguir mayúsculas) falla con [ErrorDatos.Duplicado].
     */
    suspend fun insertar(nombre: String): Result<Aula>

    /**
     * Elimina el aula. Solo el Admin puede (RLS). Si ya tuvo préstamos falla con
     * [ErrorDatos.ReferenciaEnUso] y no se borra nada.
     */
    suspend fun eliminar(idAula: Long): Result<Unit>
}

/**
 * Pone el mensaje de aulas a los errores de la base: 23505 (nombre repetido, índice
 * `aula_nombre_unico`) y 23503 (el aula ya tiene préstamos). El resto pasa igual.
 */
internal fun errorDeAula(e: Throwable): Throwable = when (e) {
    is ErrorDatos.Duplicado -> ErrorDatos.Duplicado(e.restriccion, e.cause, MENSAJE_AULA_DUPLICADA)
    is ErrorDatos.ReferenciaEnUso -> ErrorDatos.ReferenciaEnUso(e.restriccion, e.cause, MENSAJE_AULA_CON_PRESTAMOS)
    else -> e
}

private fun <T> Result<T>.conErroresDeAula(): Result<T> =
    fold(onSuccess = { Result.success(it) }, onFailure = { Result.failure(errorDeAula(it)) })

class SupabaseAulaRepository(
    private val supabase: SupabaseClient = SupabaseProvider.client,
    /** Espera la sesión antes de consultar y refresca el token si venció (ver SesionSupabase.kt). */
    private val guardia: GuardiaSesion = SupabaseGuardiaSesion(supabase),
) : AulaRepository {

    override suspend fun listar(): Result<List<Aula>> = llamarConSesion(guardia) {
        supabase.from("aula")
            .select { order("nombre", Order.ASCENDING) }
            .decodeList<Aula>()
    }

    override suspend fun insertar(nombre: String): Result<Aula> = llamarConSesion(guardia) {
        supabase.from("aula")
            .insert(NuevaAula(nombre = nombre)) { select() }
            .decodeSingle<Aula>()
    }.conErroresDeAula()

    override suspend fun eliminar(idAula: Long): Result<Unit> = llamarConSesion<Unit>(guardia) {
        supabase.from("aula").delete { filter { eq("id_aula", idAula) } }
    }.conErroresDeAula()
}
