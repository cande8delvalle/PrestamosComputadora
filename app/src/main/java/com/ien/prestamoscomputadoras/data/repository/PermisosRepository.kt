package com.ien.prestamoscomputadoras.data.repository

import com.ien.prestamoscomputadoras.data.Permiso
import com.ien.prestamoscomputadoras.data.model.Perfil
import com.ien.prestamoscomputadoras.data.model.Rol
import com.ien.prestamoscomputadoras.data.model.RolPermiso
import com.ien.prestamoscomputadoras.data.remote.GuardiaSesion
import com.ien.prestamoscomputadoras.data.remote.SupabaseGuardiaSesion
import com.ien.prestamoscomputadoras.data.remote.SupabaseProvider
import com.ien.prestamoscomputadoras.data.remote.llamarConSesion
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from

/** Qué puede hacer un usuario. */
data class AccesoUsuario(
    val esAdmin: Boolean,
    val permisos: Set<Permiso>,
) {
    fun puede(permiso: Permiso): Boolean = permiso in permisos

    companion object {
        /** Sin sesión o cuenta inexistente: no puede hacer nada. (Un ERROR no es esto.) */
        val SIN_ACCESO = AccesoUsuario(esAdmin = false, permisos = emptySet())
    }
}

/**
 * El Admin y Personal Administrativo, con sus permisos.
 *
 * Las lecturas devuelven `Result`: si fallan (sin red, sesión renovándose) es un ERROR, no
 * "sin permisos". Así quien las usa conserva lo último que tenía y muestra el aviso.
 * La única escritura es [cambiarPermisoPersonal], y solo la usa la pantalla Roles y Permisos.
 */
interface PermisosRepository {

    /** Acceso del perfil [idUsuario]. El Admin tiene siempre todos los permisos. */
    suspend fun accesoDe(idUsuario: Long?): Result<AccesoUsuario>

    /** Permisos habilitados para Personal Administrativo (nunca incluye los [Permiso.soloAdmin]). */
    suspend fun permisosPersonal(): Result<Set<Permiso>>

    /**
     * Tilda o destilda [permiso] para Personal Administrativo. Solo el Admin puede (RLS).
     * Los permisos exclusivos del Admin no se pueden otorgar: lanza [IllegalArgumentException].
     */
    suspend fun cambiarPermisoPersonal(permiso: Permiso, habilitado: Boolean): Result<Unit>
}

class SupabasePermisosRepository(
    private val supabase: SupabaseClient = SupabaseProvider.client,
    private val guardia: GuardiaSesion = SupabaseGuardiaSesion(supabase),
) : PermisosRepository {

    override suspend fun accesoDe(idUsuario: Long?): Result<AccesoUsuario> {
        if (idUsuario == null) return Result.success(AccesoUsuario.SIN_ACCESO)
        return llamarConSesion(guardia) {
            val idRol = supabase.from("perfil")
                .select { filter { eq("id_perfil", idUsuario) } }
                .decodeSingleOrNull<Perfil>()
                ?.idRol
            when (idRol) {
                null -> AccesoUsuario.SIN_ACCESO
                Rol.ID_ADMIN -> AccesoUsuario(esAdmin = true, permisos = Permiso.entries.toSet())
                else -> AccesoUsuario(esAdmin = false, permisos = leerPermisos(idRol))
            }
        }
    }

    override suspend fun permisosPersonal(): Result<Set<Permiso>> =
        llamarConSesion(guardia) { leerPermisos(Rol.ID_PERSONAL) }

    override suspend fun cambiarPermisoPersonal(permiso: Permiso, habilitado: Boolean): Result<Unit> {
        require(!permiso.soloAdmin) { "${permiso.name} es exclusivo del Admin" }
        return llamarConSesion<Unit>(guardia) {
            val tabla = supabase.from("rol_permiso")
            if (habilitado) {
                // upsert: si ya estaba habilitado no falla (como el IGNORE de Room).
                tabla.upsert(RolPermiso(Rol.ID_PERSONAL, permiso.name)) { ignoreDuplicates = true }
            } else {
                tabla.delete {
                    filter {
                        eq("id_rol", Rol.ID_PERSONAL)
                        eq("permiso", permiso.name)
                    }
                }
            }
        }
    }

    /** Ignora nombres desconocidos y, por las dudas, cualquier permiso exclusivo del Admin. */
    private suspend fun leerPermisos(idRol: Long): Set<Permiso> =
        supabase.from("rol_permiso")
            .select { filter { eq("id_rol", idRol) } }
            .decodeList<RolPermiso>()
            .mapNotNull { Permiso.desdeNombre(it.permiso) }
            .filterNot { it.soloAdmin }
            .toSet()
}
