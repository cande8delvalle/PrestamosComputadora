package com.ien.prestamoscomputadoras.util

import com.ien.prestamoscomputadoras.data.entity.EstadoComputadora
import com.ien.prestamoscomputadoras.data.entity.Prestamo

/**
 * `true` si la tarjeta del préstamo debe mostrar el chip "Observación al entregar": el préstamo
 * tiene [Prestamo.observacionesIniciales] cargadas (no vacías), esté activo o ya devuelto.
 */
fun tieneObservacionAlEntregar(prestamo: Prestamo): Boolean =
    !prestamo.observacionesIniciales.isNullOrBlank()

/**
 * `true` si la tarjeta del préstamo debe mostrar el chip "Daño en la devolución": el préstamo
 * ya fue devuelto y su revisión marcó algún problema (no enciende, pantalla con fallas, sin
 * cargador u observaciones de devolución).
 *
 * Un préstamo activo todavía no tiene [estadoDevolucion] (`null`), así que nunca lo dispara.
 */
fun tieneDanioEnDevolucion(prestamo: Prestamo, estadoDevolucion: EstadoComputadora?): Boolean =
    prestamo.estado == Prestamo.ESTADO_DEVUELTO &&
        estadoDevolucion != null &&
        (
            !estadoDevolucion.enciende ||
                !estadoDevolucion.pantallaOk ||
                !estadoDevolucion.cargador ||
                !estadoDevolucion.observaciones.isNullOrBlank()
            )
