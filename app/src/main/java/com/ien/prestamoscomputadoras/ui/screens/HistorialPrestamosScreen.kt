package com.ien.prestamoscomputadoras.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ien.prestamoscomputadoras.ui.components.AlertaPrestamoSheet
import com.ien.prestamoscomputadoras.ui.components.AmbarObservacion
import com.ien.prestamoscomputadoras.ui.components.BackHeader
import com.ien.prestamoscomputadoras.ui.components.RojoDanio
import com.ien.prestamoscomputadoras.ui.theme.IenCream
import com.ien.prestamoscomputadoras.ui.theme.IenGreyText
import com.ien.prestamoscomputadoras.ui.theme.IenPurple
import com.ien.prestamoscomputadoras.ui.theme.IenPurpleDark
import com.ien.prestamoscomputadoras.ui.theme.PrestamosComputadorasTheme
import com.ien.prestamoscomputadoras.viewmodel.EstadoPrestamo
import com.ien.prestamoscomputadoras.viewmodel.FiltroHistorial
import com.ien.prestamoscomputadoras.viewmodel.HistorialPrestamosUiState
import com.ien.prestamoscomputadoras.viewmodel.HistorialPrestamosViewModel
import com.ien.prestamoscomputadoras.viewmodel.RevisionDevolucion
import com.ien.prestamoscomputadoras.viewmodel.TarjetaHistorial
import com.ien.prestamoscomputadoras.viewmodel.TipoAlerta

/** Verde de la etiqueta "DEVUELTO" (el mismo que en la actividad reciente de Home). */
private val VerdeDevuelto = Color(0xFF2E7D32)

/**
 * Pantalla "Historial de Préstamos", a la que se llega desde "Historial semanal" de Home.
 *
 * Este composable solo conecta el [HistorialPrestamosViewModel] con la UI; el diseño vive en
 * [HistorialPrestamosContent] (stateless), que es el que se previsualiza.
 */
@Composable
fun HistorialPrestamosScreen(
    modifier: Modifier = Modifier,
    viewModel: HistorialPrestamosViewModel = viewModel(factory = HistorialPrestamosViewModel.Factory),
    /** Flecha de "volver" del header. */
    onVolverClick: () -> Unit = {},
) {
    HistorialPrestamosContent(
        uiState = viewModel.uiState,
        onFiltroSeleccionado = viewModel::onFiltroSeleccionado,
        onAlertaClick = viewModel::onAlertaClick,
        onVolverClick = onVolverClick,
        modifier = modifier,
    )

    viewModel.uiState.alertaAbierta?.let { alerta ->
        AlertaPrestamoSheet(alerta = alerta, onCerrar = viewModel::onCerrarAlerta)
    }
}

@Composable
private fun HistorialPrestamosContent(
    uiState: HistorialPrestamosUiState,
    onFiltroSeleccionado: (FiltroHistorial) -> Unit,
    onAlertaClick: (TarjetaHistorial, TipoAlerta) -> Unit,
    onVolverClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tarjetas = uiState.tarjetas

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(IenCream),
    ) {
        BackHeader(titulo = "Historial de Préstamos", onVolverClick = onVolverClick)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FiltroChip(
                texto = "Hoy",
                seleccionado = uiState.filtro == FiltroHistorial.HOY,
                onClick = { onFiltroSeleccionado(FiltroHistorial.HOY) },
            )
            Spacer(Modifier.width(8.dp))
            FiltroChip(
                texto = "Esta semana",
                seleccionado = uiState.filtro == FiltroHistorial.ESTA_SEMANA,
                onClick = { onFiltroSeleccionado(FiltroHistorial.ESTA_SEMANA) },
            )
            Spacer(Modifier.weight(1f))
            if (!uiState.cargando) {
                Text(
                    text = "${tarjetas.size} Registros",
                    color = IenGreyText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        when {
            uiState.cargando -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = IenPurple)
            }

            tarjetas.isEmpty() -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = when (uiState.filtro) {
                        FiltroHistorial.HOY -> "No hay préstamos registrados hoy."
                        FiltroHistorial.ESTA_SEMANA -> "No hay préstamos registrados esta semana."
                    },
                    color = IenGreyText,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                )
            }

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(tarjetas, key = { it.idPrestamo }) { tarjeta ->
                    TarjetaPrestamo(tarjeta, onAlertaClick = { tipo -> onAlertaClick(tarjeta, tipo) })
                }
            }
        }
    }
}

