package com.ien.prestamoscomputadoras.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ien.prestamoscomputadoras.data.entity.RolPermiso
import kotlinx.coroutines.flow.Flow

@Dao
interface RolDao {

    /** Rol del usuario, o `null` si no existe. Se vuelve a emitir si cambia. */
    @Query("SELECT id_rol FROM usuario WHERE id_usuario = :idUsuario")
    fun rolDeUsuario(idUsuario: Long): Flow<Long?>

    /** Nombres de los permisos habilitados para el rol. Se vuelve a emitir si cambian. */
    @Query("SELECT permiso FROM rol_permiso WHERE id_rol = :idRol")
    fun permisosDeRol(idRol: Long): Flow<List<String>>

    /** Habilita un permiso. IGNORE: si ya estaba habilitado no hace nada. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun habilitarPermiso(rolPermiso: RolPermiso)

    @Query("DELETE FROM rol_permiso WHERE id_rol = :idRol AND permiso = :permiso")
    suspend fun deshabilitarPermiso(idRol: Long, permiso: String)
}
