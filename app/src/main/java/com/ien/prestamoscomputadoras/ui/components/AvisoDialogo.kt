package com.ien.prestamoscomputadoras.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ien.prestamoscomputadoras.ui.theme.IenPurple
import com.ien.prestamoscomputadoras.ui.theme.IenPurpleDark
import com.ien.prestamoscomputadoras.ui.theme.PrestamosComputadorasTheme

/** Gris oscuro del mensaje: se lee bien sobre blanco (más contraste que el gris de subtítulos). */
private val TextoMensaje = Color(0xFF4A4458)

/**
 * Diálogo de aviso de la app: centrado, blanco, esquinas de 24dp, ícono en círculo lavanda
 * (como las tarjetas del Home), título violeta oscuro y un único botón violeta.
 *
 * Solo se cierra con el botón: tocar afuera o "atrás" no lo cierra, así el aviso no se pasa
 * por alto por un toque accidental.
 *
 * Accesibilidad: el diálogo anuncia [titulo] al abrirse (paneTitle), el título se marca como
 * encabezado y el ícono tiene [descripcionIcono] para TalkBack.
 */
@Composable
fun AvisoDialogo(
    titulo: String,
    mensaje: String,
    onAceptar: () -> Unit,
    icono: ImageVector = Icons.Rounded.WarningAmber,
    descripcionIcono: String = "Advertencia",
    textoBoton: String = "Entendido",
) {
    Dialog(
        onDismissRequest = {}, // ni afuera ni "atrás": solo el botón lo cierra
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            shadowElevation = 8.dp,
            modifier = Modifier.semantics { paneTitle = titulo },
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(IenPurple.copy(alpha = 0.10f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icono,
                        contentDescription = descripcionIcono,
                        tint = IenPurple,
                        modifier = Modifier.size(34.dp),
                    )
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = titulo,
                    color = IenPurpleDark,
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { heading() },
                )

                Spacer(Modifier.height(10.dp))

                Text(
                    text = mensaje,
                    color = TextoMensaje,
                    fontSize = 15.sp,
                    lineHeight = 21.sp,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = onAceptar,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = IenPurple,
                        contentColor = Color.White,
                    ),
                ) {
                    Text(text = textoBoton, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AvisoDialogoPreview() {
    PrestamosComputadorasTheme {
        AvisoDialogo(
            titulo = "Esta computadora no tiene préstamo",
            mensaje = "PC-07 ya figura como devuelta (última devolución: 30/09/2026 · 10:45 hs). " +
                "Revisá que sea el equipo correcto.",
            onAceptar = {},
        )
    }
}
