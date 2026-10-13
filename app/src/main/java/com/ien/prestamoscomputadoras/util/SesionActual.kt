package com.ien.prestamoscomputadoras.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

/**
 * Perfil con la sesión iniciada (id_perfil). Se setea en el login (o al restaurar la sesión en
 * el Splash) y se limpia en el logout.
 *
 * La sesión en sí (el token) la guarda Supabase Auth en el teléfono: si Android mata el
 * proceso, al volver a abrir la app el Splash la restaura y vuelve a llenar [usuarioId].
 */
object SesionActual {
    @Volatile
    var usuarioId: Long? = null

    /** nombre_usuario del perfil logueado, para el saludo de Home. `null` si no hay sesión. */
    @Volatile
    var nombreUsuario: String? = null

    /**
     * Scope que vive mientras viva la app, para tareas que no deben cortarse al cerrar una
     * pantalla (p. ej. avisarle a Supabase que se cerró la sesión).
     */
    val scope = CoroutineScope(SupervisorJob())
}
