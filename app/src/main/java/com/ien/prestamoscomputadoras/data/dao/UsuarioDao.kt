package com.ien.prestamoscomputadoras.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ien.prestamoscomputadoras.data.entity.Usuario
import com.ien.prestamoscomputadoras.data.entity.Rol

@Dao
interface UsuarioDao {

    /**
     * Inserta y devuelve el id generado. ABORT: si el email ya existe lanza
     * `SQLiteConstraintException` (conviene chequear antes con [existeEmail]).
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertar(usuario: Usuario): Long

    /** Para el login: `null` si no hay ningún usuario con ese email. */
    @Query("SELECT * FROM usuario WHERE email = :email LIMIT 1")
    suspend fun buscarPorEmail(email: String): Usuario?

    /**
     * Para el login con nombre de usuario. Sin distinguir mayúsculas: "jperez" y "Jperez" son
     * la misma cuenta (el registro impide crear las dos, ver [existeNombreUsuario]).
     */
    @Query("SELECT * FROM usuario WHERE nombre_usuario = :nombreUsuario COLLATE NOCASE LIMIT 1")
    suspend fun buscarPorNombreUsuario(nombreUsuario: String): Usuario?

    /** Para el registro: evita crear dos cuentas con el mismo nombre de usuario. */
    @Query("SELECT EXISTS(SELECT 1 FROM usuario WHERE nombre_usuario = :nombreUsuario COLLATE NOCASE)")
    suspend fun existeNombreUsuario(nombreUsuario: String): Boolean

    /** Para el registro: evita crear dos cuentas con el mismo email. */
    @Query("SELECT EXISTS(SELECT 1 FROM usuario WHERE email = :email)")
    suspend fun existeEmail(email: String): Boolean

    /**
     * Para el registro: si ya existe el Admin, toda cuenta nueva es Personal Administrativo.
     * Solo puede haber un Admin (además lo garantizan triggers en la base, ver Migraciones.kt).
     */
    @Query("SELECT EXISTS(SELECT 1 FROM usuario WHERE id_rol = ${Rol.ID_ADMIN})")
    suspend fun existeAdmin(): Boolean
}
