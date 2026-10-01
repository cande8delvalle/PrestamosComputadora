package com.ien.prestamoscomputadoras.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import com.ien.prestamoscomputadoras.data.entity.EstadoComputadora

@Dao
interface EstadoComputadoraDao {

    /**
     * Guarda la revisión del equipo al devolverlo. ABORT: hay como máximo una por préstamo
     * (id_prestamo es único), una segunda lanza `SQLiteConstraintException`.
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertar(estadoComputadora: EstadoComputadora): Long
}
