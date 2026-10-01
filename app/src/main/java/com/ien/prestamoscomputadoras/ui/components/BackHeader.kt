package com.ien.prestamoscomputadoras.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ien.prestamoscomputadoras.ui.theme.IenPurple
import com.ien.prestamoscomputadoras.ui.theme.IenPurpleDark
import com.ien.prestamoscomputadoras.ui.theme.PrestamosComputadorasTheme

/**
 * Header de las pantallas secundarias (las que se abren desde Home): degradado violeta,
 * flecha de "volver" a la izquierda y el título en blanco bold. El degradado se pinta
 * también detrás de la status bar.
 */
@Composable
fun BackHeader(
    titulo: String,
    onVolverClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(IenPurple, IenPurpleDark)))
            .statusBarsPadding()
            .height(64.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onVolverClick) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Volver",
                tint = Color.White,
            )
        }
        Spacer(Modifier.width(4.dp))
        Text(
            text = titulo,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BackHeaderPreview() {
    PrestamosComputadorasTheme {
        BackHeader(titulo = "Registrar Alumno", onVolverClick = {})
    }
}
