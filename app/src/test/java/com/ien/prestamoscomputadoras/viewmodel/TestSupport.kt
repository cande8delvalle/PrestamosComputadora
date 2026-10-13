package com.ien.prestamoscomputadoras.viewmodel

import com.ien.prestamoscomputadoras.data.Permiso
import com.ien.prestamoscomputadoras.data.model.Alumno
import com.ien.prestamoscomputadoras.data.model.Computadora
import com.ien.prestamoscomputadoras.data.model.EstadoComputadora
import com.ien.prestamoscomputadoras.data.model.NuevoPrestamo
import com.ien.prestamoscomputadoras.data.model.Perfil
import com.ien.prestamoscomputadoras.data.model.Prestamo
import com.ien.prestamoscomputadoras.data.model.PrestamoHistorial
import com.ien.prestamoscomputadoras.data.model.ResumenHoy
import com.ien.prestamoscomputadoras.data.model.Rol
import com.ien.prestamoscomputadoras.data.model.RolPermiso
import com.ien.prestamoscomputadoras.data.remote.ErrorDatos
import com.ien.prestamoscomputadoras.data.repository.AccesoUsuario
import com.ien.prestamoscomputadoras.data.repository.AlumnoRepository
import com.ien.prestamoscomputadoras.data.repository.BusquedaDevolucion
import com.ien.prestamoscomputadoras.data.repository.ComputadoraRepository
import com.ien.prestamoscomputadoras.data.repository.DatosRegistro
import com.ien.prestamoscomputadoras.data.repository.DevolucionRepository
import com.ien.prestamoscomputadoras.data.repository.HistorialRepository
import com.ien.prestamoscomputadoras.data.repository.HomeRepository
import com.ien.prestamoscomputadoras.data.repository.PermisosRepository
import com.ien.prestamoscomputadoras.data.repository.PrestamoEncontrado
import com.ien.prestamoscomputadoras.data.repository.PrestamoRepository
import com.ien.prestamoscomputadoras.data.repository.PrestamoYaDevueltoException
import com.ien.prestamoscomputadoras.data.repository.RESTRICCION_NOMBRE_USUARIO
import com.ien.prestamoscomputadoras.data.repository.RESTRICCION_UN_ACTIVO_POR_COMPUTADORA
import com.ien.prestamoscomputadoras.data.repository.UsuarioRepository
import com.ien.prestamoscomputadoras.util.ZONA_ARGENTINA
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toInstant
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/*
 * Repositories falsos para los tests: guardan todo en listas en memoria e imitan las reglas
 * de la base real (el Admin, el préstamo activo único, los triggers). Así los ViewModels se
 * prueban sin internet ni Supabase.
 */

/**
 * Reemplaza Dispatchers.Main (que no existe en la JVM) por un dispatcher que corre las
 * corrutinas al instante: como los repositorios falsos no suspenden, cada
 * `viewModelScope.launch` termina antes de que vuelva la llamada al ViewModel.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(UnconfinedTestDispatcher())
    override fun finished(description: Description) = Dispatchers.resetMain()
}

/** Fecha y hora en Argentina (la zona que usa la app), como [Instant]. */
fun fechaAr(mes: Int, dia: Int, hora: Int, minuto: Int = 0, anio: Int = 2026): Instant =
    LocalDateTime(anio, mes, dia, hora, minuto).toInstant(ZONA_ARGENTINA)

/** Una fila de la vista historial_prestamos con valores por defecto razonables. */
fun filaHistorial(
    id: Long,
    fechaPrestamo: Instant,
    fechaDevolucion: Instant? = null,
    observacionesIniciales: String? = null,
    prestadoPor: String = "Ana López",
    devueltoPor: String? = if (fechaDevolucion != null) "Marta Ruiz" else null,
    codigo: String = "PC-$id",
    enciende: Boolean? = if (fechaDevolucion != null) true else null,
    pantallaOk: Boolean? = if (fechaDevolucion != null) true else null,
    cargador: Boolean? = if (fechaDevolucion != null) true else null,
    observacionesDevolucion: String? = null,
) = PrestamoHistorial(
    idPrestamo = id,
    estado = if (fechaDevolucion != null) Prestamo.ESTADO_DEVUELTO else Prestamo.ESTADO_ACTIVO,
    fechaPrestamo = fechaPrestamo,
    fechaDevolucion = fechaDevolucion,
    ultimaActividad = fechaDevolucion ?: fechaPrestamo,
    observacionesIniciales = observacionesIniciales,
    idAlumno = 1,
    alumnoNombre = "Juan",
    alumnoApellido = "Pérez",
    alumnoDni = "46111222",
    idComputadora = id,
    computadoraCodigo = codigo,
    idUsuarioPrestamo = 1,
    prestadoPor = prestadoPor,
    idUsuarioDevolucion = if (devueltoPor != null) 2 else null,
    devueltoPor = devueltoPor,
    enciende = enciende,
    pantallaOk = pantallaOk,
    cargador = cargador,
    observacionesDevolucion = observacionesDevolucion,
)

