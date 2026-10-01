package com.ien.prestamoscomputadoras.data.repository

import com.ien.prestamoscomputadoras.data.dao.AlumnoDao
import com.ien.prestamoscomputadoras.data.dao.EstadoComputadoraDao
import com.ien.prestamoscomputadoras.data.dao.PrestamoDao
import com.ien.prestamoscomputadoras.data.entity.EstadoComputadora

/** Préstamo activo encontrado al escanear un QR, con los datos ya resueltos para mostrar. */
data class PrestamoEncontrado(
    val idPrestamo: Long,
    val nombreAlumno: String,
    val dniAlumno: String,
    val codigoComputadora: String,
    /** Epoch en milisegundos. */
    val fechaPrestamo: Long,
)

/** El préstamo ya no estaba ACTIVO al confirmar (p. ej. se devolvió en otro momento). */
class PrestamoYaDevueltoException : IllegalStateException("El préstamo ya fue devuelto")

/** Todo lo que necesita la pantalla "Registrar Devolución". */
class DevolucionRepository(
    private val prestamoDao: PrestamoDao,
    private val alumnoDao: AlumnoDao,
    private val estadoComputadoraDao: EstadoComputadoraDao,
    /**
     * Corre el bloque en una transacción de Room (`db.withTransaction`): la revisión y el
     * cambio de estado del préstamo se guardan juntos o no se guarda ninguno.
     */
    private val enTransaccion: suspend (suspend () -> Unit) -> Unit,
) {

    /** `null` si ese código no tiene un préstamo ACTIVO. */
    suspend fun buscarPrestamoActivo(codigoComputadora: String): PrestamoEncontrado? {
        val prestamo = prestamoDao.buscarPrestamoActivoPorCodigoComputadora(codigoComputadora)
            ?: return null
        // El FK garantiza que el alumno existe; el null es solo por el tipo de retorno.
        val alumno = alumnoDao.buscarPorId(prestamo.idAlumno) ?: return null
        return PrestamoEncontrado(
            idPrestamo = prestamo.idPrestamo,
            nombreAlumno = "${alumno.nombre} ${alumno.apellido}",
            dniAlumno = alumno.dni,
            codigoComputadora = codigoComputadora,
            fechaPrestamo = prestamo.fechaPrestamo,
        )
    }

    /**
     * Cierra el préstamo: lo marca DEVUELTO con [fecha] y guarda la revisión del equipo.
     * Lanza [PrestamoYaDevueltoException] (sin guardar nada) si el préstamo ya no estaba ACTIVO.
     */
    suspend fun registrarDevolucion(
        idPrestamo: Long,
        enciende: Boolean,
        pantallaOk: Boolean,
        cargador: Boolean,
        observaciones: String?,
        fecha: Long,
    ) {
        enTransaccion {
            if (prestamoDao.actualizarDevolucion(idPrestamo, fecha) == 0) {
                throw PrestamoYaDevueltoException()
            }
            estadoComputadoraDao.insertar(
                EstadoComputadora(
                    idPrestamo = idPrestamo,
                    enciende = enciende,
                    pantallaOk = pantallaOk,
                    cargador = cargador,
                    observaciones = observaciones,
                    fechaRevision = fecha,
                ),
            )
        }
    }
}
