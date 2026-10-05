package com.ien.prestamoscomputadoras.data.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Un ítem de "Actividad reciente": una entrega o una devolución. Un préstamo ya devuelto
 * aporta dos movimientos, uno por cada cosa.
 */
data class MovimientoReciente(
    @ColumnInfo(name = "id_prestamo") val idPrestamo: Long,
    @ColumnInfo(name = "alumno_nombre") val alumnoNombre: String,
    @ColumnInfo(name = "alumno_apellido") val alumnoApellido: String,
    @ColumnInfo(name = "codigo_computadora") val codigoComputadora: String,
    /** [TIPO_PRESTAMO] o [TIPO_DEVOLUCION]. */
    val tipo: String,
    /** Epoch ms de la entrega o de la devolución, según [tipo]. */
    @ColumnInfo(name = "fecha_movimiento") val fechaMovimiento: Long,
) {
    companion object {
        const val TIPO_PRESTAMO = "PRESTAMO"
        const val TIPO_DEVOLUCION = "DEVOLUCION"
    }
}

/**
 * Datos de la pantalla de inicio. Devuelven [Flow]: Room vuelve a emitir cada vez que cambia
 * la tabla, así Home se actualiza sola al volver de registrar un préstamo o una devolución.
 */
@Dao
interface HomeDao {

    /**
     * Préstamos en curso (estado ACTIVO), de cualquier fecha. Sube al registrar un préstamo y
     * baja al devolverlo: la devolución pasa el estado a DEVUELTO en el mismo UPDATE que
     * completa fecha_devolucion.
     */
    @Query("SELECT COUNT(*) FROM prestamo WHERE estado = 'ACTIVO'")
    fun contarActivos(): Flow<Int>

    /**
     * Préstamos devueltos con fecha de devolución en [desde, hasta). Con los límites del día
     * local es "devueltos hoy": compara el día, no la hora, y no baja durante el día.
     */
    @Query(
        "SELECT COUNT(*) FROM prestamo WHERE estado = 'DEVUELTO' " +
            "AND fecha_devolucion >= :desde AND fecha_devolucion < :hasta",
    )
    fun contarDevueltosEntre(desde: Long, hasta: Long): Flow<Int>

    /**
     * Últimos [limite] movimientos, del más reciente al más antiguo: las entregas y las
     * devoluciones, unidas con UNION ALL. Si una entrega y una devolución tienen la misma hora
     * va primero la devolución ('DEVOLUCION' < 'PRESTAMO').
     */
    @Query(
        "SELECT * FROM (" +
            "SELECT p.id_prestamo AS id_prestamo, a.nombre AS alumno_nombre, " +
            "a.apellido AS alumno_apellido, c.codigo AS codigo_computadora, " +
            "'PRESTAMO' AS tipo, p.fecha_prestamo AS fecha_movimiento " +
            "FROM prestamo p " +
            "INNER JOIN alumno a ON a.id_alumno = p.id_alumno " +
            "INNER JOIN computadora c ON c.id_computadora = p.id_computadora " +
            "UNION ALL " +
            "SELECT p.id_prestamo, a.nombre, a.apellido, c.codigo, " +
            "'DEVOLUCION', p.fecha_devolucion " +
            "FROM prestamo p " +
            "INNER JOIN alumno a ON a.id_alumno = p.id_alumno " +
            "INNER JOIN computadora c ON c.id_computadora = p.id_computadora " +
            "WHERE p.estado = 'DEVUELTO'" +
            ") ORDER BY fecha_movimiento DESC, tipo ASC, id_prestamo DESC " +
            "LIMIT :limite",
    )
    fun movimientosRecientes(limite: Int): Flow<List<MovimientoReciente>>
}