/** Imita Supabase Auth + el trigger handle_new_user (la primera cuenta es Admin). */
class FakeUsuarioRepository : UsuarioRepository {
    val perfiles = mutableListOf<Perfil>()
    val contrasenas = mutableMapOf<String, String>()
    /** Si no es `null`, todas las llamadas fallan con este error (p. ej. sin internet). */
    var falla: ErrorDatos? = null

    override suspend fun registrar(datos: DatosRegistro, contrasena: String): Result<Unit> {
        falla?.let { return Result.failure(it) }
        if (datos.email in contrasenas) return Result.failure(ErrorDatos.EmailEnUso())
        if (perfiles.any { it.nombreUsuario == datos.nombreUsuario }) {
            return Result.failure(ErrorDatos.Duplicado(RESTRICCION_NOMBRE_USUARIO))
        }
        val rol = if (perfiles.any { it.idRol == Rol.ID_ADMIN }) Rol.ID_PERSONAL else Rol.ID_ADMIN
        perfiles += Perfil(
            idPerfil = perfiles.size + 1L,
            authUserId = "uuid-${perfiles.size + 1}",
            nombre = datos.nombre,
            apellido = datos.apellido,
            nombreUsuario = datos.nombreUsuario,
            dni = datos.dni,
            email = datos.email,
            idRol = rol,
        )
        contrasenas[datos.email] = contrasena
        return Result.success(Unit)
    }

    override suspend fun iniciarSesion(email: String, contrasena: String): Result<Perfil> {
        falla?.let { return Result.failure(it) }
        val perfil = perfiles.firstOrNull { it.email == email }
        return if (perfil != null && contrasenas[email] == contrasena) {
            Result.success(perfil)
        } else {
            Result.failure(ErrorDatos.CredencialesInvalidas())
        }
    }

    override suspend fun restaurarSesion(): Result<Perfil?> = Result.success(null)

    var sesionesCerradas = 0
    override suspend fun cerrarSesion(): Result<Unit> {
        sesionesCerradas++
        return Result.success(Unit)
    }
}

class FakeAlumnoRepository(val alumnos: MutableList<Alumno> = mutableListOf()) : AlumnoRepository {
    var falla: ErrorDatos? = null

    override suspend fun insertar(alumno: Alumno): Result<Long> {
        falla?.let { return Result.failure(it) }
        val id = alumnos.size + 1L
        alumnos += alumno.copy(idAlumno = id)
        return Result.success(id)
    }

    override suspend fun buscarPorDni(dni: String): Result<Alumno?> {
        falla?.let { return Result.failure(it) }
        return Result.success(alumnos.firstOrNull { it.dni == dni })
    }
}

class FakeComputadoraRepository(computadorasIniciales: List<Computadora> = emptyList()) : ComputadoraRepository {
    val computadoras = computadorasIniciales.toMutableList()
    /** Si no es `null`, solo falla el insert (p. ej. SinPermiso de un no-Admin). */
    var fallaAlInsertar: ErrorDatos? = null

    override suspend fun insertar(computadora: Computadora): Result<Long> {
        fallaAlInsertar?.let { return Result.failure(it) }
        computadoras += computadora
        return Result.success(computadora.idComputadora)
    }

    override suspend fun buscarPorCodigo(codigo: String): Result<Computadora?> =
        Result.success(computadoras.firstOrNull { it.codigo == codigo })

    override suspend fun buscarPorId(idComputadora: Long): Result<Computadora?> =
        Result.success(computadoras.firstOrNull { it.idComputadora == idComputadora })
}

/**
 * Préstamos, alumnos y computadoras en memoria. Imita a la base: la fecha la pone "el
 * servidor" ([ahoraServidor]), no puede haber dos préstamos ACTIVOS de la misma computadora,
 * y al devolver, el "trigger" completa fecha_devolucion e id_usuario_devolucion.
 */
