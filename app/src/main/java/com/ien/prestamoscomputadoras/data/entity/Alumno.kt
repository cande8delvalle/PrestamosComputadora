package com.ien.prestamoscomputadoras.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Alumno al que se le presta una computadora. Se da de alta al registrar su primer préstamo. */
@Entity(
    tableName = "alumno",
    // Índice (no único) para que la búsqueda por DNI sea rápida.
    indices = [Index(value = ["dni"])],
)
data class Alumno(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id_alumno")
    val idAlumno: Long = 0,
    val nombre: String,
    val apellido: String,
    val dni: String,
)
