package com.ien.prestamoscomputadoras.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageAnalysis
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.ien.prestamoscomputadoras.data.repository.PrestamoEncontrado
import com.ien.prestamoscomputadoras.ui.components.BackHeader
import com.ien.prestamoscomputadoras.ui.components.ExitoOverlay
import com.ien.prestamoscomputadoras.ui.components.FormErrorRed
import com.ien.prestamoscomputadoras.ui.components.FormLabel
import com.ien.prestamoscomputadoras.ui.components.lavenderFieldColors
import com.ien.prestamoscomputadoras.ui.theme.IenCream
import com.ien.prestamoscomputadoras.ui.theme.IenGreyText
import com.ien.prestamoscomputadoras.ui.theme.IenLavender
import com.ien.prestamoscomputadoras.ui.theme.IenPurple
import com.ien.prestamoscomputadoras.ui.theme.IenYellow
import com.ien.prestamoscomputadoras.ui.theme.PrestamosComputadorasTheme
import com.ien.prestamoscomputadoras.viewmodel.EtapaDevolucion
import com.ien.prestamoscomputadoras.viewmodel.RegistrarDevolucionUiState
import com.ien.prestamoscomputadoras.viewmodel.RegistrarDevolucionViewModel
import com.ien.prestamoscomputadoras.viewmodel.formatearDni
import com.ien.prestamoscomputadoras.viewmodel.formatearFechaHoraDevolucion

/** Fondo y tono del cartel de advertencia. */
private val AdvertenciaFondo = Color(0xFFFFF3CC)
private val AdvertenciaIcono = Color(0xFFB8860B)

/**
 * Pantalla "Registrar Devolución", a la que se llega desde la acción rápida de Home.
 *
 * Dos etapas (ver [EtapaDevolucion]): primero la cámara lee el QR de la computadora y, si
 * tiene un préstamo activo, se desliza desde abajo la tarjeta para revisar el equipo y
 * confirmar. "Atrás" desde la confirmación vuelve a la cámara (estado del ViewModel), no
 * sale de la pantalla.
 */
