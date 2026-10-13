package com.ien.prestamoscomputadoras.data.repository

import com.ien.prestamoscomputadoras.data.model.Alumno
import com.ien.prestamoscomputadoras.data.model.Computadora
import com.ien.prestamoscomputadoras.data.model.NuevoPrestamo
import com.ien.prestamoscomputadoras.data.model.Prestamo
import com.ien.prestamoscomputadoras.data.remote.SupabaseProvider
import com.ien.prestamoscomputadoras.data.remote.llamarSupabase
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Índice de la base que impide dos préstamos ACTIVOS de la misma computadora. */
const val RESTRICCION_UN_ACTIVO_POR_COMPUTADORA = "prestamo_un_activo_por_computadora"

/** Todo lo que necesita la pantalla "Nuevo Préstamo": alumnos, equipos libres y el alta. */
interface PrestamoRepository {

    /**
     * Registra el préstamo como ACTIVO y devuelve el id generado. La fecha la pone el servidor.
     * Si la computadora ya está prestada, falla con ErrorDatos.Duplicado
     * ([RESTRICCION_UN_ACTIVO_POR_COMPUTADORA]).
     */
    suspend fun insertar(prestamo: NuevoPrestamo): Result<Long>

    suspend fun listarAlumnos(): Result<List<Alumno>>

    /** Computadoras sin un préstamo ACTIVO. */
    suspend fun listarComputadorasDisponibles(): Result<List<Computadora>>
}

/** Solo la columna id_computadora de un préstamo (para no traer filas enteras). */
@Serializable
private data class IdComputadora(@SerialName("id_computadora") val idComputadora: Long)

class SupabasePrestamoRepository(
    private val supabase: SupabaseClient = SupabaseProvider.client,
) : PrestamoRepository {

    override suspend fun insertar(prestamo: NuevoPrestamo): Result<Long> = llamarSupabase {
        supabase.from("prestamo")
            .insert(prestamo) { select() }
            .decodeSingle<Prestamo>()
            .idPrestamo
    }

    override suspend fun listarAlumnos(): Result<List<Alumno>> = llamarSupabase {
        supabase.from("alumno")
            .select {
                order("apellido", Order.ASCENDING)
                order("nombre", Order.ASCENDING)
            }
            .decodeList<Alumno>()
    }

    override suspend fun listarComputadorasDisponibles(): Result<List<Computadora>> = llamarSupabase {
        val prestadas = supabase.from("prestamo")
            .select(Columns.list("id_computadora")) {
                filter { eq("estado", Prestamo.ESTADO_ACTIVO) }
            }
            .decodeList<IdComputadora>()
            .map { it.idComputadora }
            .toSet()
        supabase.from("computadora")
            .select { order("codigo", Order.ASCENDING) }
            .decodeList<Computadora>()
            .filter { it.idComputadora !in prestadas }
    }
}
