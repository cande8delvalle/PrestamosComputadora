package com.ien.prestamoscomputadoras.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.ien.prestamoscomputadoras.data.dao.AdministradorDao
import com.ien.prestamoscomputadoras.data.dao.AlumnoDao
import com.ien.prestamoscomputadoras.data.dao.ComputadoraDao
import com.ien.prestamoscomputadoras.data.entity.Administrador
import com.ien.prestamoscomputadoras.data.entity.Alumno
import com.ien.prestamoscomputadoras.data.entity.Computadora
import com.ien.prestamoscomputadoras.data.entity.EstadoComputadora
import com.ien.prestamoscomputadoras.data.entity.Prestamo

@Database(
    entities = [
        Administrador::class,
        Alumno::class,
        Computadora::class,
        Prestamo::class,
        EstadoComputadora::class,
    ],
    version = 1,
    // Sin export de esquema por ahora. Activarlo (con room.schemaLocation) antes de la
    // primera versión publicada, para poder escribir migraciones.
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun administradorDao(): AdministradorDao
    abstract fun alumnoDao(): AlumnoDao
    abstract fun computadoraDao(): ComputadoraDao

    companion object {
        private const val NOMBRE_BASE = "prestamos_computadoras.db"

        @Volatile
        private var instancia: AppDatabase? = null

        /** Devuelve la única instancia de la base (se crea la primera vez que se pide). */
        fun getInstance(context: Context): AppDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    // applicationContext: evita retener una Activity en el singleton.
                    context.applicationContext,
                    AppDatabase::class.java,
                    NOMBRE_BASE,
                ).build().also { instancia = it }
            }
    }
}
