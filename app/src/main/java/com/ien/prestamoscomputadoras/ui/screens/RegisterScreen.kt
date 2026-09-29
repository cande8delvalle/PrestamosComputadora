package com.ien.prestamoscomputadoras.ui.screens

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ien.prestamoscomputadoras.ui.components.AppHeader
import com.ien.prestamoscomputadoras.ui.theme.IenCream
import com.ien.prestamoscomputadoras.ui.theme.IenGreyText
import com.ien.prestamoscomputadoras.ui.theme.IenPurple
import com.ien.prestamoscomputadoras.ui.theme.IenYellow
import com.ien.prestamoscomputadoras.ui.theme.PrestamosComputadorasTheme
import com.ien.prestamoscomputadoras.viewmodel.ERROR_CAMPO_OBLIGATORIO
import com.ien.prestamoscomputadoras.viewmodel.ERROR_CONTRASENAS_NO_COINCIDEN
import com.ien.prestamoscomputadoras.viewmodel.RegisterFieldErrors
import com.ien.prestamoscomputadoras.viewmodel.RegisterUiState
import com.ien.prestamoscomputadoras.viewmodel.RegisterViewModel

/** Rojo para los mensajes de error de los campos. */
private val ErrorRed = Color(0xFFD32F2F)

/** Verde del círculo de éxito del modal. */
private val SuccessGreen = Color(0xFF33A852)

/**
 * Pantalla de "Crear cuenta".
 *
 * Este composable solo conecta el [RegisterViewModel] con la UI; el diseño vive en
 * [RegisterContent] (stateless), que es el que se previsualiza.
 */
@Composable
fun RegisterScreen(
    modifier: Modifier = Modifier,
    viewModel: RegisterViewModel = viewModel(),
    onIniciarSesionClick: () -> Unit = {},
    /** Se invoca al presionar "Aceptar" en el modal de "Cuenta Creada" (redirige a Login). */
    onCuentaCreada: () -> Unit = {},
) {
    RegisterContent(
        uiState = viewModel.uiState,
        onNombreChange = viewModel::onNombreChange,
        onApellidoChange = viewModel::onApellidoChange,
        onDniChange = viewModel::onDniChange,
        onCorreoChange = viewModel::onCorreoChange,
        onContrasenaChange = viewModel::onContrasenaChange,
        onRepetirContrasenaChange = viewModel::onRepetirContrasenaChange,
        onToggleMostrarContrasena = viewModel::onToggleMostrarContrasena,
        onToggleMostrarRepetirContrasena = viewModel::onToggleMostrarRepetirContrasena,
        onCrearCuentaClick = viewModel::onCrearCuentaClick,
        onAceptarModalExito = {
            // Primero se cierra el modal (estado del ViewModel) y después se navega a Login.
            viewModel.onAceptarModalExito()
            onCuentaCreada()
        },
        onIniciarSesionClick = onIniciarSesionClick,
        modifier = modifier,
    )
}

