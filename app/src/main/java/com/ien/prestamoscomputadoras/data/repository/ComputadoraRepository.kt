package com.ien.prestamoscomputadoras.data.repository

import com.ien.prestamoscomputadoras.data.dao.ComputadoraDao
import com.ien.prestamoscomputadoras.data.entity.Computadora

/** Alta de computadoras. */
class ComputadoraRepository(private val computadoraDao: ComputadoraDao) {

    suspend fun insertar(computadora: Computadora): Long = computadoraDao.insertar(computadora)

    /** `null` si no hay ninguna computadora con ese código. */
    suspend fun buscarPorCodigo(codigo: String): Computadora? = computadoraDao.buscarPorCodigo(codigo)

    /** `null` si no hay ninguna computadora con ese id. */
    suspend fun buscarPorId(idComputadora: Long): Computadora? = computadoraDao.buscarPorId(idComputadora)
}
