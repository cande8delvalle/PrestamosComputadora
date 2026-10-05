package com.ien.prestamoscomputadoras.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.ien.prestamoscomputadoras.data.dao.AdministradorDao
import com.ien.prestamoscomputadoras.data.dao.AlumnoDao
import com.ien.prestamoscomputadoras.data.dao.ComputadoraDao
import com.ien.prestamoscomputadoras.data.dao.EstadoComputadoraDao
import com.ien.prestamoscomputadoras.data.dao.HistorialDao
import com.ien.prestamoscomputadoras.data.dao.HomeDao
import com.ien.prestamoscomputadoras.data.dao.PrestamoDao
import com.ien.prestamoscomputadoras.data.dao.RolDao
import com.ien.prestamoscomputadoras.data.entity.Administrador
import com.ien.prestamoscomputadoras.data.entity.Alumno
import com.ien.prestamoscomputadoras.data.entity.Computadora
import com.ien.prestamoscomputadoras.data.entity.EstadoComputadora
import com.ien.prestamoscomputadoras.data.entity.Prestamo
import com.ien.prestamoscomputadoras.data.entity.Rol
import com.ien.prestamoscomputadoras.data.entity.RolPermiso

@Database(
    entities = [
        Administrador::class,
        Alumno::class,
        Computadora::class,
        Prestamo::class,
        EstadoComputadora::class,
        Rol::class,
        RolPermiso::class,
    ],
    // v2: Administrador.nombreUsuario (índice único).
    // v3: se quita Computadora.disponible (se calcula) y se agrega Prestamo.observacionesIniciales.
    // v4: Computadora.modelo (opcional).
    // v5: roles y permisos (tablas rol y rol_permiso, Administrador.id_rol). Con migración real.
    // v6: Prestamo.id_administrador se separa en id_administrador_prestamo y
    //     id_administrador_devolucion. Con migración real.
    version = 6,
    // Sin export de esquema por ahora. Activarlo (con room.schemaLocation) antes de la
    // primera versión publicada, para poder escribir migraciones.
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun administradorDao(): AdministradorDao
    abstract fun alumnoDao(): AlumnoDao
    abstract fun computadoraDao(): ComputadoraDao
    abstract fun prestamoDao(): PrestamoDao
    abstract fun estadoComputadoraDao(): EstadoComputadoraDao
    abstract fun historialDao(): HistorialDao
    abstract fun homeDao(): HomeDao
    abstract fun rolDao(): RolDao

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
                )
                    // Desde v5 hay migraciones reales (ver Migraciones.kt). Las versiones
                    // anteriores a 4 no tienen migración: se borran y recrean.
                    .addMigrations(MIGRACION_4_5, MIGRACION_5_6)
                    .addCallback(CALLBACK_DATOS_INICIALES)
                    .fallbackToDestructiveMigrationFrom(dropAllTables = true, 1, 2, 3)
                    .build().also { instancia = it }
            }
    }
}
