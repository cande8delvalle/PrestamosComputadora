package com.ien.prestamoscomputadoras.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ien.prestamoscomputadoras.data.repository.BusquedaDevolucion
import com.ien.prestamoscomputadoras.data.repository.DevolucionRepository
import com.ien.prestamoscomputadoras.data.repository.PrestamoEncontrado
import com.ien.prestamoscomputadoras.data.repository.PrestamoYaDevueltoException
import com.ien.prestamoscomputadoras.data.repository.SupabaseDevolucionRepository
import com.ien.prestamoscomputadoras.util.SesionActual
import com.ien.prestamoscomputadoras.util.fechaArgentina
import com.ien.prestamoscomputadoras.util.horaArgentina
import kotlin.time.Instant
import kotlinx.coroutines.launch

/**
 * Tiempo durante el que se ignora un código ya rechazado, contado desde que se cerró el aviso:
 * la cámara lo sigue leyendo en cada cuadro mientras está enfocado, y sin esto el diálogo
 * volvería a abrirse apenas se toca "Entendido".
 */
private const val ESPERA_REESCANEO_MS = 3_000L

/** Las dos etapas de la pantalla: cámara buscando un QR, o tarjeta de confirmación. */
enum class EtapaDevolucion { ESCANEANDO, CONFIRMANDO }

/** Estado de la pantalla "Registrar Devolución". Inmutable: se regenera con [copy]. */
data class RegistrarDevolucionUiState(
    val etapaActual: EtapaDevolucion = EtapaDevolucion.ESCANEANDO,
    /** `true` mientras se busca en la base el préstamo del QR recién leído. */
    val buscando: Boolean = false,
    /** Préstamo del QR escaneado. Solo tiene valor en [EtapaDevolucion.CONFIRMANDO]. */
    val prestamoEncontrado: PrestamoEncontrado? = null,
    // Checklist del equipo: arrancan en true, el usuario destilda lo que esté mal.
    val enciende: Boolean = true,
    val pantallaOk: Boolean = true,
    val incluyeCargador: Boolean = true,
    val observaciones: String = "",
    val isLoading: Boolean = false,
    val mostrarModalExito: Boolean = false,
    /**
     * Error al buscar el QR (p. ej. sin internet). La UI lo muestra en un snackbar y avisa con
     * [onErrorEscaneoMostrado].
     */
    val errorEscaneo: String? = null,
    /**
     * Aviso en diálogo centrado: computadora sin préstamo (nunca prestada o ya devuelta) o
     * préstamo devuelto desde otro celular. Mientras está abierto no se procesan más QR.
     */
    val aviso: AvisoUi? = null,
    /** Error al confirmar (se muestra arriba del botón). */
    val errorConfirmacion: String? = null,
)

