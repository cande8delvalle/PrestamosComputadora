package com.ien.prestamoscomputadoras.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ien.prestamoscomputadoras.data.Permiso
import com.ien.prestamoscomputadoras.ui.components.QuickActionCard
import com.ien.prestamoscomputadoras.ui.components.RecentActivityItem
import com.ien.prestamoscomputadoras.ui.components.StatCard
import com.ien.prestamoscomputadoras.ui.theme.IenCream
import com.ien.prestamoscomputadoras.ui.theme.IenGreyText
import com.ien.prestamoscomputadoras.ui.theme.IenPurple
import com.ien.prestamoscomputadoras.ui.theme.IenPurpleDark
import com.ien.prestamoscomputadoras.ui.theme.PrestamosComputadorasTheme
import com.ien.prestamoscomputadoras.viewmodel.HomeUiState
import com.ien.prestamoscomputadoras.viewmodel.HomeViewModel

/** Acentos de las tarjetas de estadísticas: fondo pálido del ícono + tono más oscuro del ícono. */
private val StatAmarilloPalido = Color(0xFFFFF3CC)
private val StatDorado = Color(0xFFB8860B)
private val StatVerdePalido = Color(0xFFE3F4E8)
private val StatVerde = Color(0xFF2E7D32)

/**
 * Pantalla de inicio, a la que se llega después de un login exitoso.
 *
 * Este composable solo conecta el [HomeViewModel] con la UI: observa su StateFlow (solo
 * mientras la pantalla está visible) y se recompone cada vez que emite. Todo el diseño vive en
 * [HomeContent], que es stateless y por eso se puede previsualizar.
 */
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
    // TODO: navegar a las pantallas correspondientes cuando existan.
    onRegistrarAlumnoClick: () -> Unit = {},
    onRegistrarComputadoraClick: () -> Unit = {},
    onNuevoPrestamoClick: () -> Unit = {},
    onRegistrarDevolucionClick: () -> Unit = {},
    onHistorialClick: () -> Unit = {},
    onRolesPermisosClick: () -> Unit = {},
    // TODO: cerrar sesión y volver a Login/Welcome.
    onLogoutClick: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HomeContent(
        uiState = uiState,
        onRegistrarAlumnoClick = onRegistrarAlumnoClick,
        onRegistrarComputadoraClick = onRegistrarComputadoraClick,
        onNuevoPrestamoClick = onNuevoPrestamoClick,
        onRegistrarDevolucionClick = onRegistrarDevolucionClick,
        onHistorialClick = onHistorialClick,
        onRolesPermisosClick = onRolesPermisosClick,
        onLogoutClick = onLogoutClick,
        modifier = modifier,
    )
}

