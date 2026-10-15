package com.ien.prestamoscomputadoras.data.model

import kotlin.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/*
 * Modelos de la base de Supabase (reemplazan a las @Entity de Room).
 *
 * - @Serializable: kotlinx-serialization sabe convertirlos a/desde el JSON que manda Supabase.
 * - @SerialName("id_alumno"): el nombre EXACTO de la columna (snake_case) en
 *   supabase/migrations/20261012000000_schema_inicial.sql. En Kotlin se sigue usando camelCase.
 * - Fechas: las columnas son timestamptz y llegan como [Instant] (un instante, sin zona).
 *   Para mostrarlas se pasan a la hora de Argentina con ZONA_ARGENTINA (ver Fechas.kt).
 *
 * Los ids que genera la base (identity) NO se mandan al insertar: para eso hay clases
 * "Nuevo..." aparte, solo con las columnas que completa la app.
 */

/** Tabla `rol`. Hay dos filas fijas: [ID_ADMIN] y [ID_PERSONAL]. */
@Serializable
data class Rol(
    @SerialName("id_rol") val idRol: Long,
    val nombre: String,
    @SerialName("es_sistema") val esSistema: Boolean,
) {
    companion object {
        const val ID_ADMIN = 1L
        const val ID_PERSONAL = 2L
    }
}

/** Tabla `rol_permiso`: una fila por permiso habilitado. [permiso] es el `name` de Permiso. */
@Serializable
data class RolPermiso(
    @SerialName("id_rol") val idRol: Long,
    val permiso: String,
)

/**
 * Tabla `perfil` (antes `usuario`). La contraseña ya no está acá: la guarda Supabase Auth.
 * La fila la crea un trigger de la base al registrarse (ver handle_new_user en el esquema).
 */
@Serializable
data class Perfil(
    @SerialName("id_perfil") val idPerfil: Long,
    /** Id del usuario en Supabase Auth (uuid). */
    @SerialName("auth_user_id") val authUserId: String,
    val nombre: String,
    val apellido: String,
    @SerialName("nombre_usuario") val nombreUsuario: String,
    val dni: String,
    val email: String,
    @SerialName("id_rol") val idRol: Long,
)

/** Tabla `alumno`. [idAlumno] = 0 mientras no se guardó (lo genera la base). */
@Serializable
data class Alumno(
    @SerialName("id_alumno") val idAlumno: Long = 0,
    val nombre: String,
    val apellido: String,
    val dni: String,
)

/** Lo que se manda al insertar un alumno (sin id: lo genera la base). */
@Serializable
data class NuevoAlumno(
    val nombre: String,
    val apellido: String,
    val dni: String,
)

/**
 * Tabla `computadora`. El id NO lo genera la app al azar: es el número del código (PC-11 -> 11),
 * y se manda explícito al insertar.
 */
@Serializable
data class Computadora(
    @SerialName("id_computadora") val idComputadora: Long,
    val codigo: String,
    /** Opcional, ej: "ProBook 440 G8". */
    val modelo: String? = null,
)

/** Tabla `prestamo`. */
@Serializable
data class Prestamo(
    @SerialName("id_prestamo") val idPrestamo: Long,
    @SerialName("id_alumno") val idAlumno: Long,
    @SerialName("id_computadora") val idComputadora: Long,
    /** Perfil que entregó la computadora. */
    @SerialName("id_usuario_prestamo") val idUsuarioPrestamo: Long,
    /** Perfil que recibió la devolución. `null` mientras está activo. Lo completa el trigger. */
    @SerialName("id_usuario_devolucion") val idUsuarioDevolucion: Long? = null,
    /** La pone el servidor (default now()). */
    @SerialName("fecha_prestamo") val fechaPrestamo: Instant,
    /** `null` mientras está activo. La completa el trigger al pasar a DEVUELTO. */
    @SerialName("fecha_devolucion") val fechaDevolucion: Instant? = null,
    /** [ESTADO_ACTIVO] o [ESTADO_DEVUELTO]. */
    val estado: String,
    @SerialName("observaciones_iniciales") val observacionesIniciales: String? = null,
) {
    companion object {
        const val ESTADO_ACTIVO = "ACTIVO"
        const val ESTADO_DEVUELTO = "DEVUELTO"
    }
}

