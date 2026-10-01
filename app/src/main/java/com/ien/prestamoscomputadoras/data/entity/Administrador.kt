package com.ien.prestamoscomputadoras.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Usuario de la app (quien registra préstamos y devoluciones).
 *
 * La contraseña NUNCA se guarda en texto plano: solo su hash
 * (ver [com.ien.prestamoscomputadoras.util.hashPassword]).
 *
 * [idRol] define qué puede hacer: la primera cuenta registrada es Admin y las siguientes,
 * Personal Administrativo (ver [com.ien.prestamoscomputadoras.data.repository.AdministradorRepository.registrar]).
 */
@Entity(
    tableName = "administrador",
    indices = [
        Index(value = ["email"], unique = true),
        Index(value = ["nombre_usuario"], unique = true),
        Index(value = ["id_rol"]),
    ],
    foreignKeys = [
        ForeignKey(
            entity = Rol::class,
            parentColumns = ["id_rol"],
            childColumns = ["id_rol"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
)
data class Administrador(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id_administrador")
    val idAdministrador: Long = 0,
    val nombre: String,
    val apellido: String,
    @ColumnInfo(name = "nombre_usuario")
    val nombreUsuario: String,
    val dni: String,
    val email: String,
    @ColumnInfo(name = "contrasena_hash")
    val contrasenaHash: String,
    @ColumnInfo(name = "id_rol")
    val idRol: Long = Rol.ID_PERSONAL,
)
