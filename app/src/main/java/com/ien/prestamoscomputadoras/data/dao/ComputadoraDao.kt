package com.ien.prestamoscomputadoras.data.dao

import androidx.room.Dao
import androidx.room.Query
import com.ien.prestamoscomputadoras.data.entity.Computadora
import kotlinx.coroutines.flow.Flow

/**
 * Solo consulta y actualización. No hay inserción a propósito: la carga inicial de
 * computadoras se hace externamente, directo en la base de datos.
 */
@Dao
interface ComputadoraDao {

    /** Se re-emite automáticamente cada vez que cambia la tabla. */
    @Query("SELECT * FROM computadora ORDER BY codigo")
    fun listarTodas(): Flow<List<Computadora>>

    /** Busca por el código que contiene el QR. `null` si no existe. */
    @Query("SELECT * FROM computadora WHERE codigo = :codigo LIMIT 1")
    suspend fun buscarPorCodigo(codigo: String): Computadora?

    /** Devuelve la cantidad de filas actualizadas (0 si el id no existe). */
    @Query("UPDATE computadora SET disponible = :disponible WHERE id_computadora = :idComputadora")
    suspend fun actualizarDisponible(idComputadora: Long, disponible: Boolean): Int
}
