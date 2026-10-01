package com.ien.prestamoscomputadoras.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ien.prestamoscomputadoras.ui.components.BackHeader
import com.ien.prestamoscomputadoras.ui.theme.IenCream
import com.ien.prestamoscomputadoras.ui.theme.IenLavender
import com.ien.prestamoscomputadoras.ui.theme.IenLavenderDark
import com.ien.prestamoscomputadoras.ui.theme.IenPurple
import com.ien.prestamoscomputadoras.ui.theme.IenYellow
import com.ien.prestamoscomputadoras.ui.theme.PrestamosComputadorasTheme
import com.ien.prestamoscomputadoras.viewmodel.ERROR_CAMPO_OBLIGATORIO
import com.ien.prestamoscomputadoras.viewmodel.ERROR_DNI_ALUMNO_DUPLICADO
import com.ien.prestamoscomputadoras.viewmodel.RegistrarAlumnoUiState
import com.ien.prestamoscomputadoras.viewmodel.RegistrarAlumnoViewModel

/** Rojo para los mensajes de error de los campos (mismo que en Register). */
private val ErrorRed = Color(0xFFD32F2F)

/** Verde del círculo de éxito del modal (mismo que en Register). */
private val SuccessGreen = Color(0xFF33A852)

/**
 * Pantalla "Registrar Alumno", a la que se llega desde la acción rápida de Home.
 *
 * Este composable solo conecta el [RegistrarAlumnoViewModel] con la UI; el diseño vive en
 * [RegistrarAlumnoContent] (stateless), que es el que se previsualiza.
 */
@Composable
fun RegistrarAlumnoScreen(
    modifier: Modifier = Modifier,
    viewModel: RegistrarAlumnoViewModel = viewModel(factory = RegistrarAlumnoViewModel.Factory),
    /** Flecha de "volver" del header. */
    onVolverClick: () -> Unit = {},
    /** Botón "Volver al inicio" del modal de éxito (navega a Home). */
    onVolverAlInicioClick: () -> Unit = {},
) {
    RegistrarAlumnoContent(
        uiState = viewModel.uiState,
        onNombreChange = viewModel::onNombreChange,
        onApellidoChange = viewModel::onApellidoChange,
        onDniChange = viewModel::onDniChange,
        onRegistrarClick = viewModel::onRegistrarClick,
        onRegistrarOtroClick = viewModel::onRegistrarOtroClick,
        onVolverAlInicioClick = {
            // Primero se cierra el modal (estado del ViewModel) y después se navega a Home.
            viewModel.onCerrarModalExito()
            onVolverAlInicioClick()
        },
        onVolverClick = onVolverClick,
        modifier = modifier,
    )
}

@Composable
private fun RegistrarAlumnoContent(
    uiState: RegistrarAlumnoUiState,
    onNombreChange: (String) -> Unit,
    onApellidoChange: (String) -> Unit,
    onDniChange: (String) -> Unit,
    onRegistrarClick: () -> Unit,
    onRegistrarOtroClick: () -> Unit,
    onVolverAlInicioClick: () -> Unit,
    onVolverClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(IenCream),
        ) {
            BackHeader(titulo = "Registrar Alumno", onVolverClick = onVolverClick)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(remember { ScrollState(0) })
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp),
            ) {
                Spacer(Modifier.height(24.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(IenLavender, RoundedCornerShape(16.dp))
                        .padding(horizontal = 18.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    AlumnoField(
                        label = "NOMBRE",
                        value = uiState.nombre,
                        onValueChange = onNombreChange,
                        placeholder = "Ej: María",
                        error = uiState.errorNombre,
                        enabled = !uiState.isLoading,
                    )
                    AlumnoField(
                        label = "APELLIDO",
                        value = uiState.apellido,
                        onValueChange = onApellidoChange,
                        placeholder = "Ej: Gonzales",
                        error = uiState.errorApellido,
                        enabled = !uiState.isLoading,
                    )
                    AlumnoField(
                        label = "DNI",
                        value = uiState.dni,
                        onValueChange = onDniChange,
                        placeholder = "Ej: 45799888",
                        error = uiState.errorDni,
                        enabled = !uiState.isLoading,
                        keyboardType = KeyboardType.Number,
                    )
                }

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = onRegistrarClick,
                    enabled = !uiState.isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = IenYellow,
                        contentColor = IenPurple,
                        disabledContainerColor = IenYellow.copy(alpha = 0.6f),
                        disabledContentColor = IenPurple.copy(alpha = 0.7f),
                    ),
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            color = IenPurple,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(22.dp),
                        )
                    } else {
                        Text(
                            text = "Registrar Alumno",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }

        if (uiState.mostrarModalExito) {
            AlumnoRegistradoOverlay(
                onRegistrarOtroClick = onRegistrarOtroClick,
                onVolverAlInicioClick = onVolverAlInicioClick,
            )
        }
    }
}

