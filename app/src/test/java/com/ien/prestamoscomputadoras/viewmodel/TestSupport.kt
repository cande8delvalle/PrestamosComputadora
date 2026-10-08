package com.ien.prestamoscomputadoras.viewmodel

import com.ien.prestamoscomputadoras.data.dao.UsuarioDao
import com.ien.prestamoscomputadoras.data.dao.AlumnoDao
import com.ien.prestamoscomputadoras.data.dao.ComputadoraDao
import com.ien.prestamoscomputadoras.data.dao.EstadoComputadoraDao
import com.ien.prestamoscomputadoras.data.dao.HomeDao
import com.ien.prestamoscomputadoras.data.dao.PrestamoHistorial
import com.ien.prestamoscomputadoras.data.dao.RolDao
import com.ien.prestamoscomputadoras.data.dao.PrestamoDao
import com.ien.prestamoscomputadoras.data.entity.Usuario
import com.ien.prestamoscomputadoras.data.entity.Alumno
import com.ien.prestamoscomputadoras.data.entity.Computadora
import com.ien.prestamoscomputadoras.data.entity.EstadoComputadora
import com.ien.prestamoscomputadoras.data.entity.Prestamo
import com.ien.prestamoscomputadoras.data.entity.Rol
import com.ien.prestamoscomputadoras.data.entity.RolPermiso
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Reemplaza Dispatchers.Main (que no existe en la JVM) por un dispatcher que corre las
 * corrutinas al instante: como los DAOs falsos no suspenden, cada `viewModelScope.launch`
 * termina antes de que vuelva la llamada al ViewModel.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(UnconfinedTestDispatcher())
    override fun finished(description: Description) = Dispatchers.resetMain()
}

class FakeUsuarioDao : UsuarioDao {
    val usuarios = mutableListOf<Usuario>()

    override suspend fun insertar(usuario: Usuario): Long {
        // Igual que el trigger `un_solo_admin_insert` de la base real.
        check(usuario.idRol != Rol.ID_ADMIN || !existeAdmin()) { "Solo puede existir un Admin" }
        val id = usuarios.size + 1L
        usuarios += usuario.copy(idUsuario = id)
        return id
    }

    override suspend fun buscarPorEmail(email: String) = usuarios.firstOrNull { it.email == email }

    override suspend fun buscarPorNombreUsuario(nombreUsuario: String) =
        usuarios.firstOrNull { it.nombreUsuario.equals(nombreUsuario, ignoreCase = true) }

    override suspend fun existeNombreUsuario(nombreUsuario: String) =
        buscarPorNombreUsuario(nombreUsuario) != null

    override suspend fun existeEmail(email: String) = buscarPorEmail(email) != null

    override suspend fun existeAdmin() = usuarios.any { it.idRol == Rol.ID_ADMIN }
}

class FakeAlumnoDao(val alumnos: MutableList<Alumno> = mutableListOf()) : AlumnoDao {
    override suspend fun insertar(alumno: Alumno): Long {
        val id = alumnos.size + 1L
        alumnos += alumno.copy(idAlumno = id)
        return id
    }

    override suspend fun buscarPorDni(dni: String) = alumnos.firstOrNull { it.dni == dni }

    override suspend fun buscarPorId(idAlumno: Long) = alumnos.firstOrNull { it.idAlumno == idAlumno }

    override suspend fun listarTodos(): List<Alumno> = alumnos.toList()
}

