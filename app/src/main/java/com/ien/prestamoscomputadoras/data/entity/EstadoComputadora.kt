package com.ien.prestamoscomputadoras.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Revisión del equipo al momento de la devolución. Hay como máximo una por préstamo
 * (id_prestamo es único) y se borra junto con su préstamo (CASCADE).
 */
@Entity(
    tableName = "estado_computadora",
    foreignKeys = [
        ForeignKey(
            entity = Prestamo::class,
            parentColumns = ["id_prestamo"],
            childColumns = ["id_prestamo"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["id_prestamo"], unique = true)],
)
data class EstadoComputadora(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id_estado")
    val idEstado: Long = 0,
    @ColumnInfo(name = "id_prestamo")
    val idPrestamo: Long,
    val enciende: Boolean,
    @ColumnInfo(name = "pantalla_ok")
    val pantallaOk: Boolean,
    val cargador: Boolean,
    val observaciones: String? = null,
    /** Epoch en milisegundos. */
    @ColumnInfo(name = "fecha_revision")
    val fechaRevision: Long,
)
