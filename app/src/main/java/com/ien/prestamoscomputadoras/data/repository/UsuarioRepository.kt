package com.ien.prestamoscomputadoras.data.repository

import com.ien.prestamoscomputadoras.data.dao.UsuarioDao
import com.ien.prestamoscomputadoras.data.entity.Usuario
import com.ien.prestamoscomputadoras.data.entity.Rol
import com.ien.prestamoscomputadoras.util.hashPassword

/** Alta y autenticación de usuarios. */
class UsuarioRepository(private val usuarioDao: UsuarioDao) {

    /**
     * Da de alta la cuenta asignándole el rol; se ignora el [Usuario.idRol] recibido.
     *
     * Solo puede existir UN Admin en todo el sistema: si ya existe, la cuenta nueva es
     * Personal Administrativo sin excepción. Solo cuando todavía no hay Admin (la primera
     * cuenta de la app) se registra como Admin. Devuelve el id generado.
     *
     * Si aun así se intentara insertar un segundo Admin, la base lo rechaza (trigger
     * `un_solo_admin_insert`) con `SQLiteConstraintException`.
     */
    suspend fun registrar(usuario: Usuario): Long {
        val rol = if (usuarioDao.existeAdmin()) Rol.ID_PERSONAL else Rol.ID_ADMIN
        return usuarioDao.insertar(usuario.copy(idRol = rol))
    }

    suspend fun existeNombreUsuario(nombreUsuario: String): Boolean =
        usuarioDao.existeNombreUsuario(nombreUsuario)

    suspend fun existeEmail(email: String): Boolean = usuarioDao.existeEmail(email)

    /**
     * Devuelve la cuenta si [usuario] (nombre de usuario, o email si contiene "@")
     * existe y [contrasena] coincide con su hash. `null` si alguna de las dos cosas falla.
     */
    suspend fun autenticar(usuario: String, contrasena: String): Usuario? {
        val cuenta = if ("@" in usuario) {
            usuarioDao.buscarPorEmail(usuario.lowercase())
        } else {
            usuarioDao.buscarPorNombreUsuario(usuario)
        }
        return cuenta?.takeIf { it.contrasenaHash == hashPassword(contrasena) }
    }
}
