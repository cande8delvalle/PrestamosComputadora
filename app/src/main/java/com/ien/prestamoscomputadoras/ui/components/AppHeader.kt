package com.ien.prestamoscomputadoras.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ien.prestamoscomputadoras.R
import com.ien.prestamoscomputadoras.ui.theme.IenPurple
import com.ien.prestamoscomputadoras.ui.theme.IenPurpleDark
import com.ien.prestamoscomputadoras.ui.theme.PrestamosComputadorasTheme

/** Alto fijo y compacto de la franja violeta (sin contar la status bar). */
private val HEADER_HEIGHT = 148.dp

/**
 * Header superior compartido entre pantallas (Login, Register, ...).
 *
 * Fondo con degradado violeta, logo circular del instituto y nombre del establecimiento.
 * Tiene un alto fijo ([HEADER_HEIGHT]) y compacto: así nunca "empuja" ni tapa el contenido
 * de la pantalla, que se ubica siempre debajo. El degradado se pinta también detrás de la
 * status bar; el contenido se centra en el espacio visible.
 */
@Composable
fun AppHeader(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(listOf(IenPurple, IenPurpleDark)),
            )
            .statusBarsPadding()
            .height(HEADER_HEIGHT)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Logo del instituto. La imagen ya trae su propio círculo blanco de fondo,
        // por eso NO se le agrega background ni borde extra detrás (evita el doble círculo).
        Image(
            painter = painterResource(id = R.drawable.logo_ien),
            contentDescription = "Logo del Instituto Educativo Económico Nacional",
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape),
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Instituto Educativo Económico Nacional",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = "UEGP N° 167",
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AppHeaderPreview() {
    PrestamosComputadorasTheme {
        AppHeader()
    }
}
