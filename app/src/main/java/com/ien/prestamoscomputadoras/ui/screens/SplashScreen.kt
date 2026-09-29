package com.ien.prestamoscomputadoras.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ien.prestamoscomputadoras.R
import com.ien.prestamoscomputadoras.ui.components.ienAnimatedGradientBackground
import com.ien.prestamoscomputadoras.ui.theme.PrestamosComputadorasTheme
import kotlinx.coroutines.delay

/** Duración total del splash antes de navegar a Welcome. */
private const val SPLASH_DURATION_MS = 2600L

/** Cada cuánto rota el ícono destacado debajo del logo. */
private const val ICON_ROTATION_MS = 1100L

private val splashIcons: List<ImageVector> = listOf(
    Icons.Filled.Laptop,
    Icons.Filled.CheckCircle,
    Icons.Filled.History,
)

/**
 * Pantalla de splash: se muestra unos segundos al abrir la app, antes de [WelcomeScreen].
 * Fondo con el degradado de marca animado ([ienAnimatedGradientBackground]), logo circular
 * del instituto, un ícono destacado que va rotando por cross-fade y partículas decorativas
 * sutiles alrededor. Es stateless salvo por su propio timer interno.
 */
@Composable
fun SplashScreen(
    modifier: Modifier = Modifier,
    onSplashFinished: () -> Unit = {},
) {
    LaunchedEffect(Unit) {
        delay(SPLASH_DURATION_MS)
        onSplashFinished()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .ienAnimatedGradientBackground(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id = R.drawable.logo_ien),
                contentDescription = "Logo del Instituto Educativo Económico Nacional",
                modifier = Modifier
                    .size(116.dp)
                    .clip(CircleShape),
            )

            Spacer(Modifier.height(28.dp))

            Box(contentAlignment = Alignment.Center) {
                SplashParticles()
                RotatingIcon()
            }
        }
    }
}

/** Ícono destacado debajo del logo, cambia por cross-fade cada [ICON_ROTATION_MS]. */
@Composable
private fun RotatingIcon() {
    var iconIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(ICON_ROTATION_MS)
            iconIndex = (iconIndex + 1) % splashIcons.size
        }
    }

    Crossfade(
        targetState = iconIndex,
        animationSpec = tween(durationMillis = 450),
        label = "splashIconCrossfade",
    ) { index ->
        Icon(
            imageVector = splashIcons[index],
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(32.dp),
        )
    }
}

/**
 * Puntitos decorativos alrededor del ícono central, cada uno con su propio ciclo de
 * fade in/out asincrónico (distinta duración y demora) para dar sensación de movimiento
 * sutil sin recargar la pantalla.
 */
@Composable
private fun SplashParticles() {
    Particle(offsetX = (-40).dp, offsetY = (-26).dp, size = 7.dp, periodMillis = 2200, delayMillis = 0)
    Particle(offsetX = 46.dp, offsetY = (-10).dp, size = 5.dp, periodMillis = 2800, delayMillis = 400)
    Particle(offsetX = (-24).dp, offsetY = 30.dp, size = 6.dp, periodMillis = 2500, delayMillis = 900)
}

@Composable
private fun Particle(
    offsetX: androidx.compose.ui.unit.Dp,
    offsetY: androidx.compose.ui.unit.Dp,
    size: androidx.compose.ui.unit.Dp,
    periodMillis: Int,
    delayMillis: Int,
) {
    val transition = rememberInfiniteTransition(label = "particleTransition")
    val alpha by transition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = periodMillis, delayMillis = delayMillis, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "particleAlpha",
    )

    Box(
        modifier = Modifier
            .offset(x = offsetX, y = offsetY)
            .size(size)
            .background(Color.White.copy(alpha = alpha), CircleShape),
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun SplashScreenPreview() {
    PrestamosComputadorasTheme {
        SplashScreen()
    }
}
