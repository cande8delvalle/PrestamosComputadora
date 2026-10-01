package com.ien.prestamoscomputadoras.ui.screens

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.ien.prestamoscomputadoras.viewmodel.LoginUiState
import com.ien.prestamoscomputadoras.viewmodel.LoginViewModel

/**
 * Pantalla de inicio de sesión.
 *
 * Este composable solo conecta el [LoginViewModel] con la UI. Todo el diseño
 * vive en [LoginContent], que es "tonto" (stateless) y por eso se puede previsualizar.
 */
@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = viewModel(factory = LoginViewModel.Factory),
    onCrearCuentaClick: () -> Unit = {},
    /** Se invoca cuando el login pasa la validación (navega a Home). */
    onLoginExitoso: () -> Unit = {},
) {
    val uiState = viewModel.uiState

    LoginContent(
        uiState = uiState,
        onUsuarioChange = viewModel::onUsuarioChange,
        onContrasenaChange = viewModel::onContrasenaChange,
        onToggleMostrarContrasena = viewModel::onToggleMostrarContrasena,
        onLoginClick = { viewModel.onLoginClick(onLoginExitoso) },
        onCrearCuentaClick = onCrearCuentaClick,
        modifier = modifier,
    )
}

@Composable
private fun LoginContent(
    uiState: LoginUiState,
    onUsuarioChange: (String) -> Unit,
    onContrasenaChange: (String) -> Unit,
    onToggleMostrarContrasena: () -> Unit,
    onLoginClick: () -> Unit,
    onCrearCuentaClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(IenCream),
    ) {
        AppHeader()

        // Todo el contenido (título + campos + botón + link + footer) vive en un único
        // Column scrolleable: en pantallas chicas nada queda tapado ni recortado, y con
        // `ScrollState(0)` (no `rememberScrollState`) siempre arranca desde arriba.
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(remember { ScrollState(0) })
                .imePadding()
                .padding(horizontal = 24.dp),
        ) {
            Spacer(Modifier.height(28.dp))

            Text(
                text = "Bienvenido/a",
                color = IenPurple,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Ingresá tus datos para iniciar sesión",
                color = IenGreyText,
                style = MaterialTheme.typography.bodyMedium,
            )

            Spacer(Modifier.height(28.dp))

            FieldLabel("Usuario")
            OutlinedTextField(
                value = uiState.usuario,
                onValueChange = onUsuarioChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Filled.Person, contentDescription = null)
                },
                placeholder = { Text("Ingresá tu usuario") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                isError = uiState.error != null,
                shape = RoundedCornerShape(12.dp),
                colors = ienFieldColors(),
            )

            Spacer(Modifier.height(16.dp))

            FieldLabel("Contraseña")
            OutlinedTextField(
                value = uiState.contrasena,
                onValueChange = onContrasenaChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Filled.Lock, contentDescription = null)
                },
                placeholder = { Text("********") },
                trailingIcon = {
                    val icono =
                        if (uiState.mostrarContrasena) Icons.Filled.Visibility
                        else Icons.Filled.VisibilityOff
                    val descripcion =
                        if (uiState.mostrarContrasena) "Ocultar contraseña"
                        else "Mostrar contraseña"
                    IconButton(onClick = onToggleMostrarContrasena) {
                        Icon(icono, contentDescription = descripcion)
                    }
                },
                visualTransformation =
                    if (uiState.mostrarContrasena) VisualTransformation.None
                    else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                isError = uiState.error != null,
                shape = RoundedCornerShape(12.dp),
                colors = ienFieldColors(),
            )

            if (uiState.error != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = uiState.error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Spacer(Modifier.height(28.dp))

            Button(
                onClick = onLoginClick,
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
                    text = "Iniciar Sesión",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                )
            }

            Spacer(Modifier.height(20.dp))

            CrearCuentaRow(onCrearCuentaClick = onCrearCuentaClick)

            Spacer(Modifier.height(28.dp))

            LoginFooter()

            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun FieldLabel(texto: String) {
    Text(
        text = texto,
        color = IenPurple,
        fontWeight = FontWeight.SemiBold,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(bottom = 6.dp),
    )
}

@Composable
private fun CrearCuentaRow(onCrearCuentaClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "¿No tenés cuenta? ",
            color = IenGreyText,
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = "Crear cuenta",
            color = IenPurple,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.clickable(onClick = onCrearCuentaClick),
        )
    }
}

@Composable
private fun LoginFooter() {
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
private fun LoginScreenPreview() {
    PrestamosComputadorasTheme {
        LoginContent(
            uiState = LoginUiState(),
            onUsuarioChange = {},
            onContrasenaChange = {},
            onToggleMostrarContrasena = {},
            onLoginClick = {},
            onCrearCuentaClick = {},
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Login con error")
@Composable
private fun LoginScreenErrorPreview() {
    PrestamosComputadorasTheme {
        LoginContent(
            uiState = LoginUiState(
                usuario = "",
                contrasena = "123",
                error = "Completá el usuario y la contraseña.",
            ),
            onUsuarioChange = {},
            onContrasenaChange = {},
            onToggleMostrarContrasena = {},
            onLoginClick = {},
            onCrearCuentaClick = {},
        )
    }
}
