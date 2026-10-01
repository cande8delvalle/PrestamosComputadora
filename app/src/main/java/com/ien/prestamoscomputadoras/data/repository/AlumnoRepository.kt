package com.ien.prestamoscomputadoras.data.repository

import com.ien.prestamoscomputadoras.data.dao.AlumnoDao
import com.ien.prestamoscomputadoras.data.entity.Alumno

/** Acceso a los alumnos. Los ViewModels usan esta clase en vez de hablar directo con el DAO. */
class AlumnoRepository(private val alumnoDao: AlumnoDao) {

    /** Inserta el alumno y devuelve el id generado. */
    suspend fun insertar(alumno: Alumno): Long = alumnoDao.insertar(alumno)

    /** `null` si no hay ningún alumno con ese DNI. */
    suspend fun buscarPorDni(dni: String): Alumno? = alumnoDao.buscarPorDni(dni)
}
