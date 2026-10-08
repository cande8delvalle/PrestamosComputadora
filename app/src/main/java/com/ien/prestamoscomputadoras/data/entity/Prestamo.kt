package com.ien.prestamoscomputadoras.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Préstamo de una computadora a un alumno. Guarda quién entregó el equipo y quién recibió la
 * devolución, que pueden ser administradores distintos.
 *
 * Las fechas se guardan como epoch en milisegundos (Long), así no hacen falta TypeConverters.
 * Las FK usan RESTRICT: no se puede borrar un alumno/computadora/administrador con préstamos.
 */
@Entity(
    tableName = "prestamo",
    foreignKeys = [
        ForeignKey(
            entity = Alumno::class,
            parentColumns = ["id_alumno"],
            childColumns = ["id_alumno"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = Computadora::class,
            parentColumns = ["id_computadora"],
            childColumns = ["id_computadora"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = Administrador::class,
            parentColumns = ["id_administrador"],
            childColumns = ["id_administrador_prestamo"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = Administrador::class,
            parentColumns = ["id_administrador"],
            childColumns = ["id_administrador_devolucion"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    // Room recomienda indexar cada columna FK (evita full scans al validar la relación).
    indices = [
        Index(value = ["id_alumno"]),
        Index(value = ["id_computadora"]),
        Index(value = ["id_administrador_prestamo"]),
        Index(value = ["id_administrador_devolucion"]),
    ],
)
data class Prestamo(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id_prestamo")
    val idPrestamo: Long = 0,
    @ColumnInfo(name = "id_alumno")
    val idAlumno: Long,
    @ColumnInfo(name = "id_computadora")
    val idComputadora: Long,
    /** Administrador que entregó la computadora. */
    @ColumnInfo(name = "id_administrador_prestamo")
    val idAdministradorPrestamo: Long,
    @ColumnInfo(name = "fecha_prestamo")
    val fechaPrestamo: Long,
    /** `null` mientras el préstamo sigue activo. */
    @ColumnInfo(name = "fecha_devolucion")
    val fechaDevolucion: Long? = null,
    /** Administrador que recibió la devolución. `null` mientras el préstamo sigue activo. */
    @ColumnInfo(name = "id_administrador_devolucion")
    val idAdministradorDevolucion: Long? = null,
    /** [ESTADO_ACTIVO] o [ESTADO_DEVUELTO]. */
    val estado: String = ESTADO_ACTIVO,
    /**
     * Observaciones opcionales sobre el estado del equipo al entregarlo. La revisión de la
     * devolución va aparte, en [EstadoComputadora].
     */
    @ColumnInfo(name = "observaciones_iniciales")
    val observacionesIniciales: String? = null,
) {
    companion object {
        const val ESTADO_ACTIVO = "ACTIVO"
        const val ESTADO_DEVUELTO = "DEVUELTO"
    }
}
