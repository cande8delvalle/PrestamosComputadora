package com.ien.prestamoscomputadoras.util

import kotlin.time.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime

/**
 * Zona horaria del instituto. Todas las fechas se muestran y se agrupan ("hoy", "esta semana")
 * en esta zona, sin importar la zona configurada en el celular ni la del servidor (UTC).
 */
val ZONA_ARGENTINA: TimeZone = TimeZone.of("America/Argentina/Buenos_Aires")

/** El instante en hora de Argentina. */
fun Instant.enArgentina(): LocalDateTime = toLocalDateTime(ZONA_ARGENTINA)

private fun Int.dosDigitos() = toString().padStart(2, '0')

/** "24/06/2026". */
fun Instant.fechaArgentina(): String = enArgentina().let {
    "${it.day.dosDigitos()}/${it.month.number.dosDigitos()}/${it.year}"
}

/** "09:30". */
fun Instant.horaArgentina(): String = enArgentina().let {
    "${it.hour.dosDigitos()}:${it.minute.dosDigitos()}"
}
