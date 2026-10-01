package com.ien.prestamoscomputadoras.ui.screens

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ien.prestamoscomputadoras.data.entity.Alumno
import com.ien.prestamoscomputadoras.data.entity.Computadora
import com.ien.prestamoscomputadoras.ui.components.BackHeader
import com.ien.prestamoscomputadoras.ui.components.ExitoOverlay
import com.ien.prestamoscomputadoras.ui.components.FormError
import com.ien.prestamoscomputadoras.ui.components.FormErrorRed
import com.ien.prestamoscomputadoras.ui.components.FormLabel
import com.ien.prestamoscomputadoras.ui.components.lavenderFieldColors
import com.ien.prestamoscomputadoras.ui.theme.IenCream
import com.ien.prestamoscomputadoras.ui.theme.IenGreyText
import com.ien.prestamoscomputadoras.ui.theme.IenLavender
import com.ien.prestamoscomputadoras.ui.theme.IenPurple
import com.ien.prestamoscomputadoras.ui.theme.IenYellow
import com.ien.prestamoscomputadoras.ui.theme.PrestamosComputadorasTheme
import com.ien.prestamoscomputadoras.viewmodel.ERROR_SELECCIONAR_ALUMNO
import com.ien.prestamoscomputadoras.viewmodel.ERROR_SELECCIONAR_COMPUTADORA
import com.ien.prestamoscomputadoras.viewmodel.RegistrarPrestamoUiState
import com.ien.prestamoscomputadoras.viewmodel.RegistrarPrestamoViewModel
import com.ien.prestamoscomputadoras.viewmodel.formatearDni

/**
 * Pantalla "Nuevo Préstamo", a la que se llega desde la acción rápida de Home.
 *
 * Este composable solo conecta el [RegistrarPrestamoViewModel] con la UI; el diseño vive en
 * [RegistrarPrestamoContent] (stateless), que es el que se previsualiza.
 */
@Composable
fun RegistrarPrestamoScreen(
    modifier: Modifier = Modifier,
    viewModel: RegistrarPrestamoViewModel = viewModel(factory = RegistrarPrestamoViewModel.Factory),
    /** Flecha de "volver" del header. */
    onVolverClick: () -> Unit = {},
    /** Botón "Volver al inicio" del modal de éxito (navega a Home). */
    onVolverAlInicioClick: () -> Unit = {},
) {
    RegistrarPrestamoContent(
        uiState = viewModel.uiState,
        onAlumnoSeleccionado = viewModel::onAlumnoSeleccionado,
        onComputadoraSeleccionada = viewModel::onComputadoraSeleccionada,
        onObservacionesChange = viewModel::onObservacionesChange,
        onRegistrarClick = viewModel::onRegistrarClick,
        onRealizarOtroClick = viewModel::onRealizarOtroClick,
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
private fun RegistrarPrestamoContent(
    uiState: RegistrarPrestamoUiState,
    onAlumnoSeleccionado: (Alumno) -> Unit,
    onComputadoraSeleccionada: (Computadora) -> Unit,
    onObservacionesChange: (String) -> Unit,
    onRegistrarClick: () -> Unit,
    onRealizarOtroClick: () -> Unit,
    onVolverAlInicioClick: () -> Unit,
    onVolverClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val botonHabilitado =
        !uiState.isLoading && !uiState.cargandoListas && !uiState.sinComputadorasDisponibles

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(IenCream),
        ) {
            BackHeader(titulo = "Nuevo Préstamo", onVolverClick = onVolverClick)

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
                        FormLabel("ALUMNO")
                        if (!uiState.cargandoListas && uiState.alumnos.isEmpty()) {
                            MensajeListaVacia("No hay alumnos registrados todavía")
                        } else {
                            Selector(
                                opciones = uiState.alumnos,
                                seleccionado = uiState.alumnoSeleccionado,
                                textoSeleccionado = { "${it.nombre} ${it.apellido}" },
                                placeholder = "Seleccioná un alumno",
                                error = uiState.errorAlumno,
                                enabled = !uiState.isLoading,
                                onSeleccionar = onAlumnoSeleccionado,
                            ) { alumno ->
                                ItemSelector(
                                    icono = Icons.Filled.Person,
                                    titulo = "${alumno.nombre} ${alumno.apellido}",
                                    subtitulo = "DNI: ${formatearDni(alumno.dni)}",
                                )
                            }
                        }
                        FormError(uiState.errorAlumno)
                    }

                    Column {
                        FormLabel("IDENTIFICADOR DE EQUIPO")
                        if (uiState.sinComputadorasDisponibles) {
                            MensajeListaVacia("No hay computadoras disponibles en este momento")
                        } else {
                            Selector(
                                opciones = uiState.computadorasDisponibles,
                                seleccionado = uiState.computadoraSeleccionada,
                                textoSeleccionado = { it.codigo },
                                placeholder = "Seleccioná una computadora",
                                error = uiState.errorComputadora,
                                enabled = !uiState.isLoading,
                                onSeleccionar = onComputadoraSeleccionada,
                            ) { computadora ->
                                ItemSelector(icono = Icons.Filled.Laptop, titulo = computadora.codigo)
                            }
                        }
                        FormError(uiState.errorComputadora)
                    }

                    Column {
                        FormLabel("ESTADO INICIAL (OPCIONAL)")
                        OutlinedTextField(
                            value = uiState.observacionesIniciales,
                            onValueChange = onObservacionesChange,
                            enabled = !uiState.isLoading,
                            minLines = 3,
                            placeholder = { Text("Observaciones sobre el estado del equipo...") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = lavenderFieldColors(),
                        )
                    }
                }

                if (uiState.errorGeneral != null) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = uiState.errorGeneral,
                        color = FormErrorRed,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = onRegistrarClick,
                    enabled = botonHabilitado,
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
                            text = "Registrar Préstamo",
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
                mensaje = "Préstamo Registrado Correctamente",
                textoPrincipal = "Realizar otro Préstamo",
                onPrincipalClick = onRealizarOtroClick,
                textoSecundario = "Volver al inicio",
                onSecundarioClick = onVolverAlInicioClick,
            )
        }
    }
}

