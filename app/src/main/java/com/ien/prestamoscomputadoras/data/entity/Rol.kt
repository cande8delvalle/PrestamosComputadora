package com.ien.prestamoscomputadoras.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Rol de un usuario de la app. Hay dos filas fijas, creadas con la base (ver
 * [com.ien.prestamoscomputadoras.data.AppDatabase]): [ID_ADMIN] y [ID_PERSONAL].
 */
@Entity(tableName = "rol")
data class Rol(
    @PrimaryKey
    @ColumnInfo(name = "id_rol")
    val idRol: Long,
    val nombre: String,
    /** `true` = rol de sistema: tiene todos los permisos y no se puede editar. */
    @ColumnInfo(name = "es_sistema")
    val esSistema: Boolean,
) {
    companion object {
        const val ID_ADMIN = 1L
        const val ID_PERSONAL = 2L
    }
}
