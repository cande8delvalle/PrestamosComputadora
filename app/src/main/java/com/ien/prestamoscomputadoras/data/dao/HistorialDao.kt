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
    @ColumnInfo(name = "admin_nombre") val adminNombre: String,
    @ColumnInfo(name = "admin_apellido") val adminApellido: String,
)

@Dao
interface HistorialDao {

    /**
     * Préstamos con fecha de préstamo en [desde, hasta), del más reciente al más antiguo.
     * El administrador sale de `prestamo.id_administrador` (quien lo gestionó en su momento).
     */
    @Query(
        "SELECT p.*, " +
            "a.nombre AS alumno_nombre, a.apellido AS alumno_apellido, a.dni AS alumno_dni, " +
            "c.codigo AS codigo_computadora, " +
            "ad.nombre AS admin_nombre, ad.apellido AS admin_apellido, " +
            "e.id_estado AS revision_id_estado, e.id_prestamo AS revision_id_prestamo, " +
            "e.enciende AS revision_enciende, e.pantalla_ok AS revision_pantalla_ok, " +
            "e.cargador AS revision_cargador, e.observaciones AS revision_observaciones, " +
            "e.fecha_revision AS revision_fecha_revision " +
            "FROM prestamo p " +
            "INNER JOIN alumno a ON a.id_alumno = p.id_alumno " +
            "INNER JOIN computadora c ON c.id_computadora = p.id_computadora " +
            "INNER JOIN administrador ad ON ad.id_administrador = p.id_administrador " +
            "LEFT JOIN estado_computadora e ON e.id_prestamo = p.id_prestamo " +
            "WHERE p.fecha_prestamo >= :desde AND p.fecha_prestamo < :hasta " +
            "ORDER BY p.fecha_prestamo DESC, p.id_prestamo DESC",
    )
    suspend fun listarEntre(desde: Long, hasta: Long): List<PrestamoHistorial>
}
