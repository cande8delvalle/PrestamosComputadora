package com.ien.prestamoscomputadoras.util

import java.security.MessageDigest

/**
 * Devuelve el hash SHA-256 de [password] en hexadecimal (64 caracteres en minúscula).
 * Es determinístico: para el login se hashea lo ingresado y se compara con el guardado.
 */
// NOTA: para producción real, reemplazar por bcrypt o Argon2, que son más
// seguros que SHA-256 simple para contraseñas.
fun hashPassword(password: String): String =
    MessageDigest.getInstance("SHA-256")
        .digest(password.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
