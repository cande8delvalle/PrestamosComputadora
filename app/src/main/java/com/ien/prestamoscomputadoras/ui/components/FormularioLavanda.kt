package com.ien.prestamoscomputadoras.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ien.prestamoscomputadoras.ui.theme.IenLavenderDark
import com.ien.prestamoscomputadoras.ui.theme.IenPurple

/*
 * Piezas del formulario "tarjeta lavanda" de las pantallas secundarias (mismo look que
 * Registrar Alumno): label violeta en mayúscula, input lavanda sin borde y error rojo debajo.
 */

/** Rojo de los mensajes de error de los campos. */
val FormErrorRed = Color(0xFFD32F2F)

/** Label del campo, en violeta bold mayúscula. */
@Composable
fun FormLabel(texto: String) {
    Text(
        text = texto,
        color = IenPurple,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        modifier = Modifier.padding(bottom = 6.dp),
    )
}

/** Mensaje de error debajo del campo. No dibuja nada si [error] es `null`. */
@Composable
fun FormError(error: String?) {
    if (error != null) {
        Text(
            text = error,
            color = FormErrorRed,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 4.dp, start = 4.dp),
        )
    }
}

/** Colores del input: fondo lavanda oscuro, sin borde salvo al enfocar o con error. */
@Composable
fun lavenderFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = IenPurple,
    unfocusedBorderColor = Color.Transparent,
    disabledBorderColor = Color.Transparent,
    errorBorderColor = FormErrorRed,
    cursorColor = IenPurple,
    focusedContainerColor = IenLavenderDark,
    unfocusedContainerColor = IenLavenderDark,
    disabledContainerColor = IenLavenderDark,
    errorContainerColor = IenLavenderDark,
    focusedTrailingIconColor = IenPurple,
    unfocusedTrailingIconColor = IenPurple,
)
