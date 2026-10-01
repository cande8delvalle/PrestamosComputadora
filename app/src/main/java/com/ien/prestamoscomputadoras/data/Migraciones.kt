package com.ien.prestamoscomputadoras.data

import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ien.prestamoscomputadoras.data.entity.Rol

/**
 * v4 -> v5: roles y permisos. Conserva todos los datos. Como solo puede existir un Admin, de
 * las cuentas que ya existían solo la primera (id más bajo) queda como Admin; el resto pasa a
 * Personal Administrativo.
 *
 * El SQL de las tablas es copia exacta del que genera Room (AppDatabase_Impl): si no coincide,
 * Room rechaza la base al abrirla.
 */
val MIGRACION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `rol` (`id_rol` INTEGER NOT NULL, `nombre` TEXT NOT NULL, " +
                "`es_sistema` INTEGER NOT NULL, PRIMARY KEY(`id_rol`))",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `rol_permiso` (`id_rol` INTEGER NOT NULL, `permiso` TEXT NOT NULL, " +
                "PRIMARY KEY(`id_rol`, `permiso`), FOREIGN KEY(`id_rol`) REFERENCES `rol`(`id_rol`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        sembrarRolesYPermisos(db)

        // SQLite no permite agregar con ALTER TABLE una columna con FK y valor por defecto
        // no nulo: se recrea la tabla, se copian los datos y se reemplaza la vieja.
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `administrador_nueva` (`id_administrador` INTEGER PRIMARY KEY " +
                "AUTOINCREMENT NOT NULL, `nombre` TEXT NOT NULL, `apellido` TEXT NOT NULL, " +
                "`nombre_usuario` TEXT NOT NULL, `dni` TEXT NOT NULL, `email` TEXT NOT NULL, " +
                "`contrasena_hash` TEXT NOT NULL, `id_rol` INTEGER NOT NULL, FOREIGN KEY(`id_rol`) " +
                "REFERENCES `rol`(`id_rol`) ON UPDATE NO ACTION ON DELETE RESTRICT )",
        )
        db.execSQL(
            "INSERT INTO `administrador_nueva` (`id_administrador`, `nombre`, `apellido`, " +
                "`nombre_usuario`, `dni`, `email`, `contrasena_hash`, `id_rol`) " +
                "SELECT `id_administrador`, `nombre`, `apellido`, `nombre_usuario`, `dni`, `email`, " +
                "`contrasena_hash`, CASE WHEN `id_administrador` = " +
                "(SELECT MIN(`id_administrador`) FROM `administrador`) " +
                "THEN ${Rol.ID_ADMIN} ELSE ${Rol.ID_PERSONAL} END FROM `administrador`",
        )
        db.execSQL("DROP TABLE `administrador`")
        db.execSQL("ALTER TABLE `administrador_nueva` RENAME TO `administrador`")
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_administrador_email` ON `administrador` (`email`)",
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_administrador_nombre_usuario` " +
                "ON `administrador` (`nombre_usuario`)",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_administrador_id_rol` ON `administrador` (`id_rol`)")
        crearTriggersUnSoloAdmin(db)
    }
}

/** En una instalación nueva, crea los roles y los permisos iniciales junto con la base. */
val CALLBACK_DATOS_INICIALES = object : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        sembrarRolesYPermisos(db)
        crearTriggersUnSoloAdmin(db)
    }
}

/**
 * Garantía a nivel base de que existe UN solo Admin: cualquier INSERT o UPDATE que dejaría un
 * segundo Admin se aborta (`SQLiteConstraintException`), venga del código que venga.
 * Room no valida triggers, así que no afectan la verificación del esquema.
 */
private fun crearTriggersUnSoloAdmin(db: SupportSQLiteDatabase) {
    db.execSQL(
        "CREATE TRIGGER IF NOT EXISTS `un_solo_admin_insert` BEFORE INSERT ON `administrador` " +
            "WHEN NEW.`id_rol` = ${Rol.ID_ADMIN} AND EXISTS(SELECT 1 FROM `administrador` " +
            "WHERE `id_rol` = ${Rol.ID_ADMIN}) " +
            "BEGIN SELECT RAISE(ABORT, 'Solo puede existir un Admin'); END",
    )
    db.execSQL(
        "CREATE TRIGGER IF NOT EXISTS `un_solo_admin_update` BEFORE UPDATE OF `id_rol` ON `administrador` " +
            "WHEN NEW.`id_rol` = ${Rol.ID_ADMIN} AND EXISTS(SELECT 1 FROM `administrador` " +
            "WHERE `id_rol` = ${Rol.ID_ADMIN} AND `id_administrador` <> NEW.`id_administrador`) " +
            "BEGIN SELECT RAISE(ABORT, 'Solo puede existir un Admin'); END",
    )
}

/** Los dos roles fijos y los permisos con los que arranca Personal Administrativo. */
private fun sembrarRolesYPermisos(db: SupportSQLiteDatabase) {
    db.execSQL(
        "INSERT OR IGNORE INTO `rol` (`id_rol`, `nombre`, `es_sistema`) VALUES " +
            "(${Rol.ID_ADMIN}, 'Admin', 1), (${Rol.ID_PERSONAL}, 'Personal Administrativo', 0)",
    )
    Permiso.inicialesPersonal.forEach { permiso ->
        db.execSQL(
            "INSERT OR IGNORE INTO `rol_permiso` (`id_rol`, `permiso`) VALUES (?, ?)",
            arrayOf<Any>(Rol.ID_PERSONAL, permiso.name),
        )
    }
}
