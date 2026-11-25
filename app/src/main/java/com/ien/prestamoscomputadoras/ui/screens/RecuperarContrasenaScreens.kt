package com.ien.prestamoscomputadoras.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ien.prestamoscomputadoras.ui.components.BackHeader
import com.ien.prestamoscomputadoras.ui.components.CampoTexto
import com.ien.prestamoscomputadoras.ui.components.FondoCampo
import com.ien.prestamoscomputadoras.ui.components.MensajeError
import com.ien.prestamoscomputadoras.ui.theme.IenCream
import com.ien.prestamoscomputadoras.ui.theme.IenGreyText
import com.ien.prestamoscomputadoras.ui.theme.IenLavender
import com.ien.prestamoscomputadoras.ui.theme.IenPurple
import com.ien.prestamoscomputadoras.ui.theme.IenYellow
import com.ien.prestamoscomputadoras.ui.theme.PrestamosComputadorasTheme
import com.ien.prestamoscomputadoras.viewmodel.RecuperarContrasenaUiState
import com.ien.prestamoscomputadoras.viewmodel.RecuperarContrasenaViewModel

/*
 * Las 3 pantallas de "Olvidé mi contraseña". Comparten el mismo RecuperarContrasenaViewModel
 * (lo crea AppNavigation asociado al grafo "recuperar_contrasena").
 */

/** Pantalla 1: "Encuentra tu cuenta". */
@Composable
fun EncontrarCuentaScreen(
    viewModel: RecuperarContrasenaViewModel,
    onCodigoEnviado: () -> Unit,
    onVolverClick: () -> Unit,
) {
    val s = viewModel.uiState
    EncontrarCuentaContent(
        uiState = s,
        onEmailChange = viewModel::onEmailChange,
        onContinuarClick = { viewModel.onContinuarCorreo(onCodigoEnviado) },
        onVolverClick = onVolverClick,
    )
}

/** Pantalla 2: "Revisá tu correo". */
@Composable
fun IngresarCodigoScreen(
    viewModel: RecuperarContrasenaViewModel,
    onCodigoVerificado: () -> Unit,
    onVolverClick: () -> Unit,
) {
    IngresarCodigoContent(
        uiState = viewModel.uiState,
        onCodigoChange = viewModel::onCodigoChange,
        onContinuarClick = { viewModel.onContinuarCodigo(onCodigoVerificado) },
        onReenviarClick = viewModel::onReenviarCodigo,
        onVolverClick = onVolverClick,
    )
}

/** Pantalla 3: "Nueva contraseña". */
@Composable
fun NuevaContrasenaScreen(
    viewModel: RecuperarContrasenaViewModel,
    onTerminado: () -> Unit,
    /** Abandonar el flujo (la sesión de recuperación se descarta antes de llamarlo). */
    onCancelar: () -> Unit,
) {
    val cancelar = {
        viewModel.descartarSesion()
        onCancelar()
    }
    // El código ya se usó: volver a la pantalla anterior no serviría. Atrás sale del flujo.
    BackHandler(onBack = cancelar)
    NuevaContrasenaContent(
        uiState = viewModel.uiState,
        onNuevaChange = viewModel::onNuevaContrasenaChange,
        onRepetirChange = viewModel::onRepetirContrasenaChange,
        onToggleMostrar = viewModel::onToggleMostrarContrasena,
        onGuardarClick = { viewModel.onGuardarContrasena(onTerminado) },
        onVolverClick = cancelar,
    )
}

@Composable
private fun EncontrarCuentaContent(
    uiState: RecuperarContrasenaUiState,
    onEmailChange: (String) -> Unit,
    onContinuarClick: () -> Unit,
    onVolverClick: () -> Unit,
) {
    PasoRecuperacion(
        titulo = "Encuentra tu cuenta",
        subtitulo = "Ingresá el correo con el que te registraste y te enviaremos un código.",
        error = uiState.error,
        textoBoton = "Continuar",
        cargando = uiState.isLoading,
        onBotonClick = onContinuarClick,
        onVolverClick = onVolverClick,
    ) {
        EtiquetaCampo("Correo electrónico")
        CampoTexto(
            value = uiState.email,
            onValueChange = onEmailChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
            placeholder = "Ingresá tu correo electrónico",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            isError = uiState.error != null,
            fondo = FondoCampo.LAVANDA,
        )
    }
}

@Composable
private fun IngresarCodigoContent(
    uiState: RecuperarContrasenaUiState,
    onCodigoChange: (String) -> Unit,
    onContinuarClick: () -> Unit,
    onReenviarClick: () -> Unit,
    onVolverClick: () -> Unit,
) {
    PasoRecuperacion(
        titulo = "Revisá tu correo",
        subtitulo = "Enviamos un código a ${uiState.email.trim()}. Ingresalo para continuar.",
        aviso = uiState.aviso,
        error = uiState.error,
        textoBoton = "Continuar",
        cargando = uiState.isLoading,
        onBotonClick = onContinuarClick,
        onVolverClick = onVolverClick,
        pie = {
            val puedeReenviar = uiState.segundosEspera == 0 && !uiState.isLoading
            Text(
                text = if (uiState.segundosEspera > 0) {
                    "Obtener un código nuevo (${uiState.segundosEspera} s)"
                } else {
                    "Obtener un código nuevo"
                },
                color = if (puedeReenviar) IenPurple else IenGreyText,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = puedeReenviar, onClick = onReenviarClick)
                    .padding(vertical = 12.dp),
            )
        },
    ) {
        EtiquetaCampo("Código de 6 dígitos")
        CampoTexto(
            value = uiState.codigo,
            onValueChange = onCodigoChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Filled.Pin, contentDescription = null) },
            placeholder = "000000",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            isError = uiState.error != null,
            fondo = FondoCampo.LAVANDA,
        )
    }
}

