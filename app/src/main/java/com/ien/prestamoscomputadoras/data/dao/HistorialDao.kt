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
    /** Administrador que entregó la computadora. */
    @ColumnInfo(name = "admin_prestamo_nombre") val adminPrestamoNombre: String,
    @ColumnInfo(name = "admin_prestamo_apellido") val adminPrestamoApellido: String,
    /**
     * Administrador que recibió la devolución. `null` si el préstamo sigue activo, o si se
     * devolvió antes de que se guardara este dato (versión 5 de la base o anterior).
     */
    @ColumnInfo(name = "admin_devolucion_nombre") val adminDevolucionNombre: String?,
    @ColumnInfo(name = "admin_devolucion_apellido") val adminDevolucionApellido: String?,
)

@Dao
interface HistorialDao {

    /**
     * Préstamos con fecha de préstamo en [desde, hasta), del más reciente al más antiguo.
     * La tabla administrador se une dos veces con alias distintos: `ap` es quien entregó y `ad`
     * quien recibió la devolución (LEFT JOIN: no hay mientras el préstamo está activo).
     */
    @Query(
        "SELECT p.*, " +
            "a.nombre AS alumno_nombre, a.apellido AS alumno_apellido, a.dni AS alumno_dni, " +
            "c.codigo AS codigo_computadora, " +
            "ap.nombre AS admin_prestamo_nombre, ap.apellido AS admin_prestamo_apellido, " +
            "ad.nombre AS admin_devolucion_nombre, ad.apellido AS admin_devolucion_apellido, " +
            "e.id_estado AS revision_id_estado, e.id_prestamo AS revision_id_prestamo, " +
            "e.enciende AS revision_enciende, e.pantalla_ok AS revision_pantalla_ok, " +
            "e.cargador AS revision_cargador, e.observaciones AS revision_observaciones, " +
            "e.fecha_revision AS revision_fecha_revision " +
            "FROM prestamo p " +
            "INNER JOIN alumno a ON a.id_alumno = p.id_alumno " +
            "INNER JOIN computadora c ON c.id_computadora = p.id_computadora " +
            "INNER JOIN administrador ap ON ap.id_administrador = p.id_administrador_prestamo " +
            "LEFT JOIN administrador ad ON ad.id_administrador = p.id_administrador_devolucion " +
            "LEFT JOIN estado_computadora e ON e.id_prestamo = p.id_prestamo " +
            "WHERE p.fecha_prestamo >= :desde AND p.fecha_prestamo < :hasta " +
            "ORDER BY p.fecha_prestamo DESC, p.id_prestamo DESC",
    )
    suspend fun listarEntre(desde: Long, hasta: Long): List<PrestamoHistorial>
}