/** ViewModel de [com.ien.prestamoscomputadoras.ui.screens.RegistrarDevolucionScreen]. */
class RegistrarDevolucionViewModel(
    private val devolucionRepository: DevolucionRepository,
    /** Reloj inyectable para los tests. */
    private val ahora: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    var uiState by mutableStateOf(RegistrarDevolucionUiState())
        private set

    private var ultimoCodigoRechazado: String? = null
    private var momentoUltimoRechazo = 0L

    /**
     * Llega por cada QR que lee la cámara (puede ser varias veces por segundo). Solo se procesa
     * si se está escaneando y no hay otra búsqueda en curso. Si el código tiene un préstamo
     * activo se pasa a la etapa de confirmación; si no, se informa y se sigue escaneando.
     */
    fun onCodigoEscaneado(valor: String) {
        val s = uiState
        if (s.etapaActual != EtapaDevolucion.ESCANEANDO || s.buscando || s.mostrarModalExito || s.aviso != null) return
        val codigo = valor.trim()
        if (codigo.isEmpty()) return
        if (codigo == ultimoCodigoRechazado && ahora() - momentoUltimoRechazo < ESPERA_REESCANEO_MS) return

        uiState = s.copy(buscando = true)
        viewModelScope.launch {
            devolucionRepository.buscarParaDevolucion(codigo)
                .onSuccess { busqueda ->
                    uiState = when (busqueda) {
                        is BusquedaDevolucion.ConPrestamoActivo -> RegistrarDevolucionUiState(
                            etapaActual = EtapaDevolucion.CONFIRMANDO,
                            prestamoEncontrado = busqueda.prestamo,
                        )
                        // Sin préstamo activo: diálogo. Se ignora este código hasta cerrarlo.
                        is BusquedaDevolucion.SinPrestamos -> rechazar(
                            codigo,
                            aviso = avisoNuncaPrestada(busqueda.codigoComputadora),
                        )
                        is BusquedaDevolucion.YaDevuelta -> rechazar(
                            codigo,
                            aviso = avisoYaDevuelta(busqueda.codigoComputadora, busqueda.ultimaDevolucion),
                        )
                    }
                }
                .onFailure { e ->
                    // Error de red: snackbar y se sigue escaneando (con la misma espera).
                    uiState = rechazar(codigo, aviso = null).copy(errorEscaneo = e.message)
                }
        }
    }

    /** Marca [codigo] como rechazado (para no procesarlo en cada cuadro) y deja de buscar. */
    private fun rechazar(codigo: String, aviso: AvisoUi?): RegistrarDevolucionUiState {
        ultimoCodigoRechazado = codigo
        momentoUltimoRechazo = ahora()
        return uiState.copy(buscando = false, aviso = aviso)
    }

    /**
     * "Entendido" del diálogo: vuelve al escáner listo para leer otro QR. El código recién
     * rechazado se sigue ignorando unos segundos desde AHORA, por si sigue frente a la cámara.
     */
    fun onAvisoAceptado() {
        momentoUltimoRechazo = ahora()
        uiState = if (uiState.etapaActual == EtapaDevolucion.CONFIRMANDO) {
            // El préstamo ya lo devolvió otro: no hay nada que confirmar, se vuelve a escanear.
            RegistrarDevolucionUiState()
        } else {
            uiState.copy(aviso = null)
        }
    }

    fun onErrorEscaneoMostrado() {
        uiState = uiState.copy(errorEscaneo = null)
    }

    fun onEnciendeChange(valor: Boolean) {
        uiState = uiState.copy(enciende = valor)
    }

    fun onPantallaOkChange(valor: Boolean) {
        uiState = uiState.copy(pantallaOk = valor)
    }

    fun onIncluyeCargadorChange(valor: Boolean) {
        uiState = uiState.copy(incluyeCargador = valor)
    }

    fun onObservacionesChange(valor: String) {
        uiState = uiState.copy(observaciones = valor)
    }

    /**
     * Marca el préstamo como DEVUELTO y guarda la revisión del equipo. La fecha/hora y quién lo
     * recibió (el usuario logueado) los completa un trigger de la base. Si sale bien, muestra
     * el modal de éxito.
     */
    fun onConfirmarClick() {
        val s = uiState
        val prestamo = s.prestamoEncontrado
        if (s.isLoading || prestamo == null) return

        val usuarioId = SesionActual.usuarioId
        if (usuarioId == null) {
            uiState = s.copy(errorConfirmacion = ERROR_SIN_SESION)
            return
        }

        uiState = s.copy(isLoading = true, errorConfirmacion = null)
        viewModelScope.launch {
            devolucionRepository.registrarDevolucion(
                idPrestamo = prestamo.idPrestamo,
                enciende = s.enciende,
                pantallaOk = s.pantallaOk,
                cargador = s.incluyeCargador,
                observaciones = s.observaciones.trim().ifEmpty { null },
            )
                .onSuccess { uiState = uiState.copy(isLoading = false, mostrarModalExito = true) }
                .onFailure { e ->
                    uiState = if (e is PrestamoYaDevueltoException) {
                        // Lo devolvió otro celular mientras tanto: diálogo, y al cerrarlo, al escáner.
                        ultimoCodigoRechazado = prestamo.codigoComputadora
                        uiState.copy(isLoading = false, aviso = avisoPrestamoYaDevuelto(prestamo.codigoComputadora))
                    } else {
                        uiState.copy(isLoading = false, errorConfirmacion = e.message)
                    }
                }
        }
    }

    /**
     * Vuelve a la etapa de escaneo descartando todo (botón "Cancelar", flecha/atrás desde la
     * confirmación y "Realizar otra Devolución" del modal).
     */
    fun volverAEscanear() {
        if (uiState.isLoading) return
        uiState = RegistrarDevolucionUiState()
    }

    /** "Volver al inicio": cierra el modal. La navegación a Home la hace la UI. */
    fun onCerrarModalExito() {
        uiState = uiState.copy(mostrarModalExito = false)
    }

    companion object {
        /** Crea el ViewModel con su repositorio (Supabase). */
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { RegistrarDevolucionViewModel(SupabaseDevolucionRepository()) }
        }
    }
}

/** "24/06/2026 · 17:00 hs", en hora de Argentina. */
fun formatearFechaHoraDevolucion(instante: Instant): String =
    "${instante.fechaArgentina()} · ${instante.horaArgentina()} hs"