/**
 * Selector desplegable de solo lectura: el campo muestra [textoSeleccionado] del elegido y,
 * al tocarlo, se abre la lista de [opciones], cada una dibujada con [item].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> Selector(
    opciones: List<T>,
    seleccionado: T?,
    textoSeleccionado: (T) -> String,
    placeholder: String,
    error: String?,
    enabled: Boolean,
    onSeleccionar: (T) -> Unit,
    item: @Composable (T) -> Unit,
) {
    var expandido by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expandido,
        onExpandedChange = { if (enabled) expandido = it },
    ) {
        OutlinedTextField(
            value = seleccionado?.let(textoSeleccionado).orEmpty(),
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            singleLine = true,
            placeholder = { Text(placeholder) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandido) },
            isError = error != null,
            shape = RoundedCornerShape(12.dp),
            colors = lavenderFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled),
        )
        ExposedDropdownMenu(
            expanded = expandido,
            onDismissRequest = { expandido = false },
        ) {
            opciones.forEach { opcion ->
                DropdownMenuItem(
                    text = { item(opcion) },
                    onClick = {
                        onSeleccionar(opcion)
                        expandido = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}

/** Contenido de una opción: ícono violeta, título violeta bold y subtítulo gris opcional. */
@Composable
private fun ItemSelector(icono: ImageVector, titulo: String, subtitulo: String? = null) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icono, contentDescription = null, tint = IenPurple, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(text = titulo, color = IenPurple, fontWeight = FontWeight.Bold)
            if (subtitulo != null) {
                Text(text = subtitulo, color = IenGreyText, fontSize = 13.sp)
            }
        }
    }
}

/** Texto que reemplaza al selector cuando no hay opciones para elegir. */
@Composable
private fun MensajeListaVacia(texto: String) {
    Text(
        text = texto,
        color = IenGreyText,
        fontSize = 14.sp,
        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
    )
}

private val alumnosDePrueba = listOf(
    Alumno(idAlumno = 1, nombre = "María", apellido = "Gonzales", dni = "46111222"),
    Alumno(idAlumno = 2, nombre = "Juan", apellido = "Pérez", dni = "45799888"),
)
private val computadorasDePrueba = listOf(
    Computadora(idComputadora = 7, codigo = "PC-07"),
    Computadora(idComputadora = 12, codigo = "PC-12"),
)

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun RegistrarPrestamoScreenPreview() {
    PrestamosComputadorasTheme {
        RegistrarPrestamoContent(
            uiState = RegistrarPrestamoUiState(
                alumnos = alumnosDePrueba,
                computadorasDisponibles = computadorasDePrueba,
                cargandoListas = false,
                alumnoSeleccionado = alumnosDePrueba.first(),
            ),
            onAlumnoSeleccionado = {},
            onComputadoraSeleccionada = {},
            onObservacionesChange = {},
            onRegistrarClick = {},
            onRealizarOtroClick = {},
            onVolverAlInicioClick = {},
            onVolverClick = {},
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Nuevo Préstamo con errores / sin equipos")
@Composable
private fun RegistrarPrestamoErroresPreview() {
    PrestamosComputadorasTheme {
        RegistrarPrestamoContent(
            uiState = RegistrarPrestamoUiState(
                alumnos = alumnosDePrueba,
                computadorasDisponibles = emptyList(),
                cargandoListas = false,
                errorAlumno = ERROR_SELECCIONAR_ALUMNO,
                errorComputadora = ERROR_SELECCIONAR_COMPUTADORA,
            ),
            onAlumnoSeleccionado = {},
            onComputadoraSeleccionada = {},
            onObservacionesChange = {},
            onRegistrarClick = {},
            onRealizarOtroClick = {},
            onVolverAlInicioClick = {},
            onVolverClick = {},
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Nuevo Préstamo modal éxito")
@Composable
private fun RegistrarPrestamoModalPreview() {
    PrestamosComputadorasTheme {
        RegistrarPrestamoContent(
            uiState = RegistrarPrestamoUiState(cargandoListas = false, mostrarModalExito = true),
            onAlumnoSeleccionado = {},
            onComputadoraSeleccionada = {},
            onObservacionesChange = {},
            onRegistrarClick = {},
            onRealizarOtroClick = {},
            onVolverAlInicioClick = {},
            onVolverClick = {},
        )
    }
}