@Composable
private fun NuevaContrasenaContent(
    uiState: RecuperarContrasenaUiState,
    onNuevaChange: (String) -> Unit,
    onRepetirChange: (String) -> Unit,
    onToggleMostrar: () -> Unit,
    onGuardarClick: () -> Unit,
    onVolverClick: () -> Unit,
) {
    PasoRecuperacion(
        titulo = "Nueva contraseña",
        subtitulo = "Elegí una contraseña de al menos 8 caracteres.",
        error = uiState.error,
        textoBoton = "Guardar",
        cargando = uiState.isLoading,
        onBotonClick = onGuardarClick,
        onVolverClick = onVolverClick,
    ) {
        val ojito: @Composable () -> Unit = {
            IconButton(onClick = onToggleMostrar) {
                Icon(
                    imageVector = if (uiState.mostrarContrasena) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                    contentDescription = if (uiState.mostrarContrasena) "Ocultar contraseña" else "Mostrar contraseña",
                )
            }
        }
        val transformacion =
            if (uiState.mostrarContrasena) VisualTransformation.None else PasswordVisualTransformation()

        EtiquetaCampo("Nueva contraseña")
        CampoTexto(
            value = uiState.nuevaContrasena,
            onValueChange = onNuevaChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
            trailingIcon = ojito,
            placeholder = "********",
            visualTransformation = transformacion,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            isError = uiState.error != null,
            fondo = FondoCampo.LAVANDA,
        )
        Spacer(Modifier.height(16.dp))
        EtiquetaCampo("Repetir contraseña")
        CampoTexto(
            value = uiState.repetirContrasena,
            onValueChange = onRepetirChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
            trailingIcon = ojito,
            placeholder = "********",
            visualTransformation = transformacion,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            isError = uiState.error != null,
            fondo = FondoCampo.LAVANDA,
        )
    }
}

/**
 * Estructura común de los 3 pasos: header violeta con flecha, título y subtítulo, tarjeta
 * lavanda con los campos, mensaje de error/aviso, botón amarillo y un pie opcional.
 */
@Composable
private fun PasoRecuperacion(
    titulo: String,
    subtitulo: String,
    error: String?,
    textoBoton: String,
    cargando: Boolean,
    onBotonClick: () -> Unit,
    onVolverClick: () -> Unit,
    aviso: String? = null,
    pie: (@Composable () -> Unit)? = null,
    campos: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IenCream),
    ) {
        BackHeader(titulo = "Recuperar contraseña", onVolverClick = onVolverClick)

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(remember { ScrollState(0) })
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
        ) {
            Spacer(Modifier.height(28.dp))
            Text(text = titulo, color = IenPurple, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(text = subtitulo, color = IenGreyText, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(24.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(IenLavender, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                horizontalAlignment = Alignment.Start,
            ) {
                campos()
            }

            if (aviso != null && error == null) {
                Spacer(Modifier.height(12.dp))
                Text(text = aviso, color = IenPurple, style = MaterialTheme.typography.bodySmall)
            }
            if (error != null) {
                Spacer(Modifier.height(12.dp))
                MensajeError(texto = error)
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onBotonClick,
                enabled = !cargando,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = IenYellow,
                    contentColor = IenPurple,
                    disabledContainerColor = IenYellow.copy(alpha = 0.5f),
                    disabledContentColor = IenPurple.copy(alpha = 0.6f),
                ),
            ) {
                Text(
                    text = if (cargando) "Un momento..." else textoBoton,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                )
            }

            pie?.let {
                Spacer(Modifier.height(8.dp))
                it()
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun EtiquetaCampo(texto: String) {
    Text(
        text = texto,
        color = IenPurple,
        fontWeight = FontWeight.SemiBold,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(bottom = 6.dp),
    )
}

@Preview(showBackground = true, showSystemUi = true, name = "1 Encuentra tu cuenta")
@Composable
private fun EncontrarCuentaPreview() {
    PrestamosComputadorasTheme {
        EncontrarCuentaContent(RecuperarContrasenaUiState(), {}, {}, {})
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "2 Revisá tu correo")
@Composable
private fun IngresarCodigoPreview() {
    PrestamosComputadorasTheme {
        IngresarCodigoContent(
            RecuperarContrasenaUiState(
                email = "ana@ien.edu.ar",
                codigo = "123",
                segundosEspera = 42,
                aviso = "Si el correo está registrado, te enviamos un código.",
            ),
            {}, {}, {}, {},
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "3 Nueva contraseña")
@Composable
private fun NuevaContrasenaPreview() {
    PrestamosComputadorasTheme {
        NuevaContrasenaContent(RecuperarContrasenaUiState(error = "Las contraseñas no coinciden."), {}, {}, {}, {}, {})
    }
}
