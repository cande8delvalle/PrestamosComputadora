package com.ien.prestamoscomputadoras.data.repository

import com.ien.prestamoscomputadoras.data.Permiso
import com.ien.prestamoscomputadoras.data.dao.RolDao
import com.ien.prestamoscomputadoras.data.entity.Rol
import com.ien.prestamoscomputadoras.data.entity.RolPermiso
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/** Qué puede hacer un usuario. */
data class AccesoUsuario(
    val esAdmin: Boolean,
    val permisos: Set<Permiso>,
) {
    fun puede(permiso: Permiso): Boolean = permiso in permisos

    companion object {
        /** Sin sesión o cuenta inexistente: no puede hacer nada. */
        val SIN_ACCESO = AccesoUsuario(esAdmin = false, permisos = emptySet())
    }
}

/** El Admin y Personal Administrativo, con sus permisos. */
@OptIn(ExperimentalCoroutinesApi::class)
class PermisosRepository(private val rolDao: RolDao) {

    /**
     * Acceso del administrador [idAdministrador], que se vuelve a emitir si cambian su rol o los
     * permisos de su rol. El Admin tiene siempre todos los permisos (no se leen de la base).
     */
    fun accesoDe(idAdministrador: Long?): Flow<AccesoUsuario> {
        if (idAdministrador == null) return flowOf(AccesoUsuario.SIN_ACCESO)
        return rolDao.rolDeAdministrador(idAdministrador).flatMapLatest { idRol ->
            when (idRol) {
                null -> flowOf(AccesoUsuario.SIN_ACCESO)
                Rol.ID_ADMIN -> flowOf(AccesoUsuario(esAdmin = true, permisos = Permiso.entries.toSet()))
                else -> permisosDeRol(idRol).map { AccesoUsuario(esAdmin = false, permisos = it) }
            }
        }
    }

    /** Permisos habilitados para Personal Administrativo (nunca incluye los [Permiso.soloAdmin]). */
    fun permisosPersonal(): Flow<Set<Permiso>> = permisosDeRol(Rol.ID_PERSONAL)

    /**
     * Tilda o destilda [permiso] para Personal Administrativo. Los permisos exclusivos del Admin
     * no se pueden otorgar: lanza [IllegalArgumentException].
     */
    suspend fun cambiarPermisoPersonal(permiso: Permiso, habilitado: Boolean) {
        require(!permiso.soloAdmin) { "${permiso.name} es exclusivo del Admin" }
        if (habilitado) {
            rolDao.habilitarPermiso(RolPermiso(Rol.ID_PERSONAL, permiso.name))
        } else {
            rolDao.deshabilitarPermiso(Rol.ID_PERSONAL, permiso.name)
        }
    }

    /** Ignora nombres desconocidos y, por las dudas, cualquier permiso exclusivo del Admin. */
    private fun permisosDeRol(idRol: Long): Flow<Set<Permiso>> =
        rolDao.permisosDeRol(idRol).map { nombres ->
            nombres.mapNotNull(Permiso::desdeNombre).filterNot { it.soloAdmin }.toSet()
        }
}
