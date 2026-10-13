package com.ien.prestamoscomputadoras.viewmodel

import kotlin.time.Instant

/**
 * Un aviso para mostrar en un diálogo centrado (ui/components/AvisoDialogo). El ViewModel
 * decide el texto; la pantalla solo lo dibuja.
 */
data class AvisoUi(val titulo: String, val mensaje: String)

const val TITULO_SIN_PRESTAMO = "Esta computadora no tiene préstamo"
const val TITULO_PRESTAMO_YA_DEVUELTO = "Este préstamo ya fue devuelto"
const val TITULO_COMPUTADORA_YA_PRESTADA = "Esa computadora ya está prestada"

/** a) La computadora nunca tuvo préstamos. */
fun avisoNuncaPrestada(codigo: String) = AvisoUi(
    titulo = TITULO_SIN_PRESTAMO,
    mensaje = "$codigo todavía no tiene ningún préstamo registrado. Revisá que sea el equipo correcto.",
)

/** b) Tuvo préstamos pero ninguno activo: ya fue devuelta. La fecha va en hora de Argentina. */
fun avisoYaDevuelta(codigo: String, ultimaDevolucion: Instant?) = AvisoUi(
    titulo = TITULO_SIN_PRESTAMO,
    mensaje = if (ultimaDevolucion != null) {
        "$codigo ya figura como devuelta (última devolución: ${formatearFechaHoraDevolucion(ultimaDevolucion)}). " +
            "Revisá que sea el equipo correcto."
    } else {
        "$codigo ya figura como devuelta. Revisá que sea el equipo correcto."
    },
)

/** Dos celulares a la vez: otro registró la devolución mientras este confirmaba. */
fun avisoPrestamoYaDevuelto(codigo: String) = AvisoUi(
    titulo = TITULO_PRESTAMO_YA_DEVUELTO,
    mensaje = "Otro usuario registró la devolución de $codigo hace un momento. No hace falta registrarla de nuevo.",
)

/** Dos celulares a la vez: otro prestó la computadora mientras este completaba el formulario. */
fun avisoComputadoraYaPrestada(codigo: String) = AvisoUi(
    titulo = TITULO_COMPUTADORA_YA_PRESTADA,
    mensaje = "$codigo ya tiene un préstamo activo: otro usuario la prestó hace un momento. Elegí otra computadora.",
)
