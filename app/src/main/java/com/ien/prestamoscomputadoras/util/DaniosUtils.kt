package com.ien.prestamoscomputadoras.util

import com.ien.prestamoscomputadoras.data.model.Prestamo
import com.ien.prestamoscomputadoras.data.model.PrestamoHistorial

/**
 * `true` si la tarjeta del préstamo debe mostrar el chip "Observación al entregar": el préstamo
 * tiene observaciones iniciales cargadas (no vacías), esté activo o ya devuelto.
 */
fun tieneObservacionAlEntregar(prestamo: PrestamoHistorial): Boolean =
    !prestamo.observacionesIniciales.isNullOrBlank()

/**
 * `true` si la tarjeta del préstamo debe mostrar el chip "Daño en la devolución": el préstamo
 * ya fue devuelto y su revisión marcó algún problema (no enciende, pantalla con fallas, sin
 * cargador u observaciones de devolución).
 *
 * Un préstamo activo todavía no tiene revisión (sus campos son `null`), así que nunca lo dispara.
 */
fun tieneDanioEnDevolucion(prestamo: PrestamoHistorial): Boolean =
    prestamo.estado == Prestamo.ESTADO_DEVUELTO &&
        (
            prestamo.enciende == false ||
                prestamo.pantallaOk == false ||
                prestamo.cargador == false ||
                !prestamo.observacionesDevolucion.isNullOrBlank()
            )
