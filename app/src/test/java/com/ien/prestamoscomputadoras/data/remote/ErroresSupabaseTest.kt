package com.ien.prestamoscomputadoras.data.remote

import com.ien.prestamoscomputadoras.data.repository.PrestamoYaDevueltoException
import io.github.jan.supabase.auth.exception.AuthErrorCode
import java.io.IOException
import java.net.UnknownHostException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ErroresSupabaseTest {

    @Test
    fun sinInternet_seTraduceAMensajeClaro() = runTest {
        listOf(UnknownHostException("xxxx.supabase.co"), IOException("Connection reset")).forEach { e ->
            val resultado = llamarSupabase<Unit> { throw e }
            val error = resultado.exceptionOrNull()
            assertTrue(error is ErrorDatos.SinConexion)
            assertEquals(MENSAJE_SIN_CONEXION, error?.message)
        }
    }

    @Test
    fun exito_devuelveElValor() = runTest {
        assertEquals(42, llamarSupabase { 42 }.getOrNull())
    }

    @Test
    fun errorDesconocido_mensajeGenerico() = runTest {
        val error = llamarSupabase<Unit> { error("algo raro") }.exceptionOrNull()
        assertTrue(error is ErrorDatos.Otro)
        assertEquals(MENSAJE_ERROR_INESPERADO, error?.message)
    }

    @Test
    fun erroresDeNegocio_pasanSinTraducir() = runTest {
        val error = llamarSupabase<Unit> { throw PrestamoYaDevueltoException() }.exceptionOrNull()
        assertTrue(error is PrestamoYaDevueltoException)
    }

    @Test
    fun errorDeAuth_codigoInvalidoOVencido() {
        val error = traducirErrorAuth(AuthErrorCode.OtpExpired, 403, "otp_expired")
        assertTrue(error is ErrorDatos.CodigoInvalido)
        assertEquals(MENSAJE_CODIGO_INVALIDO, error.message)
    }

    @Test
    fun errorDeAuth_limiteDeEnvios_oDeIntentos() {
        listOf(
            traducirErrorAuth(AuthErrorCode.OverEmailSendRateLimit, 429, "over_email_send_rate_limit"),
            traducirErrorAuth(AuthErrorCode.OverRequestRateLimit, 429, "over_request_rate_limit"),
            // Sin código conocido, el 429 alcanza.
            traducirErrorAuth(null, 429, "algo"),
        ).forEach { error ->
            assertTrue(error is ErrorDatos.DemasiadosIntentos)
            assertEquals(MENSAJE_DEMASIADOS_INTENTOS, error.message)
        }
    }

    @Test
    fun errorDeAuth_contrasenaDebil_yContrasenaIgualALaAnterior() {
        assertTrue(traducirErrorAuth(AuthErrorCode.WeakPassword, 422, "weak_password") is ErrorDatos.ContrasenaDebil)
        val igual = traducirErrorAuth(AuthErrorCode.SamePassword, 422, "same_password")
        assertTrue(igual is ErrorDatos.ContrasenaIgualAnterior)
        assertEquals(MENSAJE_CONTRASENA_IGUAL_ANTERIOR, igual.message)
    }

    @Test
    fun errorDeAuth_cuentaSinConfirmar_porCodigoOPorTexto() {
        listOf(
            traducirErrorAuth(AuthErrorCode.EmailNotConfirmed, 400, "email_not_confirmed"),
            traducirErrorAuth(null, 400, "Email not confirmed"),
        ).forEach { error ->
            assertTrue(error is ErrorDatos.EmailSinConfirmar)
            assertEquals("Revisá tu correo y confirmá tu cuenta antes de ingresar", error.message)
        }
    }

    @Test
    fun errorDeAuth_credencialesYSesion_siguenIgual() {
        assertTrue(traducirErrorAuth(AuthErrorCode.InvalidCredentials, 400, "x") is ErrorDatos.CredencialesInvalidas)
        assertTrue(traducirErrorAuth(AuthErrorCode.SessionExpired, 401, "x") is ErrorDatos.SesionVencida)
        assertTrue(traducirErrorAuth(null, 500, "x") is ErrorDatos.Otro)
    }

    @Test(expected = CancellationException::class)
    fun cancelacion_noSeTrataComoError() = runTest {
        llamarSupabase<Unit> { throw CancellationException("se cerró la pantalla") }
    }
}
