package com.ien.prestamoscomputadoras.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.ien.prestamoscomputadoras.ui.theme.IenLavenderDark
import com.ien.prestamoscomputadoras.ui.theme.IenPurple
import com.ien.prestamoscomputadoras.ui.theme.IenPurpleDark

/*
 * Campo de texto único de la app. Todas las pantallas lo usan, así el estilo se cambia en un
 * solo lugar:
 * - Lo que escribe el usuario: violeta oscuro y semibold (bien distinto del placeholder).
 * - El placeholder: gris violáceo medio y peso normal. Se lee bien pero no se confunde con
 *   lo escrito (más claro y sin negrita).
 */

/** Color de lo que escribe el usuario: el de mayor contraste de la app. */
val ColorTextoEscrito = IenPurpleDark

/**
 * Color del placeholder (los ejemplos). Contraste medido con la fórmula de WCAG:
 * 6,6:1 sobre blanco y 4,5:1 sobre el lavanda de los formularios (IenLavenderDark).
 * Antes se usaba el gris del tema, que sobre lavanda quedaba por debajo de 4,5:1.
 */
val ColorPlaceholder = Color(0xFF625873)

/** Estilo de lo que escribe el usuario. */
@Composable
fun estiloTextoEscrito(): TextStyle = LocalTextStyle.current.copy(fontWeight = FontWeight.SemiBold)

/** Los dos fondos de campo que usa la app. */
enum class FondoCampo {
    /** Blanco con borde violeta suave (Login y Registro, sobre fondo crema). */
    BLANCO,

    /** Lavanda sin borde (formularios en tarjeta lavanda: alumno, computadora, préstamo...). */
    LAVANDA,
}

/** Colores del campo según el fondo. El color del texto escrito es el mismo en los dos. */
@Composable
fun coloresCampo(fondo: FondoCampo): TextFieldColors {
    val textoEscrito = ColorTextoEscrito
    return when (fondo) {
        FondoCampo.BLANCO -> OutlinedTextFieldDefaults.colors(
            focusedPlaceholderColor = ColorPlaceholder,
            unfocusedPlaceholderColor = ColorPlaceholder,
            errorPlaceholderColor = ColorPlaceholder,
            disabledPlaceholderColor = ColorPlaceholder.copy(alpha = 0.6f),
            focusedTextColor = textoEscrito,
            unfocusedTextColor = textoEscrito,
            errorTextColor = textoEscrito,
            disabledTextColor = textoEscrito.copy(alpha = 0.6f),
            focusedBorderColor = IenPurple,
            unfocusedBorderColor = IenPurple.copy(alpha = 0.35f),
            errorBorderColor = FormErrorRed,
            focusedLeadingIconColor = IenPurple,
            unfocusedLeadingIconColor = IenPurple.copy(alpha = 0.6f),
            focusedTrailingIconColor = IenPurple,
            unfocusedTrailingIconColor = IenPurple.copy(alpha = 0.6f),
            cursorColor = IenPurple,
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            errorContainerColor = Color.White,
            disabledContainerColor = Color.White,
        )

        FondoCampo.LAVANDA -> OutlinedTextFieldDefaults.colors(
            focusedPlaceholderColor = ColorPlaceholder,
            unfocusedPlaceholderColor = ColorPlaceholder,
            errorPlaceholderColor = ColorPlaceholder,
            disabledPlaceholderColor = ColorPlaceholder.copy(alpha = 0.6f),
            focusedTextColor = textoEscrito,
            unfocusedTextColor = textoEscrito,
            errorTextColor = textoEscrito,
            disabledTextColor = textoEscrito.copy(alpha = 0.6f),
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
    }
}

/**
 * Campo de texto de la app (envuelve a OutlinedTextField con el estilo común).
 * Los parámetros son los mismos de OutlinedTextField que usan las pantallas.
 */
@Composable
fun CampoTexto(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    fondo: FondoCampo = FondoCampo.LAVANDA,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    singleLine: Boolean = false,
    minLines: Int = 1,
    isError: Boolean = false,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        readOnly = readOnly,
        textStyle = estiloTextoEscrito(),
        // El placeholder: ColorPlaceholder (en coloresCampo) y peso normal.
        placeholder = placeholder?.let { texto -> { Text(texto, fontWeight = FontWeight.Normal) } },
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        isError = isError,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        singleLine = singleLine,
        minLines = minLines,
        shape = RoundedCornerShape(12.dp),
        colors = coloresCampo(fondo),
    )
}