@Composable
private fun RegisterContent(
    uiState: RegisterUiState,
    onNombreChange: (String) -> Unit,
    onApellidoChange: (String) -> Unit,
    onDniChange: (String) -> Unit,
    onCorreoChange: (String) -> Unit,
    onContrasenaChange: (String) -> Unit,
    onRepetirContrasenaChange: (String) -> Unit,
    onToggleMostrarContrasena: () -> Unit,
    onToggleMostrarRepetirContrasena: () -> Unit,
    onCrearCuentaClick: () -> Unit,
    onAceptarModalExito: () -> Unit,
    onIniciarSesionClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(IenCream),
        ) {
            AppHeader()

            // ScrollState no persistido (no `rememberScrollState`): al entrar a la pantalla
            // el formulario SIEMPRE arranca desde arriba (título "Bienvenido/a" + "Nombre"),
            // nunca en una posición scrolleada heredada de una visita anterior.
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(remember { ScrollState(0) })
                    .imePadding()
                    .padding(horizontal = 24.dp),
            ) {
                Spacer(Modifier.height(24.dp))

                Text(
                    text = "Bienvenido/a",
                    color = IenPurple,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Ingresá tus datos para crear una cuenta",
                    color = IenGreyText,
                    style = MaterialTheme.typography.bodyMedium,
                )

                Spacer(Modifier.height(24.dp))

                RegisterField(
                    label = "Nombre",
                    value = uiState.nombre,
                    onValueChange = onNombreChange,
                    placeholder = "Juan",
                    error = uiState.errores.nombre,
                    leadingIcon = Icons.Filled.Person,
                    keyboardType = KeyboardType.Text,
                )

                RegisterField(
                    label = "Apellido",
                    value = uiState.apellido,
                    onValueChange = onApellidoChange,
                    placeholder = "Pérez",
                    error = uiState.errores.apellido,
                    leadingIcon = Icons.Filled.Person,
                    keyboardType = KeyboardType.Text,
                )

                RegisterField(
                    label = "DNI",
                    value = uiState.dni,
                    onValueChange = onDniChange,
                    placeholder = "40123456",
                    error = uiState.errores.dni,
                    leadingIcon = Icons.Filled.Badge,
                    keyboardType = KeyboardType.Number,
                )

                RegisterField(
                    label = "Correo electrónico",
                    value = uiState.correo,
                    onValueChange = onCorreoChange,
                    placeholder = "admin@ien.edu.ar",
                    error = uiState.errores.correo,
                    leadingIcon = Icons.Filled.Email,
                    keyboardType = KeyboardType.Email,
                )

                RegisterField(
                    label = "Contraseña",
                    value = uiState.contrasena,
                    onValueChange = onContrasenaChange,
                    placeholder = "********",
                    error = uiState.errores.contrasena,
                    leadingIcon = Icons.Filled.Lock,
                    keyboardType = KeyboardType.Password,
                    esPassword = true,
                    mostrarPassword = uiState.mostrarContrasena,
                    onToggleMostrarPassword = onToggleMostrarContrasena,
                )

                RegisterField(
                    label = "Repetir Contraseña",
                    value = uiState.repetirContrasena,
                    onValueChange = onRepetirContrasenaChange,
                    placeholder = "********",
                    error = uiState.errores.repetirContrasena,
                    leadingIcon = Icons.Filled.Lock,
                    keyboardType = KeyboardType.Password,
                    esPassword = true,
                    mostrarPassword = uiState.mostrarRepetirContrasena,
                    onToggleMostrarPassword = onToggleMostrarRepetirContrasena,
                )

                Spacer(Modifier.height(20.dp))

                Button(
                    onClick = onCrearCuentaClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = IenYellow,
                        contentColor = IenPurple,
                    ),
                ) {
                    Text(
                        text = "Crear Cuenta",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                    )
                }

                Spacer(Modifier.height(20.dp))

                IniciarSesionRow(onIniciarSesionClick = onIniciarSesionClick)

                Spacer(Modifier.height(28.dp))

                // El footer va DENTRO del área scrolleable, como último elemento: así el
                // orden campos -> botón "Crear Cuenta" -> link "Iniciar Sesión" -> footer
                // siempre entra en el scroll y nada queda tapado ni recortado, en cualquier
                // tamaño de pantalla.
                RegisterFooter()

                Spacer(Modifier.height(12.dp))
            }
        }

        if (uiState.mostrarModalExito) {
            SuccessOverlay(onAceptar = onAceptarModalExito)
        }
    }
}

@Composable
private fun RegisterField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    error: String?,
    leadingIcon: ImageVector,
    keyboardType: KeyboardType,
    esPassword: Boolean = false,
    mostrarPassword: Boolean = false,
    onToggleMostrarPassword: () -> Unit = {},
) {
    Column(modifier = Modifier.padding(bottom = 14.dp)) {
        Text(
            text = label,
            color = IenPurple,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 6.dp),
        )

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = { Icon(leadingIcon, contentDescription = null) },
            trailingIcon = if (esPassword) {
                {
                    val icono =
                        if (mostrarPassword) Icons.Filled.Visibility
                        else Icons.Filled.VisibilityOff
                    val descripcion =
                        if (mostrarPassword) "Ocultar contraseña" else "Mostrar contraseña"
                    IconButton(onClick = onToggleMostrarPassword) {
                        Icon(icono, contentDescription = descripcion)
                    }
                }
            } else {
                null
            },
            placeholder = { Text(placeholder) },
            visualTransformation = when {
                !esPassword -> VisualTransformation.None
                mostrarPassword -> VisualTransformation.None
                else -> PasswordVisualTransformation()
            },
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            isError = error != null,
            shape = RoundedCornerShape(12.dp),
            colors = ienFieldColors(),
        )

        if (error != null) {
            Text(
                text = error,
                color = ErrorRed,
                // Mismo tamaño de fuente que usan los placeholders de los inputs.
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 4.dp, start = 4.dp),
            )
        }
    }
}

