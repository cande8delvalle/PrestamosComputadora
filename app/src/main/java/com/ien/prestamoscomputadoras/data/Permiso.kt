package com.ien.prestamoscomputadoras.data

/**
 * Catálogo fijo de permisos de la app. Vive en código (no en una tabla) porque cada permiso
 * corresponde a una pantalla que existe; en la base solo se guarda qué permisos tiene
 * habilitados cada rol (tabla `rol_permiso`, por [name]).
 *
 * El orden de declaración es el orden en que se listan en "Roles y Permisos".
 */
enum class Permiso(
    val titulo: String,
    val descripcion: String,
    /**
     * `true` si es exclusivo del Admin: siempre lo tiene, nunca se puede otorgar a otro rol y
     * ni siquiera se ofrece como opción al editar Personal Administrativo.
     */
    val soloAdmin: Boolean = false,
) {
    REGISTRAR_PRESTAMO(
        titulo = "Registrar Préstamos",
        descripcion = "Cargar un nuevo préstamo de computadora a un alumno.",
    ),
    REGISTRAR_DEVOLUCION(
        titulo = "Registrar Devolución",
        descripcion = "Cerrar un préstamo activo y marcar la computadora como devuelta.",
    ),
    VER_HISTORIAL(
        titulo = "Ver Historial de Préstamos",
        descripcion = "Consultar préstamos activos y devueltos, con quién los gestionó.",
    ),
    REGISTRAR_COMPUTADORA(
        titulo = "Registrar Computadora",
        descripcion = "Dar de alta un equipo nuevo en el sistema.",
    ),
    REGISTRAR_ALUMNO(
        titulo = "Registrar Alumnos",
        descripcion = "Dar de alta un alumno nuevo en el sistema.",
    ),
    GESTION_PERMISOS(
        titulo = "Gestión de Permisos",
        descripcion = "Otorgar permisos a los administrativos.",
        soloAdmin = true,
    ),
    ;

    companion object {
        /** Los que se pueden tildar/destildar para Personal Administrativo. */
        val editables: List<Permiso> = entries.filter { !it.soloAdmin }

        /** Con los que arranca Personal Administrativo (instalación nueva o migración). */
        val inicialesPersonal: Set<Permiso> = setOf(REGISTRAR_PRESTAMO, REGISTRAR_DEVOLUCION, VER_HISTORIAL)

        /** Ignora nombres desconocidos (p. ej. un permiso que se quitó del catálogo). */
        fun desdeNombre(nombre: String): Permiso? = entries.firstOrNull { it.name == nombre }
    }
}
