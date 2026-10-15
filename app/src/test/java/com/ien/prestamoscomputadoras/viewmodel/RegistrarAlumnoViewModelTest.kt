package com.ien.prestamoscomputadoras.viewmodel

import com.ien.prestamoscomputadoras.data.model.Alumno
import com.ien.prestamoscomputadoras.data.remote.ErrorDatos
import com.ien.prestamoscomputadoras.data.remote.MENSAJE_SIN_CONEXION
import com.ien.prestamoscomputadoras.data.remote.MENSAJE_SIN_PERMISO
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RegistrarAlumnoViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repo = FakeAlumnoRepository(
        mutableListOf(Alumno(idAlumno = 1, nombre = "María", apellido = "Gonzales", dni = "46111222")),
    )

    private fun registrar(dni: String) = RegistrarAlumnoViewModel(repo).apply {
        onNombreChange(" Juan ")
        onApellidoChange(" Pérez ")
        onDniChange(dni)
        onRegistrarClick()
    }

    @Test
    fun valido_guardaYMuestraModal() {
        val vm = registrar("40123456")
        assertEquals(Alumno(idAlumno = 2, nombre = "Juan", apellido = "Pérez", dni = "40123456"), repo.alumnos.last())
        assertTrue(vm.uiState.mostrarModalExito)
        assertFalse(vm.uiState.isLoading)
    }

    @Test
    fun dniRepetido_muestraErrorYNoGuarda() {
        val vm = registrar("46111222")
        assertEquals(ERROR_DNI_ALUMNO_DUPLICADO, vm.uiState.errorDni)
        assertEquals(1, repo.alumnos.size)
    }

    @Test
    fun noAdmin_mensajeDePermiso() {
        // La búsqueda funciona (todos pueden leer) pero el insert lo rechaza la base (RLS).
        val vm = RegistrarAlumnoViewModel(
            object : com.ien.prestamoscomputadoras.data.repository.AlumnoRepository {
                override suspend fun insertar(alumno: Alumno) = Result.failure<Long>(ErrorDatos.SinPermiso())
                override suspend fun buscarPorDni(dni: String) = Result.success<Alumno?>(null)
            },
        ).apply {
            onNombreChange("Juan")
            onApellidoChange("Pérez")
            onDniChange("40123456")
            onRegistrarClick()
        }
        assertEquals(MENSAJE_SIN_PERMISO, vm.uiState.errorGeneral)
        assertNull(vm.uiState.errorDni)
        assertFalse(vm.uiState.mostrarModalExito)
    }

    @Test
    fun sinInternet_mensajeClaro() {
        repo.falla = ErrorDatos.SinConexion()
        val vm = registrar("40123456")
        assertEquals(MENSAJE_SIN_CONEXION, vm.uiState.errorGeneral)
        assertFalse(vm.uiState.isLoading)
    }
}
