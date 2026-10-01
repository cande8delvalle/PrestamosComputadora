package com.ien.prestamoscomputadoras.util

/**
 * Administrador con la sesión iniciada. Se setea en el login y se limpia en el logout.
 *
 * Vive solo en memoria: si Android mata el proceso, al volver la sesión queda en `null`
 * aunque la app reabra en Home.
 */
object SesionActual {
    @Volatile
    var administradorId: Long? = null
}
