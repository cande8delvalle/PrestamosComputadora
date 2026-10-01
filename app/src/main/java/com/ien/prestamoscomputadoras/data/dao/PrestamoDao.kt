package com.ien.prestamoscomputadoras.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ien.prestamoscomputadoras.data.entity.Prestamo

@Dao
interface PrestamoDao {

    /** Registra un préstamo nuevo. Devuelve el id generado. */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertar(prestamo: Prestamo): Long

    /** Préstamo ACTIVO de la computadora con ese código (el que trae el QR). `null` si no hay. */
    @Query(
        "SELECT p.* FROM prestamo p INNER JOIN computadora c ON p.id_computadora = c.id_computadora " +
            "WHERE c.codigo = :codigo AND p.estado = 'ACTIVO' LIMIT 1",
    )
    suspend fun buscarPrestamoActivoPorCodigoComputadora(codigo: String): Prestamo?

    /**
     * Marca el préstamo como DEVUELTO. Solo actualiza si seguía ACTIVO: devuelve la cantidad de
     * filas afectadas (0 = no existe o ya estaba devuelto).
     */
    @Query(
        "UPDATE prestamo SET fecha_devolucion = :fechaDevolucion, estado = 'DEVUELTO' " +
            "WHERE id_prestamo = :idPrestamo AND estado = 'ACTIVO'",
    )
    suspend fun actualizarDevolucion(idPrestamo: Long, fechaDevolucion: Long): Int
}
