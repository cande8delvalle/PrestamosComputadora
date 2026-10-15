package com.ien.prestamoscomputadoras.data.repository

import com.ien.prestamoscomputadoras.data.model.Perfil
import com.ien.prestamoscomputadoras.data.remote.ErrorDatos
import com.ien.prestamoscomputadoras.data.remote.SupabaseProvider
import com.ien.prestamoscomputadoras.data.remote.llamarSupabase
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Nombre de la restricción única de `perfil.nombre_usuario` (la crea Postgres por defecto). */
const val RESTRICCION_NOMBRE_USUARIO = "perfil_nombre_usuario_key"

/** Datos del formulario de registro (la contraseña va aparte: nunca se guarda en un modelo). */
data class DatosRegistro(
    val nombre: String,
    val apellido: String,
    val nombreUsuario: String,
    val dni: String,
    val email: String,
)

/** Alta de cuentas, login y sesión. */
interface UsuarioRepository {

    /**
     * Crea la cuenta en Supabase Auth. El perfil lo crea un trigger de la base con los datos
     * de [datos]: la primera cuenta queda Admin y las siguientes, Personal Administrativo.
     * No deja la sesión iniciada: después del registro se va al login.
     */
    suspend fun registrar(datos: DatosRegistro, contrasena: String): Result<Unit>

    /** Inicia sesión con email y contraseña y devuelve el perfil del usuario. */
    suspend fun iniciarSesion(email: String, contrasena: String): Result<Perfil>

    /**
     * Si quedó guardada una sesión de antes (la app se cerró sin cerrar sesión), devuelve su
     * perfil; `null` si no hay sesión guardada.
     */
    suspend fun restaurarSesion(): Result<Perfil?>

    suspend fun cerrarSesion(): Result<Unit>
}

class SupabaseUsuarioRepository(
    private val supabase: SupabaseClient = SupabaseProvider.client,
) : UsuarioRepository {

    override suspend fun registrar(datos: DatosRegistro, contrasena: String): Result<Unit> =
        llamarSupabase {
            try {
                supabase.auth.signUpWith(Email) {
                    email = datos.email
                    password = contrasena
                    // Metadatos que lee el trigger handle_new_user para crear el perfil.
                    data = buildJsonObject {
                        put("nombre", datos.nombre)
                        put("apellido", datos.apellido)
                        put("nombre_usuario", datos.nombreUsuario)
                        put("dni", datos.dni)
                    }
                }
            } catch (e: AuthRestException) {
                // Si el trigger no pudo crear el perfil, Supabase responde "Database error
                // saving new user". Con el email ya validado por Auth, la única restricción
                // única que puede fallar en perfil es nombre_usuario.
                if (e.message.orEmpty().contains("Database error saving new user", ignoreCase = true)) {
                    throw ErrorDatos.Duplicado(RESTRICCION_NOMBRE_USUARIO, e)
                }
                throw e
            }
            // Si el proyecto no pide confirmar el email, signUp ya deja la sesión abierta:
            // se cierra para seguir el flujo de siempre (registro -> login).
            if (supabase.auth.currentSessionOrNull() != null) supabase.auth.signOut()
        }

    override suspend fun iniciarSesion(email: String, contrasena: String): Result<Perfil> =
        llamarSupabase {
            supabase.auth.signInWith(Email) {
                this.email = email
                password = contrasena
            }
            perfilDeLaSesion() ?: run {
                supabase.auth.signOut()
                throw ErrorDatos.Otro(mensaje = "Tu cuenta no tiene un perfil cargado. Avisale al Admin.")
            }
        }

    override suspend fun restaurarSesion(): Result<Perfil?> = llamarSupabase {
        // Espera a que Auth termine de leer la sesión guardada en el teléfono.
        supabase.auth.awaitInitialization()
        if (supabase.auth.currentUserOrNull() == null) null else perfilDeLaSesion()
    }

    override suspend fun cerrarSesion(): Result<Unit> = llamarSupabase { supabase.auth.signOut() }

    /** Perfil del usuario logueado (fila de `perfil` con su auth_user_id). */
    private suspend fun perfilDeLaSesion(): Perfil? {
        val idAuth = supabase.auth.currentUserOrNull()?.id ?: return null
        return supabase.from("perfil")
            .select { filter { eq("auth_user_id", idAuth) } }
            .decodeSingleOrNull<Perfil>()
    }
}
