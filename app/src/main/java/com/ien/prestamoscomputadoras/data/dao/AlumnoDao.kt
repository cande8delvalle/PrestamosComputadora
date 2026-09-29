package com.ien.prestamoscomputadoras.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ien.prestamoscomputadoras.data.entity.Alumno

@Dao
interface AlumnoDao {

    /** Se usa al registrar un préstamo nuevo. Devuelve el id generado. */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertar(alumno: Alumno): Long

    /** Para reutilizar los datos de un alumno ya registrado. `null` si no existe. */
    @Query("SELECT * FROM alumno WHERE dni = :dni LIMIT 1")
    suspend fun buscarPorDni(dni: String): Alumno?
}
