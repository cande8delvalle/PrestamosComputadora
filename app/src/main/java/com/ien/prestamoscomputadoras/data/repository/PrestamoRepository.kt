package com.ien.prestamoscomputadoras.data.repository

import com.ien.prestamoscomputadoras.data.dao.AlumnoDao
import com.ien.prestamoscomputadoras.data.dao.ComputadoraDao
import com.ien.prestamoscomputadoras.data.dao.PrestamoDao
import com.ien.prestamoscomputadoras.data.entity.Alumno
import com.ien.prestamoscomputadoras.data.entity.Computadora
import com.ien.prestamoscomputadoras.data.entity.Prestamo

/** Todo lo que necesita la pantalla "Nuevo Préstamo": alumnos, equipos libres y el alta. */
class PrestamoRepository(
    private val prestamoDao: PrestamoDao,
    private val computadoraDao: ComputadoraDao,
    private val alumnoDao: AlumnoDao,
) {

    /** Inserta el préstamo y devuelve el id generado. */
    suspend fun insertar(prestamo: Prestamo): Long = prestamoDao.insertar(prestamo)

    suspend fun listarAlumnos(): List<Alumno> = alumnoDao.listarTodos()

    /** Computadoras sin un préstamo ACTIVO. */
    suspend fun listarComputadorasDisponibles(): List<Computadora> =
        computadoraDao.listarDisponibles()
}
