package com.ien.prestamoscomputadoras.data.dao

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

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
     * Últimos [limite] préstamos, uno por fila, ordenados por lo último que pasó con cada uno:
     * la devolución si ya se devolvió, si no la entrega. Trae la misma fila completa que el
     * Historial, así el detalle de cada préstamo se abre sin volver a consultar la base.
     */
    @Query(
        SELECT_PRESTAMO_HISTORIAL +
            "ORDER BY COALESCE(p.fecha_devolucion, p.fecha_prestamo) DESC, p.id_prestamo DESC " +
            "LIMIT :limite",
    )
    fun prestamosRecientes(limite: Int): Flow<List<PrestamoHistorial>>
}
