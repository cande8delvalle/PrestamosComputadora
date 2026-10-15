package com.ien.prestamoscomputadoras.data.repository

import com.ien.prestamoscomputadoras.data.model.NuevoEstadoComputadora
import com.ien.prestamoscomputadoras.data.model.Prestamo
import com.ien.prestamoscomputadoras.data.model.PrestamoHistorial
import com.ien.prestamoscomputadoras.data.remote.ErrorDeNegocio
import com.ien.prestamoscomputadoras.data.remote.SupabaseProvider
import com.ien.prestamoscomputadoras.data.remote.GuardiaSesion
import com.ien.prestamoscomputadoras.data.remote.SupabaseGuardiaSesion
import com.ien.prestamoscomputadoras.data.remote.llamarConSesion
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlin.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Préstamo activo encontrado al escanear un QR, con los datos ya resueltos para mostrar. */
data class PrestamoEncontrado(
    val idPrestamo: Long,
    val nombreAlumno: String,
    val dniAlumno: String,
    val codigoComputadora: String,
    val fechaPrestamo: Instant,
    /**
     * Observaciones que se cargaron al ENTREGAR la computadora (observaciones_iniciales).
     * `null` si no hay o están vacías. Se muestran al recibirla para compararlas.
     */
    val observacionesIniciales: String? = null,
)

/** Qué se encontró al identificar una computadora para devolverla. */
sealed interface BusquedaDevolucion {

    /** Tiene un préstamo ACTIVO: se puede registrar la devolución. */
    data class ConPrestamoActivo(val prestamo: PrestamoEncontrado) : BusquedaDevolucion

    /** a) Nunca tuvo préstamos (o ese código no está registrado). */
    data class SinPrestamos(val codigoComputadora: String) : BusquedaDevolucion

    /** b) Tuvo préstamos, pero ninguno activo: ya fue devuelta. */
    data class YaDevuelta(val codigoComputadora: String, val ultimaDevolucion: Instant?) : BusquedaDevolucion
}

/** El préstamo ya no estaba ACTIVO al confirmar (p. ej. lo devolvió otro usuario recién). */
class PrestamoYaDevueltoException : ErrorDeNegocio("El préstamo ya fue devuelto")

/**
 * La devolución quedó registrada, pero no se pudo guardar la revisión del equipo
 * (son dos llamadas a la base y falló la segunda, p. ej. se cortó internet en el medio).
 */
class RevisionNoGuardadaException(causa: Throwable) :
    ErrorDeNegocio("La devolución se registró, pero no se pudo guardar la revisión del equipo.", causa)

/** Todo lo que necesita la pantalla "Registrar Devolución". */
interface DevolucionRepository {

    /**
     * Busca la computadora [codigoComputadora] para devolverla: su préstamo ACTIVO o, si no
     * tiene, si nunca tuvo préstamos o ya fue devuelta (con la fecha de la última devolución).
     */
    suspend fun buscarParaDevolucion(codigoComputadora: String): Result<BusquedaDevolucion>

    /**
     * Cierra el préstamo (lo pasa a DEVUELTO) y guarda la revisión del equipo. Quién lo
     * recibió y la fecha los completa un trigger de la base con el usuario logueado.
     * Falla con [PrestamoYaDevueltoException] si el préstamo ya no estaba ACTIVO y con
     * [RevisionNoGuardadaException] si se cerró pero no se pudo guardar la revisión.
     */
    suspend fun registrarDevolucion(
        idPrestamo: Long,
        enciende: Boolean,
        pantallaOk: Boolean,
        cargador: Boolean,
        observaciones: String?,
    ): Result<Unit>
}

/** Lo único que se manda al devolver: el trigger completa fecha_devolucion e id_usuario_devolucion. */
@Serializable
private data class CambioEstado(@SerialName("estado") val estado: String)

class SupabaseDevolucionRepository(
    private val supabase: SupabaseClient = SupabaseProvider.client,
    /** Espera la sesión antes de consultar y refresca el token si venció (ver SesionSupabase.kt). */
    private val guardia: GuardiaSesion = SupabaseGuardiaSesion(supabase),
) : DevolucionRepository {

    override suspend fun buscarParaDevolucion(codigoComputadora: String): Result<BusquedaDevolucion> =
        llamarConSesion(guardia) {
            // Una sola consulta a la vista (ya trae alumno y código resueltos). Orden:
            // "ACTIVO" < "DEVUELTO", así el activo (si hay) sale primero; si no, la devolución
            // más reciente.
            val fila = supabase.from("historial_prestamos")
                .select {
                    filter { eq("computadora_codigo", codigoComputadora) }
                    order("estado", Order.ASCENDING)
                    order("fecha_devolucion", Order.DESCENDING)
                    limit(1)
                }
                .decodeSingleOrNull<PrestamoHistorial>()
            when {
                fila == null -> BusquedaDevolucion.SinPrestamos(codigoComputadora)
                fila.estado != Prestamo.ESTADO_ACTIVO ->
                    BusquedaDevolucion.YaDevuelta(fila.computadoraCodigo, fila.fechaDevolucion)
                else -> BusquedaDevolucion.ConPrestamoActivo(
                    PrestamoEncontrado(
                        idPrestamo = fila.idPrestamo,
                        nombreAlumno = "${fila.alumnoNombre} ${fila.alumnoApellido}",
                        dniAlumno = fila.alumnoDni,
                        codigoComputadora = fila.computadoraCodigo,
                        fechaPrestamo = fila.fechaPrestamo,
                        observacionesIniciales = fila.observacionesIniciales?.trim()?.ifEmpty { null },
                    ),
                )
            }
        }

    override suspend fun registrarDevolucion(
        idPrestamo: Long,
        enciende: Boolean,
        pantallaOk: Boolean,
        cargador: Boolean,
        observaciones: String?,
    ): Result<Unit> = llamarConSesion(guardia) {
        // 1) ACTIVO -> DEVUELTO, solo si seguía ACTIVO. select() devuelve las filas cambiadas:
        //    si no cambió ninguna, ya estaba devuelto.
        val actualizados = supabase.from("prestamo")
            .update(CambioEstado(Prestamo.ESTADO_DEVUELTO)) {
                select()
                filter {
                    eq("id_prestamo", idPrestamo)
                    eq("estado", Prestamo.ESTADO_ACTIVO)
                }
            }
            .decodeList<Prestamo>()
        if (actualizados.isEmpty()) throw PrestamoYaDevueltoException()

        // 2) Revisión del equipo.
        try {
            supabase.from("estado_computadora").insert(
                NuevoEstadoComputadora(
                    idPrestamo = idPrestamo,
                    enciende = enciende,
                    pantallaOk = pantallaOk,
                    cargador = cargador,
                    observaciones = observaciones,
                ),
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw RevisionNoGuardadaException(e)
        }
    }
}
