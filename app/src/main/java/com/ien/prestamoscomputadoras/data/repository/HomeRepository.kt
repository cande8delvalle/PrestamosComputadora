package com.ien.prestamoscomputadoras.data.repository

import com.ien.prestamoscomputadoras.data.dao.HomeDao
import com.ien.prestamoscomputadoras.data.dao.MovimientoReciente
import kotlinx.coroutines.flow.Flow

/** Resumen del día para la pantalla de inicio. Todo es observable: se actualiza solo. */
class HomeRepository(private val homeDao: HomeDao) {

    /** Préstamos en curso (todavía no devueltos), sin importar la fecha. */
    fun activos(): Flow<Int> = homeDao.contarActivos()

    /** Devoluciones registradas en [desde, hasta). */
    fun devueltosEntre(desde: Long, hasta: Long): Flow<Int> = homeDao.contarDevueltosEntre(desde, hasta)

    /** Últimos [limite] préstamos y devoluciones, mezclados, del más reciente al más antiguo. */
    fun movimientosRecientes(limite: Int): Flow<List<MovimientoReciente>> =
        homeDao.movimientosRecientes(limite)
}