@Composable
private fun HomeContent(
    uiState: HomeUiState,
    onRegistrarAlumnoClick: () -> Unit,
    onRegistrarComputadoraClick: () -> Unit,
    onNuevoPrestamoClick: () -> Unit,
    onRegistrarDevolucionClick: () -> Unit,
    onHistorialClick: () -> Unit,
    onRolesPermisosClick: () -> Unit,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val acceso = uiState.acceso

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(IenCream),
    ) {
        HomeHeader(onLogoutClick = onLogoutClick)

        // Contenido scrolleable. weight(1f) ocupa todo el alto sobrante, así el footer
        // (que está FUERA de este Column) queda siempre anclado abajo, haya poco o mucho contenido.
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(20.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    icono = Icons.Filled.Laptop,
                    colorFondoIcono = StatAmarilloPalido,
                    colorIcono = StatDorado,
                    numero = uiState.cantidadPrestados,
                    etiqueta = "Prestados",
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    icono = Icons.Filled.Check,
                    colorFondoIcono = StatVerdePalido,
                    colorIcono = StatVerde,
                    numero = uiState.cantidadDevueltosHoy,
                    etiqueta = "Devueltos hoy",
                    modifier = Modifier.weight(1f),
                )
            }

            SectionTitle("ACCIONES RÁPIDAS")

            // Cada acción se muestra solo si el rol del usuario tiene ese permiso.
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (acceso.puede(Permiso.REGISTRAR_ALUMNO)) {
                    QuickActionCard(
                        icon = Icons.Filled.PersonAdd,
                        titulo = "Registrar alumno",
                        subtitulo = "Agregar nuevo estudiante al sistema",
                        onClick = onRegistrarAlumnoClick,
                    )
                }
                if (acceso.puede(Permiso.REGISTRAR_COMPUTADORA)) {
                    QuickActionCard(
                        icon = Icons.Filled.Laptop,
                        titulo = "Registrar Computadora",
                        subtitulo = "Registra una computadora nueva",
                        onClick = onRegistrarComputadoraClick,
                    )
                }
                if (acceso.puede(Permiso.REGISTRAR_PRESTAMO)) {
                    QuickActionCard(
                        icon = Icons.Filled.Laptop,
                        titulo = "Nuevo préstamo",
                        subtitulo = "Registrar entrega de computadora",
                        onClick = onNuevoPrestamoClick,
                    )
                }
                if (acceso.puede(Permiso.REGISTRAR_DEVOLUCION)) {
                    QuickActionCard(
                        icon = Icons.Filled.QrCodeScanner,
                        titulo = "Registrar devolución",
                        subtitulo = "Controlar el equipo devuelto",
                        onClick = onRegistrarDevolucionClick,
                    )
                }
                if (acceso.puede(Permiso.VER_HISTORIAL)) {
                    QuickActionCard(
                        icon = Icons.Filled.Schedule,
                        titulo = "Historial semanal",
                        subtitulo = "Ver préstamos y devoluciones",
                        onClick = onHistorialClick,
                    )
                }
            }

            // Exclusiva del Admin: Gestión de Permisos nunca se otorga a otro rol.
            if (acceso.esAdmin) {
                SectionTitle("ADMINISTRACIÓN")
                QuickActionCard(
                    icon = Icons.Filled.AdminPanelSettings,
                    titulo = "Roles y Permisos",
                    subtitulo = "Definir qué puede hacer cada rol",
                    onClick = onRolesPermisosClick,
                )
            }

            SectionTitle("ACTIVIDAD RECIENTE")

            // Column + forEach (no LazyColumn): la lista es corta y vive dentro de un verticalScroll.
            if (uiState.actividadReciente.isEmpty()) {
                Text(
                    text = "Todavía no hay movimientos.",
                    color = IenGreyText,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    uiState.actividadReciente.forEach { actividad ->
                        RecentActivityItem(actividad)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
        }

        // Footer sticky: fuera del scroll, siempre visible al pie.
        Text(
            text = "Versión 1.0 · IEN 2026",
            color = IenGreyText,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .background(IenCream)
                .navigationBarsPadding()
                .padding(vertical = 12.dp),
        )
    }
}

/** Header violeta simple: saludo a la izquierda y botón de cerrar sesión a la derecha. */
@Composable
private fun HomeHeader(onLogoutClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(IenPurple, IenPurpleDark)))
            .statusBarsPadding()
            .padding(start = 24.dp, end = 12.dp, top = 20.dp, bottom = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Buen día!",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onLogoutClick) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Logout,
                contentDescription = "Cerrar sesión",
                tint = Color.White,
            )
        }
    }
}

@Composable
private fun SectionTitle(texto: String) {
    Text(
        text = texto,
        color = IenPurple,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(top = 24.dp, bottom = 10.dp),
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun HomeScreenPreview() {
    PrestamosComputadorasTheme {
        HomeContent(
            uiState = HomeViewModel.datosDePrueba(),
            onRegistrarAlumnoClick = {},
            onRegistrarComputadoraClick = {},
            onNuevoPrestamoClick = {},
            onRegistrarDevolucionClick = {},
            onHistorialClick = {},
            onRolesPermisosClick = {},
            onLogoutClick = {},
        )
    }
}
