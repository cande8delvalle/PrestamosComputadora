package com.ien.prestamoscomputadoras.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ien.prestamoscomputadoras.data.entity.Administrador

@Dao
interface AdministradorDao {

    /**
     * Inserta y devuelve el id generado. ABORT: si el email ya existe lanza
     * `SQLiteConstraintException` (conviene chequear antes con [existeEmail]).
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertar(administrador: Administrador): Long

    /** Para el login: `null` si no hay ningún administrador con ese email. */
    @Query("SELECT * FROM administrador WHERE email = :email LIMIT 1")
    suspend fun buscarPorEmail(email: String): Administrador?

    /** Para el registro: evita crear dos cuentas con el mismo email. */
    @Query("SELECT EXISTS(SELECT 1 FROM administrador WHERE email = :email)")
    suspend fun existeEmail(email: String): Boolean
}
