package com.ien.prestamoscomputadoras.data.repository

import com.ien.prestamoscomputadoras.data.dao.HistorialDao
import com.ien.prestamoscomputadoras.data.dao.PrestamoHistorial

/** Préstamos para la pantalla "Historial de Préstamos". */
class HistorialRepository(private val historialDao: HistorialDao) {

    /** Préstamos con fecha de préstamo en [desde, hasta), del más reciente al más antiguo. */
    suspend fun listarEntre(desde: Long, hasta: Long): List<PrestamoHistorial> =
        historialDao.listarEntre(desde, hasta)
}