/**
 * Lo que se manda al registrar un préstamo. NO lleva fecha_prestamo (la pone el servidor,
 * así no depende del reloj del celular) ni datos de devolución.
 */
@Serializable
data class NuevoPrestamo(
    @SerialName("id_alumno") val idAlumno: Long,
    @SerialName("id_computadora") val idComputadora: Long,
    /** id_perfil del usuario logueado (la base verifica que sea él mismo). */
    @SerialName("id_usuario_prestamo") val idUsuarioPrestamo: Long,
    @SerialName("observaciones_iniciales") val observacionesIniciales: String? = null,
    val estado: String = Prestamo.ESTADO_ACTIVO,
)

/** Tabla `estado_computadora`: la revisión del equipo al devolverlo (una por préstamo). */
@Serializable
data class EstadoComputadora(
    @SerialName("id_estado") val idEstado: Long,
    @SerialName("id_prestamo") val idPrestamo: Long,
    val enciende: Boolean,
    @SerialName("pantalla_ok") val pantallaOk: Boolean,
    val cargador: Boolean,
    val observaciones: String? = null,
    @SerialName("fecha_revision") val fechaRevision: Instant,
)

/** Lo que se manda al guardar la revisión (fecha_revision la pone el servidor). */
@Serializable
data class NuevoEstadoComputadora(
    @SerialName("id_prestamo") val idPrestamo: Long,
    val enciende: Boolean,
    @SerialName("pantalla_ok") val pantallaOk: Boolean,
    val cargador: Boolean,
    val observaciones: String? = null,
)

/**
 * Una fila de la vista `historial_prestamos`: un préstamo con los nombres ya resueltos por la
 * base (alumno, computadora, quién lo prestó y quién lo recibió) y su revisión, si tiene.
 * La usan el Historial y "Actividad reciente" de Home.
 */
@Serializable
data class PrestamoHistorial(
    @SerialName("id_prestamo") val idPrestamo: Long,
    val estado: String,
    @SerialName("fecha_prestamo") val fechaPrestamo: Instant,
    @SerialName("fecha_devolucion") val fechaDevolucion: Instant? = null,
    /** La devolución si ya se devolvió; si no, la entrega. */
    @SerialName("ultima_actividad") val ultimaActividad: Instant,
    @SerialName("observaciones_iniciales") val observacionesIniciales: String? = null,
    @SerialName("id_alumno") val idAlumno: Long,
    @SerialName("alumno_nombre") val alumnoNombre: String,
    @SerialName("alumno_apellido") val alumnoApellido: String,
    @SerialName("alumno_dni") val alumnoDni: String,
    @SerialName("id_computadora") val idComputadora: Long,
    @SerialName("computadora_codigo") val computadoraCodigo: String,
    @SerialName("computadora_modelo") val computadoraModelo: String? = null,
    @SerialName("id_usuario_prestamo") val idUsuarioPrestamo: Long,
    /** "Nombre Apellido" de quien entregó la computadora. */
    @SerialName("prestado_por") val prestadoPor: String,
    @SerialName("id_usuario_devolucion") val idUsuarioDevolucion: Long? = null,
    /** "Nombre Apellido" de quien recibió la devolución. `null` mientras está activo. */
    @SerialName("devuelto_por") val devueltoPor: String? = null,
    // Revisión de la devolución: todo `null` mientras el préstamo sigue activo.
    val enciende: Boolean? = null,
    @SerialName("pantalla_ok") val pantallaOk: Boolean? = null,
    val cargador: Boolean? = null,
    @SerialName("observaciones_devolucion") val observacionesDevolucion: String? = null,
)

/** Resultado de la función `resumen_hoy()` (tarjetas de Home). */
@Serializable
data class ResumenHoy(
    /** Préstamos ACTIVO ahora, de cualquier fecha. */
    val prestados: Long,
    /** Devueltos hoy (el "hoy" lo calcula la base en hora de Argentina). */
    @SerialName("devueltos_hoy") val devueltosHoy: Long,
)
