package com.ien.prestamoscomputadoras.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ien.prestamoscomputadoras.viewmodel.EventoHora

/**
 * Una hora con su flecha: hacia ARRIBA si es la hora de la entrega (cuando se dio de alta el
 * préstamo) y hacia ABAJO si es la de la devolución. La flecha tiene contentDescription
 * ("Entrega" / "Devolución") para que TalkBack la lea.
 *
 * Con [conEtiqueta] se lee "↑ Entrega 23:49hs"; sin ella, solo "↑ 23:49hs".
 */
@Composable
fun HoraConFlecha(
    hora: String,
    evento: EventoHora,
    modifier: Modifier = Modifier,
    conEtiqueta: Boolean = false,
    color: Color = Color(0xFF333333),
    fontSize: TextUnit = 13.sp,
    fontWeight: FontWeight = FontWeight.Normal,
) {
    val (icono, nombre) = when (evento) {
        EventoHora.ENTREGA -> Icons.Rounded.ArrowUpward to "Entrega"
        EventoHora.DEVOLUCION -> Icons.Rounded.ArrowDownward to "Devolución"
    }
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icono,
            // Si ya está la etiqueta escrita, la flecha es decorativa (no se lee dos veces).
            contentDescription = if (conEtiqueta) null else nombre,
            tint = color,
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(3.dp))
        Text(
            text = if (conEtiqueta) "$nombre ${hora}hs" else "${hora}hs",
            color = color,
            fontSize = fontSize,
            fontWeight = fontWeight,
        )
    }
}