@Composable
fun RegistrarDevolucionScreen(
    modifier: Modifier = Modifier,
    viewModel: RegistrarDevolucionViewModel = viewModel(factory = RegistrarDevolucionViewModel.Factory),
    /** Flecha de "volver" del header, desde la etapa de escaneo (sale de la pantalla). */
    onVolverClick: () -> Unit = {},
    /** Botón "Volver al inicio" del modal de éxito (navega a Home). */
    onVolverAlInicioClick: () -> Unit = {},
) {
    val uiState = viewModel.uiState
    val confirmando = uiState.etapaActual == EtapaDevolucion.CONFIRMANDO

    BackHandler(enabled = confirmando && !uiState.mostrarModalExito) {
        viewModel.volverAEscanear()
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.errorEscaneo) {
        val error = uiState.errorEscaneo ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(error)
        viewModel.onErrorEscaneoMostrado()
    }

    val permisoCamara = rememberPermisoCamara()

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(IenCream),
        ) {
            BackHeader(
                titulo = if (confirmando) "Registrar Devolución" else "Escanear QR",
                onVolverClick = {
                    if (confirmando) viewModel.volverAEscanear() else onVolverClick()
                },
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                // Fuera de la etapa de escaneo la cámara sale de la composición: se libera
                // (unbind) y deja de leer códigos.
                if (!confirmando) {
                    EtapaEscaneo(
                        permiso = permisoCamara,
                        buscando = uiState.buscando,
                        onCodigoLeido = viewModel::onCodigoEscaneado,
                    )
                }

                // Se recuerda el último préstamo para que la tarjeta tenga datos mientras
                // se anima su salida (en ese momento el estado ya volvió a ESCANEANDO).
                var ultimoPrestamo by remember { mutableStateOf<PrestamoEncontrado?>(null) }
                uiState.prestamoEncontrado?.let { ultimoPrestamo = it }

                // Nombre calificado: dentro de este Box (que está en un Column) se elegiría
                // la variante ColumnScope.AnimatedVisibility, que no aplica acá.
                androidx.compose.animation.AnimatedVisibility(
                    visible = confirmando,
                    enter = slideInVertically { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut(),
                ) {
                    ultimoPrestamo?.let { prestamo ->
                        ConfirmacionDevolucion(
                            prestamo = prestamo,
                            uiState = uiState,
                            onEnciendeChange = viewModel::onEnciendeChange,
                            onPantallaOkChange = viewModel::onPantallaOkChange,
                            onIncluyeCargadorChange = viewModel::onIncluyeCargadorChange,
                            onObservacionesChange = viewModel::onObservacionesChange,
                            onConfirmarClick = viewModel::onConfirmarClick,
                            onCancelarClick = viewModel::volverAEscanear,
                        )
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(16.dp),
        )

        if (uiState.mostrarModalExito) {
            ExitoOverlay(
                mensaje = "Devolución Registrada Correctamente",
                textoPrincipal = "Realizar otra Devolución",
                onPrincipalClick = viewModel::volverAEscanear,
                textoSecundario = "Volver al inicio",
                onSecundarioClick = {
                    // Primero se cierra el modal (estado del ViewModel) y después se navega a Home.
                    viewModel.onCerrarModalExito()
                    onVolverAlInicioClick()
                },
            )
        }
    }
}

// ---------------------------------------------------------------------------------------
// Permiso de cámara
// ---------------------------------------------------------------------------------------

/** Estado del permiso de cámara y la acción para pedirlo. */
private class PermisoCamara(
    val concedido: Boolean,
    /** El usuario lo negó con "no volver a preguntar": solo se puede habilitar desde Ajustes. */
    val denegadoPermanente: Boolean,
    val solicitar: () -> Unit,
)

/**
 * Pide el permiso de cámara la primera vez que se entra a la pantalla y lo vuelve a chequear
 * en cada ON_RESUME (por si el usuario lo habilitó desde Ajustes y volvió).
 */
@Composable
private fun rememberPermisoCamara(): PermisoCamara {
    val context = LocalContext.current
    fun chequear() =
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED

    var concedido by remember { mutableStateOf(chequear()) }
    var denegadoPermanente by rememberSaveable { mutableStateOf(false) }
    var yaSolicitado by rememberSaveable { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        concedido = ok
        // Tras una negativa, si Android ya no mostraría la explicación es porque el usuario
        // eligió "no volver a preguntar": el diálogo del sistema no va a aparecer más.
        denegadoPermanente = !ok && context.findActivity()?.let {
            !ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.CAMERA)
        } == true
    }

    LaunchedEffect(Unit) {
        if (!concedido && !yaSolicitado) {
            yaSolicitado = true
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    LifecycleResumeEffect(Unit) {
        concedido = chequear()
        onPauseOrDispose { }
    }

    return PermisoCamara(
        concedido = concedido,
        denegadoPermanente = denegadoPermanente,
        solicitar = {
            if (denegadoPermanente) {
                context.startActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null),
                    ),
                )
            } else {
                launcher.launch(Manifest.permission.CAMERA)
            }
        },
    )
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
private fun PedirPermisoCamara(permiso: PermisoCamara, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(IenCream)
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .background(IenLavender, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.CameraAlt,
                contentDescription = null,
                tint = IenPurple,
                modifier = Modifier.size(44.dp),
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = "Necesitamos acceso a la cámara",
            color = IenPurple,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (permiso.denegadoPermanente) {
                "El permiso fue denegado. Habilitalo desde los ajustes de la app para escanear el QR del equipo."
            } else {
                "La usamos solo para escanear el código QR pegado en el equipo a devolver."
            },
            color = IenGreyText,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        BotonAmarillo(
            texto = if (permiso.denegadoPermanente) "Abrir ajustes" else "Conceder permiso",
            onClick = permiso.solicitar,
        )
    }
}

// ---------------------------------------------------------------------------------------
// Etapa 1: escaneo
// ---------------------------------------------------------------------------------------

@Composable
private fun EtapaEscaneo(
    permiso: PermisoCamara,
    buscando: Boolean,
    onCodigoLeido: (String) -> Unit,
) {
    if (!permiso.concedido) {
        PedirPermisoCamara(permiso)
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            CamaraQr(onCodigoLeido = onCodigoLeido, modifier = Modifier.fillMaxSize())
            MarcoGuia(modifier = Modifier.size(240.dp))
            if (buscando) {
                CircularProgressIndicator(color = IenYellow)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Apuntá la cámara al código QR",
                color = IenPurple,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Escaneá el código pegado en el equipo a devolver",
                color = IenGreyText,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * Vista previa de CameraX con análisis de ML Kit: cada QR leído se informa con
 * [onCodigoLeido] (puede llegar varias veces por segundo; el ViewModel filtra).
 * La cámara se libera al salir de la composición.
 */
@Composable
private fun CamaraQr(onCodigoLeido: (String) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val onCodigoActual by rememberUpdatedState(onCodigoLeido)
    val controller = remember { LifecycleCameraController(context) }

    DisposableEffect(lifecycleOwner) {
        val executor = ContextCompat.getMainExecutor(context)
        val scanner = BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build(),
        )
        controller.setEnabledUseCases(CameraController.IMAGE_ANALYSIS)
        controller.setImageAnalysisAnalyzer(
            executor,
            MlKitAnalyzer(listOf(scanner), ImageAnalysis.COORDINATE_SYSTEM_ORIGINAL, executor) { resultado ->
                resultado.getValue(scanner)
                    ?.firstNotNullOfOrNull { it.rawValue }
                    ?.let { onCodigoActual(it) }
            },
        )
        controller.bindToLifecycle(lifecycleOwner)

        onDispose {
            controller.unbind()
            controller.clearImageAnalysisAnalyzer()
            scanner.close()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PreviewView(ctx).apply {
                // COMPATIBLE (TextureView): se recorta y anima bien dentro de Compose.
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                scaleType = PreviewView.ScaleType.FILL_CENTER
                this.controller = controller
            }
        },
    )
}

/** Guía de dónde apuntar: las 4 esquinas de un cuadrado, en amarillo. */
@Composable
private fun MarcoGuia(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val largo = 40.dp.toPx()
        val grosor = 5.dp.toPx()
        val w = size.width
        val h = size.height
        fun linea(desde: Offset, hasta: Offset) =
            drawLine(IenYellow, desde, hasta, strokeWidth = grosor, cap = StrokeCap.Round)

        linea(Offset(0f, 0f), Offset(largo, 0f))
        linea(Offset(0f, 0f), Offset(0f, largo))
        linea(Offset(w, 0f), Offset(w - largo, 0f))
        linea(Offset(w, 0f), Offset(w, largo))
        linea(Offset(0f, h), Offset(largo, h))
        linea(Offset(0f, h), Offset(0f, h - largo))
        linea(Offset(w, h), Offset(w - largo, h))
        linea(Offset(w, h), Offset(w, h - largo))
    }
}

// ---------------------------------------------------------------------------------------
// Etapa 2: confirmación
// ---------------------------------------------------------------------------------------

/** Tarjeta que sube desde abajo con los datos del préstamo, el checklist y los botones. */
@Composable
private fun ConfirmacionDevolucion(
    prestamo: PrestamoEncontrado,
    uiState: RegistrarDevolucionUiState,
    onEnciendeChange: (Boolean) -> Unit,
    onPantallaOkChange: (Boolean) -> Unit,
    onIncluyeCargadorChange: (Boolean) -> Unit,
    onObservacionesChange: (String) -> Unit,
    onConfirmarClick: () -> Unit,
    onCancelarClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 12.dp),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = Color.White,
        shadowElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(remember { ScrollState(0) })
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.QrCode2,
                    contentDescription = null,
                    tint = IenPurple,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "QR escaneado",
                    color = IenPurple,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                )
            }
            Text(
                text = "Préstamo Encontrado",
                color = IenGreyText,
                fontSize = 13.sp,
                modifier = Modifier.padding(start = 28.dp),
            )

            Spacer(Modifier.height(16.dp))

            DatosPrestamo(prestamo)

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AdvertenciaFondo, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.WarningAmber,
                    contentDescription = null,
                    tint = AdvertenciaIcono,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Verificá que el equipo y el alumno sean correctos antes de confirmar la devolución.",
                    color = Color(0xFF5C4A00),
                    fontSize = 13.sp,
                )
            }

            Spacer(Modifier.height(20.dp))

            FormLabel("ESTADO DEL EQUIPO")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ItemChecklist("¿El equipo enciende?", uiState.enciende, !uiState.isLoading, onEnciendeChange)
                ItemChecklist("¿Pantalla en buen estado?", uiState.pantallaOk, !uiState.isLoading, onPantallaOkChange)
                ItemChecklist("¿Incluye cargador?", uiState.incluyeCargador, !uiState.isLoading, onIncluyeCargadorChange)
            }

            Spacer(Modifier.height(20.dp))

            FormLabel("OBSERVACIONES (OPCIONAL)")
            OutlinedTextField(
                value = uiState.observaciones,
                onValueChange = onObservacionesChange,
                enabled = !uiState.isLoading,
                minLines = 3,
                placeholder = { Text("Detallar daños, faltantes u observaciones relevantes...") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = lavenderFieldColors(),
            )

            if (uiState.errorConfirmacion != null) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = uiState.errorConfirmacion,
                    color = FormErrorRed,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(24.dp))

            BotonAmarillo(
                texto = "Confirmar Devolución",
                onClick = onConfirmarClick,
                cargando = uiState.isLoading,
            )

            Spacer(Modifier.height(4.dp))

            TextButton(
                onClick = onCancelarClick,
                enabled = !uiState.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                Text(text = "Cancelar", color = IenPurple, fontSize = 15.sp)
            }
        }
    }
}

