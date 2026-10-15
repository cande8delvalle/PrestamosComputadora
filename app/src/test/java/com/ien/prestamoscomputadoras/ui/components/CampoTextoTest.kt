package com.ien.prestamoscomputadoras.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.ien.prestamoscomputadoras.ui.theme.IenLavenderDark
import org.junit.Assert.assertTrue
import org.junit.Test

class CampoTextoTest {

    /** Relación de contraste de WCAG entre dos colores (1:1 a 21:1). */
    private fun contraste(a: Color, b: Color): Float {
        val (claro, oscuro) = listOf(a.luminance(), b.luminance()).sortedDescending()
        return (claro + 0.05f) / (oscuro + 0.05f)
    }

    @Test
    fun placeholder_seLeeSobreBlancoYSobreLavanda() {
        assertTrue(contraste(ColorPlaceholder, Color.White) >= 4.5f)
        assertTrue(contraste(ColorPlaceholder, IenLavenderDark) >= 4.5f)
    }

    @Test
    fun textoEscrito_tieneMasContrasteQueElPlaceholder() {
        // Lo escrito tiene que destacarse sobre el ejemplo en los dos fondos.
        listOf(Color.White, IenLavenderDark).forEach { fondo ->
            assertTrue(contraste(ColorTextoEscrito, fondo) > contraste(ColorPlaceholder, fondo))
        }
    }
}
