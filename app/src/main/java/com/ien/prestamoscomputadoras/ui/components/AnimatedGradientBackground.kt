package com.ien.prestamoscomputadoras.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import com.ien.prestamoscomputadoras.ui.theme.IenPurple
import com.ien.prestamoscomputadoras.ui.theme.IenPurpleDark
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Fondo con el degradado violeta de marca ([IenPurple]/[IenPurpleDark]), pero con el ángulo
 * del degradado rotando lenta e infinitamente (un ciclo completo cada [periodMillis] ms).
 * Pensado para ser sutil: rotación lineal continua, sin saltos ni "parpadeo".
 */
fun Modifier.ienAnimatedGradientBackground(periodMillis: Int = 6000): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "ienGradientTransition")
    val angleDegrees by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = periodMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "ienGradientAngle",
    )

    this.drawWithCache {
        val angleRad = Math.toRadians(angleDegrees.toDouble())
        // Radio bien por fuera de la pantalla para que el degradado la cubra por completo
        // sin importar el ángulo actual.
        val radius = hypot(size.width, size.height) / 2f
        val center = Offset(size.width / 2f, size.height / 2f)
        val delta = Offset(
            x = (cos(angleRad) * radius).toFloat(),
            y = (sin(angleRad) * radius).toFloat(),
        )
        val brush = Brush.linearGradient(
            colors = listOf(IenPurpleDark, IenPurple, IenPurpleDark),
            start = center - delta,
            end = center + delta,
        )
        onDrawBehind { drawRect(brush) }
    }
}