@Composable
private fun IniciarSesionRow(onIniciarSesionClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "¿Ya tenés una cuenta? ",
            color = IenGreyText,
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = "Iniciar Sesión",
            color = IenPurple,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.clickable(onClick = onIniciarSesionClick),
        )
    }
}

@Composable
private fun RegisterFooter() {
    Text(
        text = "Versión 1.0  ·  IEN 2026",
        color = IenGreyText,
        fontSize = 11.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 4.dp),
    )
}

/**
 * Modal de éxito. Se dibuja como overlay a pantalla completa (así también se ve en el
 * @Preview): un velo oscuro que atenúa el formulario detrás y, centrada encima, una
 * tarjeta con el círculo verde + check blanco, el texto violeta bold y el botón amarillo
 * "Aceptar".
 *
 * El velo intercepta los toques para que no lleguen al formulario; tocarlo también cierra
 * el modal, igual que el botón "Aceptar".
 */
@Composable
private fun SuccessOverlay(onAceptar: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onAceptar,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = IenCream,
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 36.dp)
                // Consume el toque sobre la tarjeta para que no cierre el modal.
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp, vertical = 32.dp),
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
                    text = "Cuenta Creada Correctamente",
                    color = IenPurple,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = onAceptar,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = IenYellow,
                        contentColor = IenPurple,
                    ),
                ) {
                    Text(text = "Aceptar", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
private fun ienFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = IenPurple,
    unfocusedBorderColor = IenPurple.copy(alpha = 0.35f),
    focusedLeadingIconColor = IenPurple,
    unfocusedLeadingIconColor = IenPurple.copy(alpha = 0.6f),
    focusedTrailingIconColor = IenPurple,
    unfocusedTrailingIconColor = IenPurple.copy(alpha = 0.6f),
    cursorColor = IenPurple,
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White,
)

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun RegisterScreenPreview() {
    PrestamosComputadorasTheme {
        RegisterContent(
            uiState = RegisterUiState(),
            onNombreChange = {},
            onApellidoChange = {},
            onDniChange = {},
            onCorreoChange = {},
            onContrasenaChange = {},
            onRepetirContrasenaChange = {},
            onToggleMostrarContrasena = {},
            onToggleMostrarRepetirContrasena = {},
            onCrearCuentaClick = {},
            onAceptarModalExito = {},
            onIniciarSesionClick = {},
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Register con errores")
@Composable
private fun RegisterScreenErroresPreview() {
    PrestamosComputadorasTheme {
        RegisterContent(
            uiState = RegisterUiState(
                nombre = "Juan",
                apellido = "",
                dni = "",
                correo = "juan@ien.edu.ar",
                contrasena = "secreta1",
                repetirContrasena = "secreta2",
                errores = RegisterFieldErrors(
                    apellido = ERROR_CAMPO_OBLIGATORIO,
                    dni = ERROR_CAMPO_OBLIGATORIO,
                    repetirContrasena = ERROR_CONTRASENAS_NO_COINCIDEN,
                ),
            ),
            onNombreChange = {},
            onApellidoChange = {},
            onDniChange = {},
            onCorreoChange = {},
            onContrasenaChange = {},
            onRepetirContrasenaChange = {},
            onToggleMostrarContrasena = {},
            onToggleMostrarRepetirContrasena = {},
            onCrearCuentaClick = {},
            onAceptarModalExito = {},
            onIniciarSesionClick = {},
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Register modal éxito")
@Composable
private fun RegisterScreenModalPreview() {
    PrestamosComputadorasTheme {
        RegisterContent(
            uiState = RegisterUiState(mostrarModalExito = true),
            onNombreChange = {},
            onApellidoChange = {},
            onDniChange = {},
            onCorreoChange = {},
            onContrasenaChange = {},
            onRepetirContrasenaChange = {},
            onToggleMostrarContrasena = {},
            onToggleMostrarRepetirContrasena = {},
            onCrearCuentaClick = {},
            onAceptarModalExito = {},
            onIniciarSesionClick = {},
        )
    }
}
