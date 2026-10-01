package com.ien.prestamoscomputadoras.data.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Un ítem de "Actividad reciente": el último movimiento (préstamo o devolución) de un préstamo. */
data class MovimientoReciente(
    @ColumnInfo(name = "alumno_nombre") val alumnoNombre: String,
    @ColumnInfo(name = "alumno_apellido") val alumnoApellido: String,
    @ColumnInfo(name = "codigo_computadora") val codigoComputadora: String,
    /** [com.ien.prestamoscomputadoras.data.entity.Prestamo.ESTADO_ACTIVO] o `ESTADO_DEVUELTO`. */
    val estado: String,
    /** Epoch ms de la devolución si está DEVUELTO, si no el del préstamo. */
    @ColumnInfo(name = "fecha_movimiento") val fechaMovimiento: Long,
)

/**
 * Datos de la pantalla de inicio. Devuelven [Flow]: Room vuelve a emitir cada vez que cambia
 * la tabla, así Home se actualiza sola al volver de registrar un préstamo o una devolución.
 */
@Dao
interface HomeDao {

    /** Préstamos en curso (estado ACTIVO). */
    @Query("SELECT COUNT(*) FROM prestamo WHERE estado = 'ACTIVO'")
    fun contarActivos(): Flow<Int>

    /** Préstamos devueltos con fecha de devolución en [desde, hasta). */
    @Query(
        "SELECT COUNT(*) FROM prestamo WHERE estado = 'DEVUELTO' " +
            "AND fecha_devolucion >= :desde AND fecha_devolucion < :hasta",
    )
    fun contarDevueltosEntre(desde: Long, hasta: Long): Flow<Int>

    /**
     * Últimos [limite] préstamos cuyo último movimiento cae en [desde, hasta), del más reciente
     * al más antiguo. Un préstamo devuelto aparece por su devolución; uno activo, por su entrega.
     */
    @Query(
        "SELECT a.nombre AS alumno_nombre, a.apellido AS alumno_apellido, " +
            "c.codigo AS codigo_computadora, p.estado AS estado, " +
            "CASE WHEN p.estado = 'DEVUELTO' THEN p.fecha_devolucion ELSE p.fecha_prestamo END " +
            "AS fecha_movimiento " +
            "FROM prestamo p " +
            "INNER JOIN alumno a ON a.id_alumno = p.id_alumno " +
            "INNER JOIN computadora c ON c.id_computadora = p.id_computadora " +
            "WHERE fecha_movimiento >= :desde AND fecha_movimiento < :hasta " +
            "ORDER BY fecha_movimiento DESC, p.id_prestamo DESC " +
            "LIMIT :limite",
    )
    fun movimientosEntre(desde: Long, hasta: Long, limite: Int): Flow<List<MovimientoReciente>>
}
