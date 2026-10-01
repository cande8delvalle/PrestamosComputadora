package com.ien.prestamoscomputadoras.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Computadora del instituto. Se da de alta desde "Registrar Computadora": el id NO es
 * autogenerado, es el número del código (PC-11 -> 11).
 *
 * La disponibilidad no se guarda: se calcula (disponible = sin préstamo ACTIVO), ver
 * [com.ien.prestamoscomputadoras.data.dao.ComputadoraDao.listarDisponibles].
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
    /** Opcional, ej: "ProBook 440 G8". */
    val modelo: String? = null,
)