/** Campo del formulario: label violeta en mayúscula, input lavanda y error rojo debajo. */
@Composable
private fun AlumnoField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    error: String?,
    enabled: Boolean,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    Column {
        Text(
            text = label,
            color = IenPurple,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            modifier = Modifier.padding(bottom = 6.dp),
        )

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            singleLine = true,
            placeholder = { Text(placeholder) },
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            isError = error != null,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = IenPurple,
                unfocusedBorderColor = Color.Transparent,
                disabledBorderColor = Color.Transparent,
                errorBorderColor = ErrorRed,
                cursorColor = IenPurple,
                focusedContainerColor = IenLavenderDark,
                unfocusedContainerColor = IenLavenderDark,
                disabledContainerColor = IenLavenderDark,
                errorContainerColor = IenLavenderDark,
            ),
        )

        if (error != null) {
            Text(
                text = error,
                color = ErrorRed,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 4.dp, start = 4.dp),
            )
        }
    }
}

/**
 * Modal de éxito: velo oscuro sobre el formulario y, centrada encima, una tarjeta con el
 * círculo verde + check, el texto violeta y los dos botones. El velo solo bloquea los
 * toques al formulario: el modal se cierra únicamente con uno de los dos botones.
 */
@Composable
private fun AlumnoRegistradoOverlay(
    onRegistrarOtroClick: () -> Unit,
    onVolverAlInicioClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            ),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = IenCream,
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .background(SuccessGreen, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(52.dp),
                    )
                }

                Spacer(Modifier.height(20.dp))

                Text(
                    text = "Alumno Registrado Correctamente",
                    color = IenPurple,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(24.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onRegistrarOtroClick,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = IenYellow,
                            contentColor = IenPurple,
                        ),
                    ) {
                        Text(
                            text = "Registrar otro alumno",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                        )
                    }
                    OutlinedButton(
                        onClick = onVolverAlInicioClick,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.5.dp, IenPurple),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White,
                            contentColor = IenPurple,
                        ),
                    ) {
                        Text(
                            text = "Volver al inicio",
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun RegistrarAlumnoScreenPreview() {
    PrestamosComputadorasTheme {
        RegistrarAlumnoContent(
            uiState = RegistrarAlumnoUiState(),
            onNombreChange = {},
            onApellidoChange = {},
            onDniChange = {},
            onRegistrarClick = {},
            onRegistrarOtroClick = {},
            onVolverAlInicioClick = {},
            onVolverClick = {},
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Registrar Alumno con errores")
@Composable
private fun RegistrarAlumnoErroresPreview() {
    PrestamosComputadorasTheme {
        RegistrarAlumnoContent(
            uiState = RegistrarAlumnoUiState(
                nombre = "María",
                dni = "45799888",
                errorApellido = ERROR_CAMPO_OBLIGATORIO,
                errorDni = ERROR_DNI_ALUMNO_DUPLICADO,
            ),
            onNombreChange = {},
            onApellidoChange = {},
            onDniChange = {},
            onRegistrarClick = {},
            onRegistrarOtroClick = {},
            onVolverAlInicioClick = {},
            onVolverClick = {},
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Registrar Alumno modal éxito")
@Composable
private fun RegistrarAlumnoModalPreview() {
    PrestamosComputadorasTheme {
        RegistrarAlumnoContent(
            uiState = RegistrarAlumnoUiState(mostrarModalExito = true),
            onNombreChange = {},
            onApellidoChange = {},
            onDniChange = {},
            onRegistrarClick = {},
            onRegistrarOtroClick = {},
            onVolverAlInicioClick = {},
            onVolverClick = {},
        )
    }
}
