package com.ien.prestamoscomputadoras.data.repository

import com.ien.prestamoscomputadoras.data.Permiso
import com.ien.prestamoscomputadoras.data.model.Perfil
import com.ien.prestamoscomputadoras.data.model.Rol
import com.ien.prestamoscomputadoras.data.model.RolPermiso
import com.ien.prestamoscomputadoras.data.remote.SupabaseProvider
import com.ien.prestamoscomputadoras.data.remote.llamarSupabase
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf

/** Qué puede hacer un usuario. */
data class AccesoUsuario(
    val esAdmin: Boolean,
    val permisos: Set<Permiso>,
) {
    fun puede(permiso: Permiso): Boolean = permiso in permisos

    companion object {
        /** Sin sesión, cuenta inexistente o sin conexión: no puede hacer nada. */
        val SIN_ACCESO = AccesoUsuario(esAdmin = false, permisos = emptySet())
    }
}

/** El Admin y Personal Administrativo, con sus permisos. */
interface PermisosRepository {

    /**
     * Acceso del perfil [idUsuario]. Se vuelve a emitir cuando se cambia un permiso desde
     * esta misma instancia. El Admin tiene siempre todos los permisos.
     */
    fun accesoDe(idUsuario: Long?): Flow<AccesoUsuario>

    /** Permisos habilitados para Personal Administrativo (nunca incluye los [Permiso.soloAdmin]). */
    fun permisosPersonal(): Flow<Set<Permiso>>

    /**
     * Tilda o destilda [permiso] para Personal Administrativo. Solo el Admin puede (RLS).
     * Los permisos exclusivos del Admin no se pueden otorgar: lanza [IllegalArgumentException].
     */
    suspend fun cambiarPermisoPersonal(permiso: Permiso, habilitado: Boolean): Result<Unit>
}

@OptIn(ExperimentalCoroutinesApi::class)
class SupabasePermisosRepository(
    private val supabase: SupabaseClient = SupabaseProvider.client,
) : PermisosRepository {

    /** Se incrementa en cada cambio para que los flows vuelvan a leer la base. */
    private val version = MutableStateFlow(0)

    override fun accesoDe(idUsuario: Long?): Flow<AccesoUsuario> {
        if (idUsuario == null) return flowOf(AccesoUsuario.SIN_ACCESO)
        return version.flatMapLatest {
            flow {
                val acceso = llamarSupabase {
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
                emit(acceso.getOrDefault(AccesoUsuario.SIN_ACCESO))
            }
        }
    }

    override fun permisosPersonal(): Flow<Set<Permiso>> = version.flatMapLatest {
        flow { emit(llamarSupabase { leerPermisos(Rol.ID_PERSONAL) }.getOrDefault(emptySet())) }
    }

    override suspend fun cambiarPermisoPersonal(permiso: Permiso, habilitado: Boolean): Result<Unit> {
        require(!permiso.soloAdmin) { "${permiso.name} es exclusivo del Admin" }
        return llamarSupabase<Unit> {
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
        }.onSuccess { version.value++ }
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
