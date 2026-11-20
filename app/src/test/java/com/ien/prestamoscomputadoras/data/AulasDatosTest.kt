package com.ien.prestamoscomputadoras.data

import com.ien.prestamoscomputadoras.data.model.Aula
import com.ien.prestamoscomputadoras.data.model.NuevaAula
import com.ien.prestamoscomputadoras.data.model.NuevoPrestamo
import com.ien.prestamoscomputadoras.data.model.PrestamoHistorial
import com.ien.prestamoscomputadoras.data.remote.ErrorDatos
import com.ien.prestamoscomputadoras.data.repository.MENSAJE_AULA_CON_PRESTAMOS
import com.ien.prestamoscomputadoras.data.repository.MENSAJE_AULA_DUPLICADA
import com.ien.prestamoscomputadoras.data.repository.errorDeAula
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** Lo de las aulas que no depende de la red: permiso, nombres de columnas y mensajes de error. */
class AulasDatosTest {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    // --- Permiso exclusivo del Admin

    @Test
    fun gestionarAulas_esExclusivoDelAdmin_yNuncaEditablePorPersonal() {
        assertTrue(Permiso.GESTIONAR_AULAS.soloAdmin)
        assertFalse(Permiso.GESTIONAR_AULAS in Permiso.editables)
        assertFalse(Permiso.GESTIONAR_AULAS in Permiso.inicialesPersonal)
        assertEquals("Gestionar Aulas", Permiso.GESTIONAR_AULAS.titulo)
    }

    // --- Mensajes de error del repositorio

    @Test
    fun nombreRepetido_23505_mensajeClaro() {
        val original = ErrorDatos.Duplicado("aula_nombre_unico")
        val e = errorDeAula(original)

        assertTrue(e is ErrorDatos.Duplicado)
        assertEquals("aula_nombre_unico", (e as ErrorDatos.Duplicado).restriccion)
        assertEquals("Ya existe un aula con ese nombre", e.message)
        assertEquals(MENSAJE_AULA_DUPLICADA, e.message)
    }

    @Test
    fun aulaConPrestamos_23503_mensajeClaro() {
        val e = errorDeAula(ErrorDatos.ReferenciaEnUso("prestamo_id_aula_fkey"))

        assertTrue(e is ErrorDatos.ReferenciaEnUso)
        assertEquals("No se puede eliminar: tiene préstamos registrados", e.message)
        assertEquals(MENSAJE_AULA_CON_PRESTAMOS, e.message)
    }

    @Test
    fun otrosErrores_pasanSinCambios() {
        val sinConexion = ErrorDatos.SinConexion()
        assertSame(sinConexion, errorDeAula(sinConexion))
    }

    @Test
    fun referenciaEnUso_sinMensajePropio_dice_estaEnUso() {
        assertEquals("No se puede eliminar: está en uso.", ErrorDatos.ReferenciaEnUso(null).message)
    }

    // --- Modelos: @SerialName igual a las columnas

    @Test
    fun aula_usaLasColumnasDeLaTabla() {
        assertEquals("""{"id_aula":1,"nombre":"Aula 1"}""", json.encodeToString(Aula(1, "Aula 1")))
        assertEquals(Aula(3, "Lab"), json.decodeFromString<Aula>("""{"id_aula":3,"nombre":"Lab"}"""))
        assertEquals("""{"nombre":"Aula 2"}""", json.encodeToString(NuevaAula("Aula 2")))
    }

    @Test
    fun nuevoPrestamo_mandaIdAula() {
        val s = json.encodeToString(
            NuevoPrestamo(idAlumno = 1, idComputadora = 2, idUsuarioPrestamo = 3, idAula = 4),
        )
        assertTrue(s, s.contains(""""id_aula":4"""))
    }

    @Test
    fun historial_leeAulaNombreYAceptaNull() {
        fun fila(aula: String) = """
            {"id_prestamo":1,"estado":"ACTIVO","fecha_prestamo":"2026-09-30T12:00:00Z",
             "ultima_actividad":"2026-09-30T12:00:00Z","id_alumno":1,"alumno_nombre":"Juan",
             "alumno_apellido":"Pérez","alumno_dni":"1","id_computadora":1,"computadora_codigo":"PC-1",
             "id_usuario_prestamo":1,"prestado_por":"Ana López","id_aula":${if (aula == "null") "null" else "2"},
             "aula_nombre":$aula}
        """.trimIndent()

        val conAula = json.decodeFromString<PrestamoHistorial>(fila("\"Laboratorio\""))
        assertEquals("Laboratorio", conAula.aulaNombre)
        assertEquals("Laboratorio", conAula.aulaParaMostrar)

        // Préstamo viejo: el LEFT JOIN devuelve null.
        val sinAula = json.decodeFromString<PrestamoHistorial>(fila("null"))
        assertNull(sinAula.aulaNombre)
        assertEquals("Sin aula", sinAula.aulaParaMostrar)
    }
}