/** Chip tipo tab: violeta sólido si está seleccionado, blanco con borde si no. */
@Composable
private fun FiltroChip(texto: String, seleccionado: Boolean, onClick: () -> Unit) {
    val forma = RoundedCornerShape(50)
    Text(
        text = texto,
        color = if (seleccionado) Color.White else IenPurple,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(forma)
            .background(if (seleccionado) IenPurple else Color.White, forma)
            .border(1.dp, IenPurple, forma)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TarjetaPrestamo(tarjeta: TarjetaHistorial, onAlertaClick: (TipoAlerta) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, IenPurple.copy(alpha = 0.12f)),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(IenPurple.copy(alpha = 0.10f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = null,
                        tint = IenPurple,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.width(10.dp))
                // weight(1f): el nombre se corta antes que empujar el badge fuera de la tarjeta.
                Text(
                    text = tarjeta.nombreAlumno,
                    color = IenPurple,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                EstadoBadge(tarjeta.estado)
            }

            Spacer(Modifier.height(10.dp))

            DatoTarjeta("DNI: ${tarjeta.dniAlumno}")
            DatoTarjeta("${tarjeta.codigoComputadora} · ${tarjeta.fechaPrestamo}")
            DatoTarjeta(
                if (tarjeta.horaDevolucion != null) {
                    "↓${tarjeta.horaPrestamo} hs   ↑${tarjeta.horaDevolucion} hs"
                } else {
                    "↓${tarjeta.horaPrestamo} hs"
                },
            )

            if (tarjeta.observacionEntrega != null || tarjeta.danioDevolucion != null) {
                Spacer(Modifier.height(8.dp))
                // FlowRow: en pantallas angostas el segundo chip baja de renglón.
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (tarjeta.observacionEntrega != null) {
                        AlertaChip(
                            texto = "Observación al entregar",
                            icono = Icons.Filled.Info,
                            color = AmbarObservacion,
                            onClick = { onAlertaClick(TipoAlerta.OBSERVACION_ENTREGA) },
                        )
                    }
                    if (tarjeta.danioDevolucion != null) {
                        AlertaChip(
                            texto = "Daño en la devolución",
                            icono = Icons.Filled.Warning,
                            color = RojoDanio,
                            onClick = { onAlertaClick(TipoAlerta.DANIO_DEVOLUCION) },
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = IenPurple.copy(alpha = 0.08f))
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Prestado por: ${tarjeta.prestadoPor}",
                color = IenGreyText,
                fontSize = 11.sp,
            )
            // Solo si ya se devolvió: mientras está activo no hay quién la haya recibido.
            if (tarjeta.devueltoPor != null) {
                Text(
                    text = "Devuelto por: ${tarjeta.devueltoPor}",
                    color = IenGreyText,
                    fontSize = 11.sp,
                )
            }
        }
    }
}

/** Chip chico y tocable de una alerta: abre su hoja con el detalle. */
@Composable
private fun AlertaChip(texto: String, icono: ImageVector, color: Color, onClick: () -> Unit) {
    val forma = RoundedCornerShape(50)
    Row(
        modifier = Modifier
            .clip(forma)
            .background(color.copy(alpha = 0.10f), forma)
            .border(1.dp, color.copy(alpha = 0.45f), forma)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = icono, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text(text = texto, color = color, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun DatoTarjeta(texto: String) {
    Text(
        text = texto,
        color = Color(0xFF333333),
        fontSize = 13.sp,
        modifier = Modifier.padding(vertical = 1.dp),
    )
}

@Composable
private fun EstadoBadge(estado: EstadoPrestamo) {
    val (texto, fondo) = when (estado) {
        EstadoPrestamo.ACTIVO -> "ACTIVO" to IenPurpleDark
        EstadoPrestamo.DEVUELTO -> "DEVUELTO" to VerdeDevuelto
    }
    Text(
        text = texto,
        color = Color.White,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .background(fondo, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Preview(showBackground = true)
@Composable
private fun HistorialPrestamosPreview() {
    PrestamosComputadorasTheme {
        HistorialPrestamosContent(
            uiState = HistorialPrestamosUiState(
                filtro = FiltroHistorial.ESTA_SEMANA,
                cargando = false,
                tarjetasSemana = listOf(
                    TarjetaHistorial(
                        idPrestamo = 2, nombreAlumno = "Juan Pérez", dniAlumno = "46.111.222",
                        codigoComputadora = "PC-7", fechaPrestamo = "30/09/2026",
                        horaPrestamo = "17:00", horaDevolucion = null,
                        estado = EstadoPrestamo.ACTIVO,
                        observacionEntrega = "Rayón en la tapa", danioDevolucion = null,
                        prestadoPor = "Ana López", devueltoPor = null, fechaPrestamoMs = 2,
                    ),
                    TarjetaHistorial(
                        idPrestamo = 1, nombreAlumno = "María González", dniAlumno = "45.987.654",
                        codigoComputadora = "PC-3", fechaPrestamo = "29/09/2026",
                        horaPrestamo = "09:30", horaDevolucion = "10:45",
                        estado = EstadoPrestamo.DEVUELTO,
                        observacionEntrega = "Rayón en la tapa",
                        danioDevolucion = RevisionDevolucion(
                            enciende = true, pantallaOk = true, cargador = false, observaciones = null,
                        ),
                        prestadoPor = "Carlos Díaz", devueltoPor = "Ana López", fechaPrestamoMs = 1,
                    ),
                ),
            ),
            onFiltroSeleccionado = {},
            onAlertaClick = { _, _ -> },
            onVolverClick = {},
        )
    }
}