/** Recuadro lavanda con el alumno, su DNI, el equipo y la fecha/hora del préstamo. */
@Composable
private fun DatosPrestamo(prestamo: PrestamoEncontrado) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(IenLavender, RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(Color.White, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = null,
                tint = IenPurple,
                modifier = Modifier.size(26.dp),
            )
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(
                text = prestamo.nombreAlumno,
                color = IenPurple,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
            )
            Text(
                text = "DNI: ${formatearDni(prestamo.dniAlumno)}",
                color = IenGreyText,
                fontSize = 13.sp,
            )
            Spacer(Modifier.height(10.dp))
            DatoConIcono(Icons.Filled.Laptop, prestamo.codigoComputadora)
            Spacer(Modifier.height(4.dp))
            DatoConIcono(Icons.Filled.Schedule, formatearFechaHoraDevolucion(prestamo.fechaPrestamo))
        }
    }
}

@Composable
private fun DatoConIcono(icono: ImageVector, texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icono, contentDescription = null, tint = IenPurple, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(text = texto, color = IenPurple, fontSize = 14.sp)
    }
}

/** Fila tildable del checklist: check circular violeta si está marcada, círculo gris si no. */
@Composable
private fun ItemChecklist(
    pregunta: String,
    marcado: Boolean,
    enabled: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (marcado) IenPurple.copy(alpha = 0.08f) else IenCream)
            .toggleable(value = marcado, enabled = enabled, role = Role.Checkbox, onValueChange = onChange)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (marcado) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (marcado) IenPurple else IenGreyText,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = pregunta,
            color = if (marcado) IenPurple else IenGreyText,
            fontWeight = if (marcado) FontWeight.SemiBold else FontWeight.Normal,
            fontSize = 15.sp,
        )
    }
}

