package com.ien.prestamoscomputadoras.data.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Query
import com.ien.prestamoscomputadoras.data.entity.EstadoComputadora
import com.ien.prestamoscomputadoras.data.entity.Prestamo

/**
 * Un préstamo con todo lo necesario para su tarjeta del historial, resuelto en una sola query.
 * [revision] es `null` mientras el préstamo sigue activo (todavía no hay revisión de devolución).
 */
data class PrestamoHistorial(
    @Embedded val prestamo: Prestamo,
    @Embedded(prefix = "revision_") val revision: EstadoComputadora?,
    @ColumnInfo(name = "alumno_nombre") val alumnoNombre: String,
    @ColumnInfo(name = "alumno_apellido") val alumnoApellido: String,
    @ColumnInfo(name = "alumno_dni") val alumnoDni: String,
    @ColumnInfo(name = "codigo_computadora") val codigoComputadora: String,
    /** Usuario que entregó la computadora. */
    @ColumnInfo(name = "usuario_prestamo_nombre") val usuarioPrestamoNombre: String,
    @ColumnInfo(name = "usuario_prestamo_apellido") val usuarioPrestamoApellido: String,
    /**
     * Usuario que recibió la devolución. `null` si el préstamo sigue activo, o si se
     * devolvió antes de que se guardara este dato (versión 5 de la base o anterior).
     */
    @ColumnInfo(name = "usuario_devolucion_nombre") val usuarioDevolucionNombre: String?,
    @ColumnInfo(name = "usuario_devolucion_apellido") val usuarioDevolucionApellido: String?,
)

/**
 * SELECT + JOINs que arman un [PrestamoHistorial]. Lo comparten el Historial y "Actividad
 * reciente" de Home, así las dos pantallas muestran el mismo detalle; cada query agrega su
 * WHERE / ORDER BY. La tabla usuario se une dos veces con alias distintos: `ap` es quien entregó
 * y `ad` quien recibió la devolución (LEFT JOIN: no hay mientras el préstamo está activo).
 * La revisión es una por préstamo (índice único), así que los JOIN no repiten filas.
 */
internal const val SELECT_PRESTAMO_HISTORIAL =
    "SELECT p.*, " +
        "a.nombre AS alumno_nombre, a.apellido AS alumno_apellido, a.dni AS alumno_dni, " +
        "c.codigo AS codigo_computadora, " +
        "ap.nombre AS usuario_prestamo_nombre, ap.apellido AS usuario_prestamo_apellido, " +
        "ad.nombre AS usuario_devolucion_nombre, ad.apellido AS usuario_devolucion_apellido, " +
        "e.id_estado AS revision_id_estado, e.id_prestamo AS revision_id_prestamo, " +
        "e.enciende AS revision_enciende, e.pantalla_ok AS revision_pantalla_ok, " +
        "e.cargador AS revision_cargador, e.observaciones AS revision_observaciones, " +
        "e.fecha_revision AS revision_fecha_revision " +
        "FROM prestamo p " +
        "INNER JOIN alumno a ON a.id_alumno = p.id_alumno " +
        "INNER JOIN computadora c ON c.id_computadora = p.id_computadora " +
        "INNER JOIN usuario ap ON ap.id_usuario = p.id_usuario_prestamo " +
        "LEFT JOIN usuario ad ON ad.id_usuario = p.id_usuario_devolucion " +
        "LEFT JOIN estado_computadora e ON e.id_prestamo = p.id_prestamo "

@Dao
interface HistorialDao {

    /** Préstamos con fecha de préstamo en [desde, hasta), del más reciente al más antiguo. */
    @Query(
        SELECT_PRESTAMO_HISTORIAL +
            "WHERE p.fecha_prestamo >= :desde AND p.fecha_prestamo < :hasta " +
            "ORDER BY p.fecha_prestamo DESC, p.id_prestamo DESC",
    )
    suspend fun listarEntre(desde: Long, hasta: Long): List<PrestamoHistorial>
}
