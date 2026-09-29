package com.ien.prestamoscomputadoras.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Computadora del instituto. La carga inicial se hace externamente, directo en la base:
 * por eso el id NO es autogenerado y la app no inserta computadoras.
 */
@Entity(
    tableName = "computadora",
    // El código es el que contiene el QR: único, y con índice para buscarlo rápido.
    indices = [Index(value = ["codigo"], unique = true)],
)
data class Computadora(
    @PrimaryKey
    @ColumnInfo(name = "id_computadora")
    val idComputadora: Long,
    val codigo: String,
    val disponible: Boolean,
)
