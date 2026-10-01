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

    /** `null` si no existe. Se usa para mostrar el alumno de un préstamo. */
    @Query("SELECT * FROM alumno WHERE id_alumno = :idAlumno")
    suspend fun buscarPorId(idAlumno: Long): Alumno?

    /** Todos los alumnos, para el selector de "Nuevo Préstamo". */
    @Query("SELECT * FROM alumno ORDER BY apellido, nombre")
    suspend fun listarTodos(): List<Alumno>
}
