package com.ien.prestamoscomputadoras.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ien.prestamoscomputadoras.ui.theme.IenCream
import com.ien.prestamoscomputadoras.ui.theme.IenGreyText
import com.ien.prestamoscomputadoras.ui.theme.IenLavender
import com.ien.prestamoscomputadoras.ui.theme.IenPurple
import com.ien.prestamoscomputadoras.ui.theme.IenPurpleDark
import com.ien.prestamoscomputadoras.ui.theme.IenYellow
import com.ien.prestamoscomputadoras.viewmodel.AlertaAbierta
import com.ien.prestamoscomputadoras.viewmodel.RevisionDevolucion
import com.ien.prestamoscomputadoras.viewmodel.TarjetaHistorial
import com.ien.prestamoscomputadoras.viewmodel.TipoAlerta
import kotlinx.coroutines.launch

/** Ámbar de "Observación al entregar" (chip y modal). */
val AmbarObservacion = Color(0xFFB26A00)

/** Rojo de "Daño en la devolución" (chip y modal) y de los checks en FALLA/FALTA. */
val RojoDanio = Color(0xFFC62828)

/** Verde de los checks OK. */
private val VerdeOk = Color(0xFF2E7D32)

/**
 * Hoja deslizante desde abajo con el detalle de una alerta del historial: velo violeta
 * oscuro, hoja crema con barrita arriba, encabezado con ícono, datos del préstamo, el detalle
 * según [AlertaAbierta.tipo] y el botón amarillo "Entendido".
 *
 * También se cierra deslizándola hacia abajo, tocando el velo o con "atrás".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertaPrestamoSheet(
    alerta: AlertaAbierta,
    onCerrar: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val tarjeta = alerta.tarjeta

    ModalBottomSheet(
        onDismissRequest = onCerrar,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = IenCream,
        scrimColor = IenPurpleDark.copy(alpha = 0.6f),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .background(IenGreyText.copy(alpha = 0.35f), RoundedCornerShape(50)),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (alerta.tipo) {
                TipoAlerta.OBSERVACION_ENTREGA -> Encabezado(
                    icono = Icons.Filled.Info,
                    color = AmbarObservacion,
                    titulo = "Observación al entregar",
                    subtitulo = "Estado del equipo registrado al momento del préstamo",
                )
                TipoAlerta.DANIO_DEVOLUCION -> Encabezado(
                    icono = Icons.Filled.Warning,
                    color = RojoDanio,
                    titulo = "Daño en la devolución",
                    subtitulo = "Resultado de la revisión al devolver el equipo",
                )
            }

            DatosPrestamo(tarjeta)

            when (alerta.tipo) {
                TipoAlerta.OBSERVACION_ENTREGA -> tarjeta.observacionEntrega?.let {
                    CuadroTexto(titulo = "Observaciones al entregar", texto = it)
                }
                TipoAlerta.DANIO_DEVOLUCION -> tarjeta.danioDevolucion?.let { revision ->
                    ChecksRevision(revision)
                    revision.observaciones?.let {
                        CuadroTexto(titulo = "Observaciones de la devolución", texto = it)
                    }
                }
            }

            Button(
                onClick = {
                    // Se anima la bajada de la hoja y recién después se cierra en el estado.
                    scope.launch { sheetState.hide() }.invokeOnCompletion { onCerrar() }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = IenYellow,
                    contentColor = IenPurple,
                ),
            ) {
                Text(text = "Entendido", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun Encabezado(icono: ImageVector, color: Color, titulo: String, subtitulo: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(color.copy(alpha = 0.14f), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = icono, contentDescription = null, tint = color, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(text = titulo, color = IenPurple, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(text = subtitulo, color = IenGreyText, fontSize = 12.sp)
        }
    }
}

/** Tarjeta blanca con alumno, computadora y fecha del préstamo. */
@Composable
private fun DatosPrestamo(tarjeta: TarjetaHistorial) {
    TarjetaBlanca {
        FilaDato("Alumno", tarjeta.nombreAlumno)
        HorizontalDivider(color = IenPurple.copy(alpha = 0.08f))
        FilaDato("Computadora", tarjeta.codigoComputadora)
        HorizontalDivider(color = IenPurple.copy(alpha = 0.08f))
        FilaDato("Fecha", "${tarjeta.fechaPrestamo} · ${tarjeta.horaPrestamo} hs")
    }
}

@Composable
private fun FilaDato(etiqueta: String, valor: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = etiqueta, color = IenGreyText, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(text = valor, color = IenPurple, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** Los tres checks de la revisión, cada uno con su estado OK o FALLA/FALTA. */
@Composable
private fun ChecksRevision(revision: RevisionDevolucion) {
    TarjetaBlanca {
        FilaCheck("Enciende correctamente", ok = revision.enciende, textoProblema = "FALLA")
        HorizontalDivider(color = IenPurple.copy(alpha = 0.08f))
        FilaCheck("Pantalla en buen estado", ok = revision.pantallaOk, textoProblema = "FALLA")
        HorizontalDivider(color = IenPurple.copy(alpha = 0.08f))
        FilaCheck("Cargador incluido", ok = revision.cargador, textoProblema = "FALTA")
    }
}

@Composable
private fun FilaCheck(texto: String, ok: Boolean, textoProblema: String) {
    val color = if (ok) VerdeOk else RojoDanio
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (ok) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(text = texto, color = Color(0xFF333333), fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text(
            text = if (ok) "OK" else textoProblema,
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

/** Cuadro lavanda con un texto libre en cursiva. */
@Composable
private fun CuadroTexto(titulo: String, texto: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(IenLavender, RoundedCornerShape(14.dp))
            .padding(16.dp),
    ) {
        Text(text = titulo, color = IenPurple, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(text = texto, color = Color(0xFF333333), fontSize = 14.sp, fontStyle = FontStyle.Italic)
    }
}

@Composable
private fun TarjetaBlanca(contenido: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        contenido()
    }
}
