package com.ien.prestamoscomputadoras.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ien.prestamoscomputadoras.R
import com.ien.prestamoscomputadoras.ui.components.ienAnimatedGradientBackground
import com.ien.prestamoscomputadoras.ui.theme.IenPurple
import com.ien.prestamoscomputadoras.ui.theme.IenYellow
import com.ien.prestamoscomputadoras.ui.theme.PrestamosComputadorasTheme

/**
 * Pantalla de inicio (start destination) de la app.
 *
 * A diferencia de Login/Register, acá el fondo violeta con degradado animado
 * ([com.ien.prestamoscomputadoras.ui.components.ienAnimatedGradientBackground]) ocupa TODA la
 * pantalla (no es solo la franja superior [com.ien.prestamoscomputadoras.ui.components.AppHeader]),
 * por eso el logo + nombre del instituto se dibujan inline en vez de reusar ese componente.
 * Colores, tipografías y estilos de botón son los mismos que en el resto de la app
 * (amarillo/ámbar para la acción principal, botón outline claro para la secundaria).
 *
 * Es stateless: solo expone los dos callbacks de navegación.
 */
@Composable
fun WelcomeScreen(
    modifier: Modifier = Modifier,
    onCrearCuentaClick: () -> Unit = {},
    onIniciarSesionClick: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .ienAnimatedGradientBackground()
            .systemBarsPadding()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // El bloque logo + nombre queda entre dos Spacer de igual peso, así se centra verticalmente
        // en el espacio sobre "Gestioná tus préstamos". Los pesos suman 1f, como el Spacer único
        // que había antes, para que el título, los botones y la versión no se muevan.
        Spacer(Modifier.weight(0.5f))

        Image(
            painter = painterResource(id = R.drawable.logo_ien),
            contentDescription = "Logo del Instituto Educativo Económico Nacional",
            modifier = Modifier
                .size(116.dp)
                .clip(CircleShape),
        )

        Spacer(Modifier.height(20.dp))

        Text(
            text = "Instituto Educativo Económico Nacional",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "UEGP N° 167",
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.weight(0.5f))
        Spacer(Modifier.height(10.dp))

        Text(
            text = "Gestioná tus préstamos",
            color = Color.White,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Pedí y devolvé computadoras del instituto de forma simple y rápida",
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 15.sp,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.weight(0.2f))

        Button(
            onClick = onCrearCuentaClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = IenYellow,
                contentColor = IenPurple,
            ),
        ) {
            Text(
                text = "Crear Cuenta",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
            )
        }

        Spacer(Modifier.height(14.dp))

        OutlinedButton(
            onClick = onIniciarSesionClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.5.dp, Color.White),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
        ) {
            Text(
                text = "Iniciar sesión",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
            )
        }

        Spacer(Modifier.height(28.dp))

        Text(
            text = "Versión 1.0  ·  IEN 2026",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(8.dp))
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun WelcomeScreenPreview() {
    PrestamosComputadorasTheme {
        WelcomeScreen()
    }
}
