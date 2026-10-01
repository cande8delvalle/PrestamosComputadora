package com.ien.prestamoscomputadoras.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey

/**
 * Permiso habilitado para un rol: una fila por permiso tildado (destildar = borrar la fila).
 * [permiso] es el `name` de [com.ien.prestamoscomputadoras.data.Permiso].
 *
 * El Admin no tiene filas: tiene todos los permisos por ser rol de sistema.
 */
@Entity(
    tableName = "rol_permiso",
    primaryKeys = ["id_rol", "permiso"],
    foreignKeys = [
        ForeignKey(
            entity = Rol::class,
            parentColumns = ["id_rol"],
            childColumns = ["id_rol"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class RolPermiso(
    @ColumnInfo(name = "id_rol")
    val idRol: Long,
    val permiso: String,
)
