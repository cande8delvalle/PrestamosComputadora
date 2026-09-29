package com.ien.prestamoscomputadoras.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Usuario de la app (quien registra préstamos y devoluciones).
 *
 * La contraseña NUNCA se guarda en texto plano: solo su hash
 * (ver [com.ien.prestamoscomputadoras.util.hashPassword]).
 */
@Entity(
    tableName = "administrador",
    indices = [Index(value = ["email"], unique = true)],
)
data class Administrador(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id_administrador")
    val idAdministrador: Long = 0,
    val nombre: String,
    val apellido: String,
    val dni: String,
    val email: String,
    @ColumnInfo(name = "contrasena_hash")
    val contrasenaHash: String,
)
