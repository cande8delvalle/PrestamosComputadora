package com.ien.prestamoscomputadoras.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ien.prestamoscomputadoras.ui.theme.IenPurple
import com.ien.prestamoscomputadoras.ui.theme.PrestamosComputadorasTheme

/** Gris violáceo de la etiqueta de la estadística. */
private val StatLabelGrey = Color(0xFF7A6D8A)

/**
 * Tarjeta de estadística: fondo blanco con sombra suave, un ícono de acento en un cuadrado
 * redondeado arriba a la izquierda, el número grande en violeta y la etiqueta debajo.
 */
@Composable
fun StatCard(
    icono: ImageVector,
    colorFondoIcono: Color,
    colorIcono: Color,
    numero: Int,
    etiqueta: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(colorFondoIcono, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icono,
                    contentDescription = null,
                    tint = colorIcono,
                    modifier = Modifier.size(20.dp),
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = numero.toString(),
                color = IenPurple,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = etiqueta,
                color = StatLabelGrey,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F0E8)
@Composable
private fun StatCardPreview() {
    PrestamosComputadorasTheme {
        Row(
            modifier = Modifier.padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatCard(
                icono = Icons.Filled.Laptop,
                colorFondoIcono = Color(0xFFFFF3CC),
                colorIcono = Color(0xFFB8860B),
                numero = 2,
                etiqueta = "Prestados",
                modifier = Modifier.weight(1f),
            )
            StatCard(
                icono = Icons.Filled.Check,
                colorFondoIcono = Color(0xFFE3F4E8),
                colorIcono = Color(0xFF2E7D32),
                numero = 4,
                etiqueta = "Devueltos hoy",
                modifier = Modifier.weight(1f),
            )
        }
    }
}
