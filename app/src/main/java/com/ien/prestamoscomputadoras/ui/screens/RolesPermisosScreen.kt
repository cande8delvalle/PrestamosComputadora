package com.ien.prestamoscomputadoras.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ien.prestamoscomputadoras.data.Permiso
import com.ien.prestamoscomputadoras.ui.components.BackHeader
import com.ien.prestamoscomputadoras.ui.theme.IenCream
import com.ien.prestamoscomputadoras.ui.theme.IenGreyText
import com.ien.prestamoscomputadoras.ui.theme.IenLavender
import com.ien.prestamoscomputadoras.ui.theme.IenPurple
import com.ien.prestamoscomputadoras.ui.theme.PrestamosComputadorasTheme
import com.ien.prestamoscomputadoras.viewmodel.PermisoItem
import com.ien.prestamoscomputadoras.viewmodel.RolTab
import com.ien.prestamoscomputadoras.viewmodel.RolesPermisosUiState
import com.ien.prestamoscomputadoras.viewmodel.RolesPermisosViewModel

/** Fondo durazno clarito de las tarjetas de permiso. */
private val DuraznoClaro = Color(0xFFFFF1E6)

/**
 * Pantalla "Roles y Permisos", exclusiva del Admin (sección Administración de Home).
 *
 * Este composable solo conecta el [RolesPermisosViewModel] con la UI; el diseño vive en
 * [RolesPermisosContent] (stateless), que es el que se previsualiza.
 */
@Composable
fun RolesPermisosScreen(
    modifier: Modifier = Modifier,
    viewModel: RolesPermisosViewModel = viewModel(factory = RolesPermisosViewModel.Factory),
    /** Flecha de "volver" del header. */
    onVolverClick: () -> Unit = {},
) {
    RolesPermisosContent(
        uiState = viewModel.uiState,
        onTabSeleccionado = viewModel::onTabSeleccionado,
        onPermisoClick = viewModel::onPermisoClick,
        onVolverClick = onVolverClick,
        modifier = modifier,
    )
}

@Composable
private fun RolesPermisosContent(
    uiState: RolesPermisosUiState,
    onTabSeleccionado: (RolTab) -> Unit,
    onPermisoClick: (Permiso) -> Unit,
    onVolverClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(IenCream),
    ) {
        BackHeader(titulo = "Roles y Permisos", onVolverClick = onVolverClick)

        when {
            uiState.cargando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = IenPurple)
            }

            !uiState.esAdmin -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Solo el Admin puede gestionar los roles y permisos.",
                    color = IenGreyText,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                )
            }

            else -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    RolTab.entries.forEach { tab ->
                        RolChip(
                            texto = tab.titulo,
                            seleccionado = uiState.tab == tab,
                            onClick = { onTabSeleccionado(tab) },
                        )
                    }
                }

                AyudaTab(uiState.tab)

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(uiState.items, key = { it.permiso.name }) { item ->
                        TarjetaPermiso(item, onClick = { onPermisoClick(item.permiso) })
                    }
                }
            }
        }
    }
}

/** Chip tipo tab: violeta sólido con texto blanco si está seleccionado, lavanda si no. */
@Composable
private fun RolChip(texto: String, seleccionado: Boolean, onClick: () -> Unit) {
    val forma = RoundedCornerShape(50)
    Text(
        text = texto,
        color = if (seleccionado) Color.White else IenPurple,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(forma)
            .background(if (seleccionado) IenPurple else IenLavender, forma)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

/** Línea de ayuda debajo de los tabs: por qué el Admin no se edita / cómo editar. */
@Composable
private fun AyudaTab(tab: RolTab) {
    val (icono, texto) = when (tab) {
        RolTab.ADMIN -> Icons.Filled.Lock to "Rol de sistema: tiene todos los permisos y no se puede editar."
        RolTab.PERSONAL -> Icons.Filled.TouchApp to "Tocá un permiso para habilitarlo o deshabilitarlo."
    }
    Row(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = icono, contentDescription = null, tint = IenGreyText, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(text = texto, color = IenGreyText, fontSize = 12.sp)
    }
}

@Composable
private fun TarjetaPermiso(item: PermisoItem, onClick: () -> Unit) {
    val forma = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(forma)
            .background(DuraznoClaro, forma)
            .border(BorderStroke(1.dp, IenPurple.copy(alpha = 0.06f)), forma)
            // Toda la tarjeta es tocable (no solo el círculo): blanco de toque más grande.
            .toggleable(
                value = item.habilitado,
                enabled = item.editable,
                role = Role.Checkbox,
                onValueChange = { onClick() },
            )
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CirculoTilde(habilitado = item.habilitado)
        Spacer(Modifier.width(14.dp))
        Column {
            Text(
                text = item.permiso.titulo,
                color = IenPurple,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = item.permiso.descripcion,
                color = IenGreyText,
                fontSize = 13.sp,
            )
        }
    }
}

/** Violeta sólido con check blanco si está habilitado; vacío con borde si no. */
@Composable
private fun CirculoTilde(habilitado: Boolean) {
    val icono: ImageVector? = if (habilitado) Icons.Filled.Check else null
    Box(
        modifier = Modifier
            .size(28.dp)
            .then(
                if (habilitado) {
                    Modifier.background(IenPurple, CircleShape)
                } else {
                    Modifier.border(2.dp, IenPurple.copy(alpha = 0.45f), CircleShape)
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (icono != null) {
            Icon(imageVector = icono, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun RolesPermisosPreview() {
    PrestamosComputadorasTheme {
        RolesPermisosContent(
            uiState = RolesPermisosUiState(
                tab = RolTab.PERSONAL,
                cargando = false,
                esAdmin = true,
                permisosPersonal = Permiso.inicialesPersonal,
            ),
            onTabSeleccionado = {},
            onPermisoClick = {},
            onVolverClick = {},
        )
    }
}
