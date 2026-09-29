package com.ien.prestamoscomputadoras

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ien.prestamoscomputadoras.ui.navigation.AppNavigation
import com.ien.prestamoscomputadoras.ui.theme.PrestamosComputadorasTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PrestamosComputadorasTheme {
                // Cada pantalla maneja sus propios insets, por eso no hay Scaffold.
                AppNavigation()
            }
        }
    }
}
