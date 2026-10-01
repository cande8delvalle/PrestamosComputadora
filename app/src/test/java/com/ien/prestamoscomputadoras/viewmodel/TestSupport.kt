package com.ien.prestamoscomputadoras.viewmodel

import com.ien.prestamoscomputadoras.data.dao.AdministradorDao
import com.ien.prestamoscomputadoras.data.dao.AlumnoDao
import com.ien.prestamoscomputadoras.data.dao.ComputadoraDao
import com.ien.prestamoscomputadoras.data.dao.EstadoComputadoraDao
import com.ien.prestamoscomputadoras.data.dao.HomeDao
import com.ien.prestamoscomputadoras.data.dao.MovimientoReciente
import com.ien.prestamoscomputadoras.data.dao.RolDao
import com.ien.prestamoscomputadoras.data.dao.PrestamoDao
import com.ien.prestamoscomputadoras.data.entity.Administrador
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

class FakeAdministradorDao : AdministradorDao {
    val administradores = mutableListOf<Administrador>()

    override suspend fun insertar(administrador: Administrador): Long {
        // Igual que el trigger `un_solo_admin_insert` de la base real.
        check(administrador.idRol != Rol.ID_ADMIN || !existeAdmin()) { "Solo puede existir un Admin" }
        val id = administradores.size + 1L
        administradores += administrador.copy(idAdministrador = id)
        return id
    }

    override suspend fun buscarPorEmail(email: String) = administradores.firstOrNull { it.email == email }

    override suspend fun buscarPorNombreUsuario(nombreUsuario: String) =
        administradores.firstOrNull { it.nombreUsuario.equals(nombreUsuario, ignoreCase = true) }

    override suspend fun existeNombreUsuario(nombreUsuario: String) =
        buscarPorNombreUsuario(nombreUsuario) != null

    override suspend fun existeEmail(email: String) = buscarPorEmail(email) != null

    override suspend fun existeAdmin() = administradores.any { it.idRol == Rol.ID_ADMIN }
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

    override suspend fun actualizarDevolucion(idPrestamo: Long, fechaDevolucion: Long): Int {
        val i = prestamos.indexOfFirst { it.idPrestamo == idPrestamo && it.estado == Prestamo.ESTADO_ACTIVO }
        if (i == -1) return 0
        prestamos[i] = prestamos[i].copy(fechaDevolucion = fechaDevolucion, estado = Prestamo.ESTADO_DEVUELTO)
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
 * Todos los préstamos son del alumno "Juan Pérez" y la computadora se llama "PC-<id>".
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

    override fun movimientosEntre(desde: Long, hasta: Long, limite: Int): Flow<List<MovimientoReciente>> =
        prestamos.map { lista ->
            lista
                .map {
                    val fecha = if (it.estado == Prestamo.ESTADO_DEVUELTO) it.fechaDevolucion!! else it.fechaPrestamo
                    MovimientoReciente("Juan", "Pérez", "PC-${it.idComputadora}", it.estado, fecha)
                }
                .filter { it.fechaMovimiento in desde until hasta }
                .sortedByDescending { it.fechaMovimiento }
                .take(limite)
        }
}

/** Rol de cada administrador y permisos por rol, observables como en Room. */
class FakeRolDao(
    rolesPorAdministrador: Map<Long, Long> = emptyMap(),
    permisosIniciales: Set<RolPermiso> = emptySet(),
) : RolDao {
    val roles = MutableStateFlow(rolesPorAdministrador)
    val permisos = MutableStateFlow(permisosIniciales)

    override fun rolDeAdministrador(idAdministrador: Long): Flow<Long?> = roles.map { it[idAdministrador] }

    override fun permisosDeRol(idRol: Long): Flow<List<String>> =
        permisos.map { set -> set.filter { it.idRol == idRol }.map { it.permiso } }

    override suspend fun habilitarPermiso(rolPermiso: RolPermiso) {
        permisos.value = permisos.value + rolPermiso
    }

    override suspend fun deshabilitarPermiso(idRol: Long, permiso: String) {
        permisos.value = permisos.value - RolPermiso(idRol, permiso)
    }
}
