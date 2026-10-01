package com.ien.prestamoscomputadoras.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ien.prestamoscomputadoras.data.entity.Computadora
import kotlinx.coroutines.flow.Flow

@Dao
interface ComputadoraDao {

    /**
     * Da de alta una computadora con el id que trae (no es autogenerado). ABORT: si el id o
     * el código ya existen lanza `SQLiteConstraintException` (conviene chequear antes).
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertar(computadora: Computadora): Long

    /** Se re-emite automáticamente cada vez que cambia la tabla. */
    @Query("SELECT * FROM computadora ORDER BY codigo")
    fun listarTodas(): Flow<List<Computadora>>

    /** Busca por el código que contiene el QR. `null` si no existe. */
    @Query("SELECT * FROM computadora WHERE codigo = :codigo LIMIT 1")
    suspend fun buscarPorCodigo(codigo: String): Computadora?

    /** `null` si no existe. */
    @Query("SELECT * FROM computadora WHERE id_computadora = :idComputadora")
    suspend fun buscarPorId(idComputadora: Long): Computadora?

    /** Computadoras sin un préstamo ACTIVO, para el selector de "Nuevo Préstamo". */
    @Query(
        "SELECT * FROM computadora WHERE id_computadora NOT IN " +
            "(SELECT id_computadora FROM prestamo WHERE estado = 'ACTIVO') ORDER BY codigo",
    )
    suspend fun listarDisponibles(): List<Computadora>
}
