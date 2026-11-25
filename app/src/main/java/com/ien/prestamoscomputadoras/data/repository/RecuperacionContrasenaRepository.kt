package com.ien.prestamoscomputadoras.data.repository

import com.ien.prestamoscomputadoras.data.remote.SupabaseProvider
import com.ien.prestamoscomputadoras.data.remote.llamarSupabase
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth

/**
 * Recuperación de contraseña con un código de 6 dígitos que Supabase manda por mail.
 * Nunca se guarda ni se registra (log) el código ni las contraseñas.
 */
interface RecuperacionContrasenaRepository {

    /**
     * Pide que Supabase mande el código a [email]. Responde igual exista o no la cuenta
     * (así no se puede averiguar qué correos están registrados).
     */
    suspend fun solicitarCodigo(email: String): Result<Unit>

    /**
     * Verifica el código. Ojo: si es correcto, Supabase abre una sesión para poder cambiar la
     * contraseña. Esa sesión no debe llevar al Home: hay que terminar el flujo con
     * [cambiarContrasena] o descartarla con [descartarSesion].
     */
    suspend fun verificarCodigo(email: String, codigo: String): Result<Unit>

    /** Cambia la contraseña del usuario de la sesión de recuperación y cierra esa sesión. */
    suspend fun cambiarContrasena(nueva: String): Result<Unit>

    /** Cierra la sesión de recuperación sin cambiar nada (el usuario abandonó el flujo). */
    suspend fun descartarSesion(): Result<Unit>
}

class SupabaseRecuperacionContrasenaRepository(
    private val supabase: SupabaseClient = SupabaseProvider.client,
) : RecuperacionContrasenaRepository {

    override suspend fun solicitarCodigo(email: String): Result<Unit> = llamarSupabase {
        supabase.auth.resetPasswordForEmail(email)
    }

    override suspend fun verificarCodigo(email: String, codigo: String): Result<Unit> = llamarSupabase {
        supabase.auth.verifyEmailOtp(type = OtpType.Email.RECOVERY, email = email, token = codigo)
        Unit
    }

    override suspend fun cambiarContrasena(nueva: String): Result<Unit> = llamarSupabase {
        supabase.auth.updateUser { password = nueva }
        cerrarSesionLocal()
    }

    override suspend fun descartarSesion(): Result<Unit> = llamarSupabase { cerrarSesionLocal() }

    /** Cierra la sesión y, si el servidor no responde, igual la borra del teléfono. */
    private suspend fun cerrarSesionLocal() {
        try {
            supabase.auth.signOut()
        } finally {
            supabase.auth.clearSession()
        }
    }
}