/** Guarda los préstamos y calcula las computadoras disponibles igual que la query real. */
class FakePrestamoYComputadoraDao(
    computadorasIniciales: List<Computadora> = emptyList(),
) : PrestamoDao, ComputadoraDao {
    val computadoras = computadorasIniciales.toMutableList()
    val prestamos = mutableListOf<Prestamo>()

    override suspend fun insertar(computadora: Computadora): Long {
        computadoras += computadora
        return computadora.idComputadora
    }

    override suspend fun buscarPorId(idComputadora: Long) =
        computadoras.firstOrNull { it.idComputadora == idComputadora }

    override suspend fun insertar(prestamo: Prestamo): Long {
        val id = prestamos.size + 1L
        prestamos += prestamo.copy(idPrestamo = id)
        return id
    }

    override suspend fun buscarPrestamoActivoPorCodigoComputadora(codigo: String): Prestamo? {
        val computadora = computadoras.firstOrNull { it.codigo == codigo } ?: return null
        return prestamos.firstOrNull {
            it.idComputadora == computadora.idComputadora && it.estado == Prestamo.ESTADO_ACTIVO
        }
    }

    override suspend fun actualizarDevolucion(
        idPrestamo: Long,
        fechaDevolucion: Long,
        idUsuarioDevolucion: Long,
    ): Int {
        val i = prestamos.indexOfFirst { it.idPrestamo == idPrestamo && it.estado == Prestamo.ESTADO_ACTIVO }
        if (i == -1) return 0
        prestamos[i] = prestamos[i].copy(
            fechaDevolucion = fechaDevolucion,
            estado = Prestamo.ESTADO_DEVUELTO,
            idUsuarioDevolucion = idUsuarioDevolucion,
        )
        return 1
    }

    override fun listarTodas(): Flow<List<Computadora>> = flowOf(computadoras.toList())

    override suspend fun buscarPorCodigo(codigo: String) = computadoras.firstOrNull { it.codigo == codigo }

    override suspend fun listarDisponibles(): List<Computadora> {
        val prestadas = prestamos.filter { it.estado == Prestamo.ESTADO_ACTIVO }.map { it.idComputadora }
        return computadoras.filter { it.idComputadora !in prestadas }
    }
}

class FakeEstadoComputadoraDao : EstadoComputadoraDao {
    val estados = mutableListOf<EstadoComputadora>()

    override suspend fun insertar(estadoComputadora: EstadoComputadora): Long {
        val id = estados.size + 1L
        estados += estadoComputadora.copy(idEstado = id)
        return id
    }
}

/**
 * Calcula igual que las queries reales a partir de una lista de préstamos observable.
 * Todos los préstamos son del alumno "Juan Pérez" (DNI 46111222) y la computadora se llama
 * "PC-<id>". Los entrega "Ana López" y los recibe "Marta Ruiz".
 */
class FakeHomeDao : HomeDao {
    val prestamos = MutableStateFlow<List<Prestamo>>(emptyList())

    override fun contarActivos(): Flow<Int> =
        prestamos.map { lista -> lista.count { it.estado == Prestamo.ESTADO_ACTIVO } }

    override fun contarDevueltosEntre(desde: Long, hasta: Long): Flow<Int> =
        prestamos.map { lista ->
            lista.count {
                it.estado == Prestamo.ESTADO_DEVUELTO && it.fechaDevolucion!! in desde until hasta
            }
        }

    override fun prestamosRecientes(limite: Int): Flow<List<PrestamoHistorial>> =
        prestamos.map { lista ->
            lista
                .sortedWith(
                    compareByDescending<Prestamo> { it.fechaDevolucion ?: it.fechaPrestamo }
                        .thenByDescending { it.idPrestamo },
                )
                .take(limite)
                .map {
                    val devuelto = it.idUsuarioDevolucion != null
                    PrestamoHistorial(
                        prestamo = it,
                        revision = null,
                        alumnoNombre = "Juan",
                        alumnoApellido = "Pérez",
                        alumnoDni = "46111222",
                        codigoComputadora = "PC-${it.idComputadora}",
                        usuarioPrestamoNombre = "Ana",
                        usuarioPrestamoApellido = "López",
                        usuarioDevolucionNombre = if (devuelto) "Marta" else null,
                        usuarioDevolucionApellido = if (devuelto) "Ruiz" else null,
                    )
                }
        }
}

/** Rol de cada usuario y permisos por rol, observables como en Room. */
class FakeRolDao(
    rolesPorUsuario: Map<Long, Long> = emptyMap(),
    permisosIniciales: Set<RolPermiso> = emptySet(),
) : RolDao {
    val roles = MutableStateFlow(rolesPorUsuario)
    val permisos = MutableStateFlow(permisosIniciales)

    override fun rolDeUsuario(idUsuario: Long): Flow<Long?> = roles.map { it[idUsuario] }

    override fun permisosDeRol(idRol: Long): Flow<List<String>> =
        permisos.map { set -> set.filter { it.idRol == idRol }.map { it.permiso } }

    override suspend fun habilitarPermiso(rolPermiso: RolPermiso) {
        permisos.value = permisos.value + rolPermiso
    }

    override suspend fun deshabilitarPermiso(idRol: Long, permiso: String) {
        permisos.value = permisos.value - RolPermiso(idRol, permiso)
    }
}
