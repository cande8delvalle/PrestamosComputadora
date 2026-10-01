package com.ien.prestamoscomputadoras.ui.screens

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ien.prestamoscomputadoras.ui.components.BackHeader
import com.ien.prestamoscomputadoras.ui.components.ExitoOverlay
import com.ien.prestamoscomputadoras.ui.components.FormError
import com.ien.prestamoscomputadoras.ui.components.FormLabel
import com.ien.prestamoscomputadoras.ui.components.lavenderFieldColors
import com.ien.prestamoscomputadoras.ui.theme.IenCream
import com.ien.prestamoscomputadoras.ui.theme.IenLavender
import com.ien.prestamoscomputadoras.ui.theme.IenPurple
import com.ien.prestamoscomputadoras.ui.theme.IenYellow
import com.ien.prestamoscomputadoras.ui.theme.PrestamosComputadorasTheme
import com.ien.prestamoscomputadoras.viewmodel.ERROR_CODIGO_DUPLICADO
import com.ien.prestamoscomputadoras.viewmodel.RegistrarComputadoraUiState
import com.ien.prestamoscomputadoras.viewmodel.RegistrarComputadoraViewModel

/**
 * Pantalla "Registrar Computadora", a la que se llega desde la acción rápida de Home.
 *
 * Este composable solo conecta el [RegistrarComputadoraViewModel] con la UI; el diseño vive en
 * [RegistrarComputadoraContent] (stateless), que es el que se previsualiza.
 */
@Composable
fun RegistrarComputadoraScreen(
    modifier: Modifier = Modifier,
    viewModel: RegistrarComputadoraViewModel = viewModel(factory = RegistrarComputadoraViewModel.Factory),
    /** Flecha de "volver" del header. */
    onVolverClick: () -> Unit = {},
    /** Botón "Volver al inicio" del modal de éxito (navega a Home). */
    onVolverAlInicioClick: () -> Unit = {},
) {
    RegistrarComputadoraContent(
        uiState = viewModel.uiState,
        onCodigoChange = viewModel::onCodigoChange,
        onModeloChange = viewModel::onModeloChange,
        onRegistrarClick = viewModel::onRegistrarClick,
        onRegistrarOtraClick = viewModel::onRegistrarOtraClick,
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
private fun RegistrarComputadoraContent(
    uiState: RegistrarComputadoraUiState,
    onCodigoChange: (String) -> Unit,
    onModeloChange: (String) -> Unit,
    onRegistrarClick: () -> Unit,
    onRegistrarOtraClick: () -> Unit,
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
            BackHeader(titulo = "Registrar Computadora", onVolverClick = onVolverClick)

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
                    Column {
                        FormLabel("CÓDIGO DE IDENTIFICACIÓN")
                        OutlinedTextField(
                            value = uiState.codigo,
                            onValueChange = onCodigoChange,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !uiState.isLoading,
                            singleLine = true,
                            placeholder = { Text("Ej: PC-11") },
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Characters,
                            ),
                            isError = uiState.errorCodigo != null,
                            shape = RoundedCornerShape(12.dp),
                            colors = lavenderFieldColors(),
                        )
                        FormError(uiState.errorCodigo)
                    }

                    Column {
                        FormLabel("MODELO (OPCIONAL)")
                        OutlinedTextField(
                            value = uiState.modelo,
                            onValueChange = onModeloChange,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !uiState.isLoading,
                            singleLine = true,
                            placeholder = { Text("Ej: ProBook 440 G8") },
                            shape = RoundedCornerShape(12.dp),
                            colors = lavenderFieldColors(),
                        )
                    }
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
                            text = "Registrar Computadora",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }

        if (uiState.mostrarModalExito) {
            ExitoOverlay(
                mensaje = "Computadora Registrada Correctamente",
                textoPrincipal = "Registrar otra computadora",
                onPrincipalClick = onRegistrarOtraClick,
                textoSecundario = "Volver al inicio",
                onSecundarioClick = onVolverAlInicioClick,
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun RegistrarComputadoraScreenPreview() {
    PrestamosComputadorasTheme {
        RegistrarComputadoraContent(
            uiState = RegistrarComputadoraUiState(),
            onCodigoChange = {},
            onModeloChange = {},
            onRegistrarClick = {},
            onRegistrarOtraClick = {},
            onVolverAlInicioClick = {},
            onVolverClick = {},
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Registrar Computadora con error")
@Composable
private fun RegistrarComputadoraErrorPreview() {
    PrestamosComputadorasTheme {
        RegistrarComputadoraContent(
            uiState = RegistrarComputadoraUiState(
                codigo = "PC-11",
                modelo = "ProBook 440 G8",
                errorCodigo = ERROR_CODIGO_DUPLICADO,
            ),
            onCodigoChange = {},
            onModeloChange = {},
            onRegistrarClick = {},
            onRegistrarOtraClick = {},
            onVolverAlInicioClick = {},
            onVolverClick = {},
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Registrar Computadora modal éxito")
@Composable
private fun RegistrarComputadoraModalPreview() {
    PrestamosComputadorasTheme {
        RegistrarComputadoraContent(
            uiState = RegistrarComputadoraUiState(mostrarModalExito = true),
            onCodigoChange = {},
            onModeloChange = {},
            onRegistrarClick = {},
            onRegistrarOtraClick = {},
            onVolverAlInicioClick = {},
            onVolverClick = {},
        )
    }
}
