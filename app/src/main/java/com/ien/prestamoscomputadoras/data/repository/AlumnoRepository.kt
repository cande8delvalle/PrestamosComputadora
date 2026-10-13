package com.ien.prestamoscomputadoras.data.repository

import com.ien.prestamoscomputadoras.data.model.Alumno
import com.ien.prestamoscomputadoras.data.model.NuevoAlumno
import com.ien.prestamoscomputadoras.data.remote.SupabaseProvider
import com.ien.prestamoscomputadoras.data.remote.llamarSupabase
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from

/** Acceso a los alumnos. Los ViewModels usan esta interfaz, no Supabase directo. */
interface AlumnoRepository {

    /** Inserta el alumno y devuelve el id generado. Solo el Admin puede (RLS). */
    suspend fun insertar(alumno: Alumno): Result<Long>

    /** `null` si no hay ningún alumno con ese DNI. */
    suspend fun buscarPorDni(dni: String): Result<Alumno?>
}

class SupabaseAlumnoRepository(
    private val supabase: SupabaseClient = SupabaseProvider.client,
) : AlumnoRepository {

    override suspend fun insertar(alumno: Alumno): Result<Long> = llamarSupabase {
        supabase.from("alumno")
            .insert(NuevoAlumno(nombre = alumno.nombre, apellido = alumno.apellido, dni = alumno.dni)) {
                select() // que devuelva la fila insertada, con el id que generó la base
            }
            .decodeSingle<Alumno>()
            .idAlumno
    }

    override suspend fun buscarPorDni(dni: String): Result<Alumno?> = llamarSupabase {
        supabase.from("alumno")
            .select {
                filter { eq("dni", dni) }
                limit(1)
            }
            .decodeSingleOrNull<Alumno>()
    }
}
