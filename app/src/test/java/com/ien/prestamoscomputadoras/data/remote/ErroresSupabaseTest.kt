package com.ien.prestamoscomputadoras.data.remote

import com.ien.prestamoscomputadoras.data.repository.PrestamoYaDevueltoException
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

    @Test(expected = CancellationException::class)
    fun cancelacion_noSeTrataComoError() = runTest {
        llamarSupabase<Unit> { throw CancellationException("se cerró la pantalla") }
    }
}
