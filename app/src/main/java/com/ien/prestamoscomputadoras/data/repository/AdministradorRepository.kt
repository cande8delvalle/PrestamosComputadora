package com.ien.prestamoscomputadoras.data.repository

import com.ien.prestamoscomputadoras.data.dao.AdministradorDao
import com.ien.prestamoscomputadoras.data.entity.Administrador
import com.ien.prestamoscomputadoras.data.entity.Rol
import com.ien.prestamoscomputadoras.util.hashPassword

/** Alta y autenticación de administradores. */
class AdministradorRepository(private val administradorDao: AdministradorDao) {

    /**
     * Da de alta la cuenta asignándole el rol; se ignora el [Administrador.idRol] recibido.
     *
     * Solo puede existir UN Admin en todo el sistema: si ya existe, la cuenta nueva es
     * Personal Administrativo sin excepción. Solo cuando todavía no hay Admin (la primera
     * cuenta de la app) se registra como Admin. Devuelve el id generado.
     *
     * Si aun así se intentara insertar un segundo Admin, la base lo rechaza (trigger
     * `un_solo_admin_insert`) con `SQLiteConstraintException`.
     */
    suspend fun registrar(administrador: Administrador): Long {
        val rol = if (administradorDao.existeAdmin()) Rol.ID_PERSONAL else Rol.ID_ADMIN
        return administradorDao.insertar(administrador.copy(idRol = rol))
    }

    suspend fun existeNombreUsuario(nombreUsuario: String): Boolean =
        administradorDao.existeNombreUsuario(nombreUsuario)

    suspend fun existeEmail(email: String): Boolean = administradorDao.existeEmail(email)

    /**
     * Devuelve el administrador si [usuario] (nombre de usuario, o email si contiene "@")
     * existe y [contrasena] coincide con su hash. `null` si alguna de las dos cosas falla.
     */
    suspend fun autenticar(usuario: String, contrasena: String): Administrador? {
        val administrador = if ("@" in usuario) {
            administradorDao.buscarPorEmail(usuario.lowercase())
        } else {
            administradorDao.buscarPorNombreUsuario(usuario)
        }
        return administrador?.takeIf { it.contrasenaHash == hashPassword(contrasena) }
    }
}
