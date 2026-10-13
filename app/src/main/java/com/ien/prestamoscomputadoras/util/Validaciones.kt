package com.ien.prestamoscomputadoras.util

/** Correo: usuario@dominio.extensión (extensión de al menos 2 letras). */
private val CORREO_REGEX = Regex("""^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\.[A-Za-z0-9-]+)*\.[A-Za-z]{2,}$""")

/** `true` si [correo] (sin espacios alrededor) tiene formato de email. La usan Login y Registro. */
fun esCorreoValido(correo: String): Boolean = CORREO_REGEX.matches(correo.trim())
