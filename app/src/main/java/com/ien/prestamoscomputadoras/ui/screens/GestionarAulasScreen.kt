package com.ien.prestamoscomputadoras.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ien.prestamoscomputadoras.data.model.Aula
import com.ien.prestamoscomputadoras.ui.components.BackHeader
import com.ien.prestamoscomputadoras.ui.components.CampoTexto
import com.ien.prestamoscomputadoras.ui.components.FormError
import com.ien.prestamoscomputadoras.ui.components.FormLabel
import com.ien.prestamoscomputadoras.ui.components.MensajeError
import com.ien.prestamoscomputadoras.ui.theme.IenCream
import com.ien.prestamoscomputadoras.ui.theme.IenGreyText
import com.ien.prestamoscomputadoras.ui.theme.IenLavender
import com.ien.prestamoscomputadoras.ui.theme.IenPurple
import com.ien.prestamoscomputadoras.ui.theme.IenYellow
import com.ien.prestamoscomputadoras.ui.theme.PrestamosComputadorasTheme
import com.ien.prestamoscomputadoras.viewmodel.GestionarAulasUiState
import com.ien.prestamoscomputadoras.viewmodel.GestionarAulasViewModel

/**
 * Pantalla "Gestionar Aulas", exclusiva del Admin (sección Administración de Home): lista de
 * aulas, alta con solo el nombre y eliminar.
 *
 * Este composable solo conecta el [GestionarAulasViewModel] con la UI; el diseño vive en
 * [GestionarAulasContent] (stateless), que es el que se previsualiza.
 */
@Composable
fun GestionarAulasScreen(
    modifier: Modifier = Modifier,
    viewModel: GestionarAulasViewModel = viewModel(factory = GestionarAulasViewModel.Factory),
    /** Flecha de "volver" del header. */
    onVolverClick: () -> Unit = {},
) {
    GestionarAulasContent(
        uiState = viewModel.uiState,
        onNombreChange = viewModel::onNombreChange,
        onAgregarClick = viewModel::onAgregarClick,
        onEliminarClick = viewModel::onEliminarClick,
        onConfirmarEliminar = viewModel::onConfirmarEliminar,
        onCancelarEliminar = viewModel::onCancelarEliminar,
        onReintentar = viewModel::cargar,
        onVolverClick = onVolverClick,
        modifier = modifier,
    )
}

@Composable
private fun GestionarAulasContent(
    uiState: GestionarAulasUiState,
    onNombreChange: (String) -> Unit,
    onAgregarClick: () -> Unit,
    onEliminarClick: (Aula) -> Unit,
    onConfirmarEliminar: () -> Unit,
    onCancelarEliminar: () -> Unit,
    onReintentar: () -> Unit = {},
    onVolverClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(IenCream),
    ) {
        BackHeader(titulo = "Gestionar Aulas", onVolverClick = onVolverClick)

        when {
            uiState.cargando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = IenPurple)
            }

            !uiState.puedeGestionar -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (uiState.error != null) {
                    MensajeError(texto = uiState.error, onReintentar = onReintentar)
                } else {
                    Text(
                        text = "Solo el Admin puede gestionar las aulas.",
                        color = IenGreyText,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            else -> LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .imePadding(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item { FormularioAlta(uiState, onNombreChange, onAgregarClick) }

                uiState.error?.let { error ->
                    item {
                        MensajeError(
                            texto = error,
                            onReintentar = if (uiState.errorDeCarga) onReintentar else null,
                        )
                    }
                }

                item {
                    Text(
                        text = "AULAS REGISTRADAS (${uiState.aulas.size})",
                        color = IenPurple,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }

                if (uiState.aulas.isEmpty()) {
                    item {
                        Text(
                            text = "Todavía no hay aulas registradas.",
                            color = IenGreyText,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                        )
                    }
                } else {
                    items(uiState.aulas, key = { it.idAula }) { aula ->
                        FilaAula(aula = aula, onEliminarClick = { onEliminarClick(aula) })
                    }
                }

                item { Spacer(Modifier.navigationBarsPadding()) }
            }
        }
    }

    uiState.aulaPorEliminar?.let { aula ->
        AlertDialog(
            onDismissRequest = onCancelarEliminar,
            title = { Text("Eliminar aula", color = IenPurple, fontWeight = FontWeight.Bold) },
            text = { Text("¿Eliminar \"${aula.nombre}\"? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = onConfirmarEliminar) { Text("Eliminar", color = IenPurple) }
            },
            dismissButton = {
                TextButton(onClick = onCancelarEliminar) { Text("Cancelar", color = IenGreyText) }
            },
        )
    }
}

/** Recuadro lavanda con el campo "Nombre del aula" y el botón "Agregar aula". */
@Composable
private fun FormularioAlta(
    uiState: GestionarAulasUiState,
    onNombreChange: (String) -> Unit,
    onAgregarClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(IenLavender, RoundedCornerShape(16.dp))
            .padding(horizontal = 18.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column {
            FormLabel("NOMBRE DEL AULA")
            CampoTexto(
                value = uiState.nombre,
                onValueChange = onNombreChange,
                enabled = !uiState.guardando,
                singleLine = true,
                placeholder = "Ej: Aula 3",
                isError = uiState.errorNombre != null,
                modifier = Modifier.fillMaxWidth(),
            )
            FormError(uiState.errorNombre)
        }

        Button(
            onClick = onAgregarClick,
            enabled = !uiState.guardando,
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
            if (uiState.guardando) {
                CircularProgressIndicator(color = IenPurple, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
            } else {
                Text(text = "Agregar aula", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun FilaAula(aula: Aula, onEliminarClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(14.dp))
            .padding(start = 16.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.MeetingRoom, contentDescription = null, tint = IenPurple, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Text(
            text = aula.nombre,
            color = IenPurple,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onEliminarClick) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = "Eliminar ${aula.nombre}",
                tint = IenGreyText,
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun GestionarAulasPreview() {
    PrestamosComputadorasTheme {
        GestionarAulasContent(
            uiState = GestionarAulasUiState(
                cargando = false,
                puedeGestionar = true,
                aulas = listOf(Aula(1, "Aula 1"), Aula(2, "Laboratorio")),
                nombre = "Aula 3",
            ),
            onNombreChange = {},
            onAgregarClick = {},
            onEliminarClick = {},
            onConfirmarEliminar = {},
            onCancelarEliminar = {},
            onVolverClick = {},
        )
    }
}
