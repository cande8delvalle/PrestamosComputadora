package com.ien.prestamoscomputadoras.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ien.prestamoscomputadoras.ui.theme.IenGreyText
import com.ien.prestamoscomputadoras.ui.theme.IenPurple
import com.ien.prestamoscomputadoras.ui.theme.PrestamosComputadorasTheme
import com.ien.prestamoscomputadoras.viewmodel.ActividadReciente
import com.ien.prestamoscomputadoras.viewmodel.EstadoPrestamo

/** Verde de la etiqueta "DEVUELTO". Solo se usa acá, por eso no está en Color.kt. */
private val VerdeDevuelto = Color(0xFF2E7D32)

/**
 * Ítem de la lista "Actividad reciente": ícono de persona, nombre del alumno,
 * código de PC + hora, y una etiqueta (pill) con el estado del préstamo.
 */
@Composable
fun RecentActivityItem(
    actividad: ActividadReciente,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, IenPurple.copy(alpha = 0.12f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(IenPurple.copy(alpha = 0.10f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    tint = IenPurple,
                    modifier = Modifier.size(22.dp),
                )
            }

            Spacer(Modifier.width(12.dp))

            // weight(1f): el nombre se achica/corta antes que empujar la pill fuera de la tarjeta.
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = actividad.nombreAlumno,
                    color = IenPurple,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${actividad.codigoComputadora} · ${actividad.hora}hs",
                    color = IenGreyText,
                    fontSize = 13.sp,
                )
            }

            Spacer(Modifier.width(8.dp))

            EstadoPill(actividad.estado)
        }
    }
}

@Composable
private fun EstadoPill(estado: EstadoPrestamo) {
    val (texto, fondo) = when (estado) {
        EstadoPrestamo.ACTIVO -> "ACTIVO" to IenPurple
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
private fun RecentActivityItemPreview() {
    PrestamosComputadorasTheme {
        Column(Modifier.padding(16.dp)) {
            RecentActivityItem(
                ActividadReciente("Juan Pérez", "PC-7", "08:30", EstadoPrestamo.ACTIVO),
            )
            Spacer(Modifier.height(8.dp))
            RecentActivityItem(
                ActividadReciente("María González", "PC-3", "08:15", EstadoPrestamo.DEVUELTO),
            )
        }
    }
}