class FakePrestamos(
    computadoras: List<Computadora> = emptyList(),
    alumnos: List<Alumno> = emptyList(),
    var ahoraServidor: Instant = fechaAr(9, 30, 12),
    /** Perfil logueado (lo que usa el trigger como "quién recibió"). */
    var perfilLogueado: Long = 3,
) : PrestamoRepository, DevolucionRepository {
    val computadoras = computadoras.toMutableList()
    val alumnos = alumnos.toMutableList()
    val prestamos = mutableListOf<Prestamo>()
    val revisiones = mutableListOf<EstadoComputadora>()
    var falla: ErrorDatos? = null

    // --- PrestamoRepository
    override suspend fun insertar(prestamo: NuevoPrestamo): Result<Long> {
        falla?.let { return Result.failure(it) }
        if (prestamos.any { it.idComputadora == prestamo.idComputadora && it.estado == Prestamo.ESTADO_ACTIVO }) {
            return Result.failure(ErrorDatos.Duplicado(RESTRICCION_UN_ACTIVO_POR_COMPUTADORA))
        }
        val id = prestamos.size + 1L
        prestamos += Prestamo(
            idPrestamo = id,
            idAlumno = prestamo.idAlumno,
            idComputadora = prestamo.idComputadora,
            idUsuarioPrestamo = prestamo.idUsuarioPrestamo,
            fechaPrestamo = ahoraServidor,
            estado = prestamo.estado,
            observacionesIniciales = prestamo.observacionesIniciales,
        )
        return Result.success(id)
    }

    override suspend fun listarAlumnos(): Result<List<Alumno>> {
        falla?.let { return Result.failure(it) }
        return Result.success(alumnos.toList())
    }

    override suspend fun listarComputadorasDisponibles(): Result<List<Computadora>> {
        falla?.let { return Result.failure(it) }
        val prestadas = prestamos.filter { it.estado == Prestamo.ESTADO_ACTIVO }.map { it.idComputadora }
        return Result.success(computadoras.filter { it.idComputadora !in prestadas })
    }

    // --- DevolucionRepository
    /** Igual que la consulta real: activo primero; si no, la devolución más reciente. */
    override suspend fun buscarParaDevolucion(codigoComputadora: String): Result<BusquedaDevolucion> {
        falla?.let { return Result.failure(it) }
        val computadora = computadoras.firstOrNull { it.codigo == codigoComputadora }
            ?: return Result.success(BusquedaDevolucion.SinPrestamos(codigoComputadora))
        val deEsaPc = prestamos.filter { it.idComputadora == computadora.idComputadora }
        val prestamo = deEsaPc.firstOrNull { it.estado == Prestamo.ESTADO_ACTIVO }
            ?: return Result.success(
                if (deEsaPc.isEmpty()) {
                    BusquedaDevolucion.SinPrestamos(codigoComputadora)
                } else {
                    BusquedaDevolucion.YaDevuelta(codigoComputadora, deEsaPc.mapNotNull { it.fechaDevolucion }.maxOrNull())
                },
            )
        val alumno = alumnos.first { it.idAlumno == prestamo.idAlumno }
        return Result.success(
            BusquedaDevolucion.ConPrestamoActivo(PrestamoEncontrado(
                idPrestamo = prestamo.idPrestamo,
                nombreAlumno = "${alumno.nombre} ${alumno.apellido}",
                dniAlumno = alumno.dni,
                codigoComputadora = codigoComputadora,
                fechaPrestamo = prestamo.fechaPrestamo,
                observacionesIniciales = prestamo.observacionesIniciales?.trim()?.ifEmpty { null },
            )),
        )
    }

    override suspend fun registrarDevolucion(
        idPrestamo: Long,
        enciende: Boolean,
        pantallaOk: Boolean,
        cargador: Boolean,
        observaciones: String?,
    ): Result<Unit> {
        falla?.let { return Result.failure(it) }
        val i = prestamos.indexOfFirst { it.idPrestamo == idPrestamo && it.estado == Prestamo.ESTADO_ACTIVO }
        if (i == -1) return Result.failure(PrestamoYaDevueltoException())
        devolverDirecto(idPrestamo)
        revisiones += EstadoComputadora(
            idEstado = revisiones.size + 1L,
            idPrestamo = idPrestamo,
            enciende = enciende,
            pantallaOk = pantallaOk,
            cargador = cargador,
            observaciones = observaciones,
            fechaRevision = ahoraServidor,
        )
        return Result.success(Unit)
    }

    /** Lo que hace el trigger completar_devolucion. También sirve para simular otro celular. */
    fun devolverDirecto(idPrestamo: Long, quien: Long = perfilLogueado) {
        val i = prestamos.indexOfFirst { it.idPrestamo == idPrestamo }
        prestamos[i] = prestamos[i].copy(
            estado = Prestamo.ESTADO_DEVUELTO,
            fechaDevolucion = ahoraServidor,
            idUsuarioDevolucion = quien,
        )
    }
}

/** Devuelve las filas dentro del rango pedido, ordenadas como la consulta real. */
class FakeHistorialRepository(private val filas: List<PrestamoHistorial>) : HistorialRepository {
    var desde: Instant? = null
    var hasta: Instant? = null
    var falla: ErrorDatos? = null

