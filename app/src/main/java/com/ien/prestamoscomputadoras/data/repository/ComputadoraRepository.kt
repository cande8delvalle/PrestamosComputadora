package com.ien.prestamoscomputadoras.data.repository

import com.ien.prestamoscomputadoras.data.model.Computadora
import com.ien.prestamoscomputadoras.data.remote.SupabaseProvider
import com.ien.prestamoscomputadoras.data.remote.llamarSupabase
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from

/** Alta de computadoras. */
interface ComputadoraRepository {

    /** Inserta con el id que trae (el número del código). Solo el Admin puede (RLS). */
    suspend fun insertar(computadora: Computadora): Result<Long>

    /** `null` si no hay ninguna computadora con ese código. */
    suspend fun buscarPorCodigo(codigo: String): Result<Computadora?>

    /** `null` si no hay ninguna computadora con ese id. */
    suspend fun buscarPorId(idComputadora: Long): Result<Computadora?>
}

class SupabaseComputadoraRepository(
    private val supabase: SupabaseClient = SupabaseProvider.client,
) : ComputadoraRepository {

    override suspend fun insertar(computadora: Computadora): Result<Long> = llamarSupabase {
        supabase.from("computadora").insert(computadora)
        computadora.idComputadora
    }

    override suspend fun buscarPorCodigo(codigo: String): Result<Computadora?> = llamarSupabase {
        supabase.from("computadora")
            .select { filter { eq("codigo", codigo) } }
            .decodeSingleOrNull<Computadora>()
    }

    override suspend fun buscarPorId(idComputadora: Long): Result<Computadora?> = llamarSupabase {
        supabase.from("computadora")
            .select { filter { eq("id_computadora", idComputadora) } }
            .decodeSingleOrNull<Computadora>()
    }
}