@Composable
private fun BotonAmarillo(texto: String, onClick: () -> Unit, cargando: Boolean = false) {
    Button(
        onClick = onClick,
        enabled = !cargando,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = IenYellow,
            contentColor = IenPurple,
            disabledContainerColor = IenYellow.copy(alpha = 0.6f),
            disabledContentColor = IenPurple.copy(alpha = 0.7f),
        ),
    ) {
        if (cargando) {
            CircularProgressIndicator(
                color = IenPurple,
                strokeWidth = 2.dp,
                modifier = Modifier.size(22.dp),
            )
        } else {
            Text(text = texto, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F0E8, heightDp = 900, name = "Devolución - Etapa 2")
@Composable
private fun ConfirmacionDevolucionPreview() {
    val prestamo = PrestamoEncontrado(
        idPrestamo = 1,
        nombreAlumno = "María Gonzales",
        dniAlumno = "46111222",
        codigoComputadora = "PC-12",
        fechaPrestamo = 1_782_320_400_000,
    )
    PrestamosComputadorasTheme {
        ConfirmacionDevolucion(
            prestamo = prestamo,
            uiState = RegistrarDevolucionUiState(
                etapaActual = EtapaDevolucion.CONFIRMANDO,
                prestamoEncontrado = prestamo,
                incluyeCargador = false,
            ),
            onEnciendeChange = {},
            onPantallaOkChange = {},
            onIncluyeCargadorChange = {},
            onObservacionesChange = {},
            onConfirmarClick = {},
            onCancelarClick = {},
        )
    }
}