    override suspend fun listarEntre(desde: Instant, hasta: Instant): Result<List<PrestamoHistorial>> {
        this.desde = desde
        this.hasta = hasta
        falla?.let { return Result.failure(it) }
        return Result.success(
            filas
                .filter { it.fechaPrestamo >= desde && it.fechaPrestamo < hasta }
                .sortedWith(compareByDescending<PrestamoHistorial> { it.ultimaActividad }.thenByDescending { it.idPrestamo }),
        )
    }
}

/**
 * Home en memoria. [filas] son los préstamos; [hoy] marca qué devoluciones son de "hoy".
 * Para simular Realtime: cambiar [filas] y llamar a [avisarCambio].
 */
class FakeHomeRepository(var hoy: Pair<Instant, Instant> = fechaAr(9, 30, 0) to fechaAr(10, 1, 0)) : HomeRepository {
    var filas: List<PrestamoHistorial> = emptyList()
    var falla: ErrorDatos? = null
    private val cambios = MutableSharedFlow<Unit>()
    /** Cuántos están escuchando Realtime en este momento. */
    val suscriptos = MutableStateFlow(0)

    override suspend fun resumenHoy(): Result<ResumenHoy> {
        falla?.let { return Result.failure(it) }
        return Result.success(
            ResumenHoy(
                prestados = filas.count { it.estado == Prestamo.ESTADO_ACTIVO }.toLong(),
                devueltosHoy = filas.count {
                    it.estado == Prestamo.ESTADO_DEVUELTO && it.fechaDevolucion!! >= hoy.first && it.fechaDevolucion!! < hoy.second
                }.toLong(),
            ),
        )
    }

    override suspend fun prestamosRecientes(limite: Int): Result<List<PrestamoHistorial>> {
        falla?.let { return Result.failure(it) }
        return Result.success(
            filas.sortedWith(compareByDescending<PrestamoHistorial> { it.ultimaActividad }.thenByDescending { it.idPrestamo })
                .take(limite),
        )
    }

    override fun cambiosEnPrestamos(): Flow<Unit> = kotlinx.coroutines.flow.flow {
        suscriptos.value++
        try {
            cambios.collect { emit(it) }
        } finally {
            suscriptos.value--
        }
    }

    suspend fun avisarCambio() = cambios.emit(Unit)
}

/**
 * Rol de cada perfil y permisos por rol, con la misma lógica que el repositorio real.
 * - [fallaLectura]: si no es `null`, las LECTURAS fallan con ese error (sin red, sesión...).
 * - [escrituras]: cuántas veces se escribió en rol_permiso (Home nunca debería hacerlo).
 */
class FakePermisosRepository(
    rolesPorUsuario: Map<Long, Long> = emptyMap(),
    permisosIniciales: Set<RolPermiso> = emptySet(),
) : PermisosRepository {
    val roles = MutableStateFlow(rolesPorUsuario)
    val permisos = MutableStateFlow(permisosIniciales)
    /** Falla de las escrituras (cambiarPermisoPersonal). */
    var falla: ErrorDatos? = null
    var fallaLectura: ErrorDatos? = null
    var escrituras = 0
    var lecturas = 0

    override suspend fun accesoDe(idUsuario: Long?): Result<AccesoUsuario> {
        if (idUsuario == null) return Result.success(AccesoUsuario.SIN_ACCESO)
        lecturas++
        fallaLectura?.let { return Result.failure(it) }
        return Result.success(
            when (val idRol = roles.value[idUsuario]) {
                null -> AccesoUsuario.SIN_ACCESO
                Rol.ID_ADMIN -> AccesoUsuario(esAdmin = true, permisos = Permiso.entries.toSet())
                else -> AccesoUsuario(esAdmin = false, permisos = permisosDe(permisos.value, idRol))
            },
        )
    }

    override suspend fun permisosPersonal(): Result<Set<Permiso>> {
        lecturas++
        fallaLectura?.let { return Result.failure(it) }
        return Result.success(permisosDe(permisos.value, Rol.ID_PERSONAL))
    }

    override suspend fun cambiarPermisoPersonal(permiso: Permiso, habilitado: Boolean): Result<Unit> {
        require(!permiso.soloAdmin) { "${permiso.name} es exclusivo del Admin" }
        escrituras++
        falla?.let { return Result.failure(it) }
        val fila = RolPermiso(Rol.ID_PERSONAL, permiso.name)
        permisos.value = if (habilitado) permisos.value + fila else permisos.value - fila
        return Result.success(Unit)
    }

    private fun permisosDe(filas: Set<RolPermiso>, idRol: Long) =
        filas.filter { it.idRol == idRol }
            .mapNotNull { Permiso.desdeNombre(it.permiso) }
            .filterNot { it.soloAdmin }
            .toSet()
}
