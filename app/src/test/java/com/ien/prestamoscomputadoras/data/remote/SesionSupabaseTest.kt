package com.ien.prestamoscomputadoras.data.remote

import com.ien.prestamoscomputadoras.data.Permiso
import com.ien.prestamoscomputadoras.data.repository.AccesoUsuario
import com.ien.prestamoscomputadoras.util.SesionActual
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * llamarConSesion: lo que corre cada vez que la app consulta la base con el usuario logueado
 * (por ejemplo, al leer los permisos al volver de segundo plano).
 */
class SesionSupabaseTest {

    /** Sesión falsa: cuenta cuántas veces se esperó y se refrescó el token. */
    private class FakeGuardia(var errorAlEsperar: ErrorDatos? = null) : GuardiaSesion {
        val eventos = mutableListOf<String>()
        override suspend fun esperarSesion() {
            eventos += "esperar"
            errorAlEsperar?.let { throw it }
        }
        override suspend fun refrescar() {
            eventos += "refrescar"
        }
    }

    private val permisosPersonal = AccesoUsuario(esAdmin = false, permisos = Permiso.inicialesPersonal)

    @After
    fun tearDown() {
        SesionActual.consumirAvisoSesionVencida()
    }

    @Test
    fun esperaLaSesionAntesDeConsultar() = runTest {
        val guardia = FakeGuardia()
        llamarConSesion(guardia) { guardia.eventos += "consulta" }
        assertEquals(listOf("esperar", "consulta"), guardia.eventos)
    }

    @Test
    fun sesionVencidaAlCargarPermisos_refrescaYReintentaUnaVez() = runTest {
        // La consulta salió sin token válido (rol anon -> 42501 con 401): la primera vez falla.
        val guardia = FakeGuardia()
        var intentos = 0
        val resultado = llamarConSesion(guardia) {
            intentos++
            if (intentos == 1) throw ErrorDatos.SesionVencida()
            permisosPersonal
        }

        assertEquals(permisosPersonal, resultado.getOrNull()) // no se vacían: llegan bien
        assertEquals(2, intentos)
        assertEquals(1, guardia.eventos.count { it == "refrescar" })
        assertFalse(SesionActual.sesionVencida.value)
    }

    @Test
    fun siSigueVencidaDespuesDeRefrescar_avisaSesionVencida() = runTest {
        val guardia = FakeGuardia()
        var intentos = 0
        val resultado = llamarConSesion<AccesoUsuario>(guardia) {
            intentos++
            throw ErrorDatos.SesionVencida()
        }

        assertTrue(resultado.exceptionOrNull() is ErrorDatos.SesionVencida)
        assertEquals(MENSAJE_SESION_VENCIDA, resultado.exceptionOrNull()?.message)
        assertEquals(2, intentos) // un solo reintento, no un loop
        assertEquals(1, guardia.eventos.count { it == "refrescar" })
        assertTrue(SesionActual.sesionVencida.value) // la app lleva al login
    }

    @Test
    fun permisoDeRolConSesionValida_esSinPermiso() = runTest {
        // 42501 con sesión válida (403): después de refrescar sigue igual -> es el rol.
        val guardia = FakeGuardia()
        val resultado = llamarConSesion<Unit>(guardia) { throw ErrorDatos.SinPermiso() }

        assertEquals(MENSAJE_SIN_PERMISO, resultado.exceptionOrNull()?.message)
        assertFalse(SesionActual.sesionVencida.value)
    }

    @Test
    fun sinConexion_noRefrescaNiDiceSoloElAdmin() = runTest {
        val guardia = FakeGuardia()
        val resultado = llamarConSesion<Unit>(guardia) { throw java.io.IOException("sin red") }

        assertEquals(MENSAJE_SIN_CONEXION, resultado.exceptionOrNull()?.message)
        assertEquals(0, guardia.eventos.count { it == "refrescar" })
    }

    @Test
    fun sinSesionAlEsperar_noConsulta() = runTest {
        val guardia = FakeGuardia(errorAlEsperar = ErrorDatos.SesionVencida())
        var consulto = false
        val resultado = llamarConSesion(guardia) { consulto = true }

        assertFalse(consulto)
        assertTrue(resultado.exceptionOrNull() is ErrorDatos.SesionVencida)
    }
}
