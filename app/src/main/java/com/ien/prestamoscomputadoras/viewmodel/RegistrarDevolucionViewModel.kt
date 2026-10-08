package com.ien.prestamoscomputadoras.viewmodel

import android.database.sqlite.SQLiteConstraintException
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.room.withTransaction
import com.ien.prestamoscomputadoras.data.AppDatabase
import com.ien.prestamoscomputadoras.data.repository.DevolucionRepository
import com.ien.prestamoscomputadoras.data.repository.PrestamoEncontrado
import com.ien.prestamoscomputadoras.data.repository.PrestamoYaDevueltoException
import com.ien.prestamoscomputadoras.util.SesionActual
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

const val ERROR_SIN_PRESTAMO_ACTIVO = "Esta computadora no tiene préstamos pendientes de devolución"
const val ERROR_PRESTAMO_YA_DEVUELTO = "Este préstamo ya fue registrado como devuelto."

/**
 * Tiempo durante el que se ignora un código que ya se rechazó: la cámara lo sigue leyendo en
 * cada cuadro mientras está enfocado, y sin esto el mensaje se repetiría sin parar.
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
    /** QR sin préstamo activo. La UI lo muestra en un snackbar y avisa con [onErrorEscaneoMostrado]. */
    val errorEscaneo: String? = null,
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
        if (s.etapaActual != EtapaDevolucion.ESCANEANDO || s.buscando || s.mostrarModalExito) return
        val codigo = valor.trim()
        if (codigo.isEmpty()) return
        if (codigo == ultimoCodigoRechazado && ahora() - momentoUltimoRechazo < ESPERA_REESCANEO_MS) return

        uiState = s.copy(buscando = true)
        viewModelScope.launch {
            try {
                val prestamo = devolucionRepository.buscarPrestamoActivo(codigo)
                if (prestamo == null) {
                    ultimoCodigoRechazado = codigo
                    momentoUltimoRechazo = ahora()
                    uiState = uiState.copy(errorEscaneo = ERROR_SIN_PRESTAMO_ACTIVO)
                } else {
                    uiState = RegistrarDevolucionUiState(
                        etapaActual = EtapaDevolucion.CONFIRMANDO,
                        prestamoEncontrado = prestamo,
                    )
                }
            } finally {
                uiState = uiState.copy(buscando = false)
            }
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
     * Guarda la revisión del equipo y marca el préstamo como DEVUELTO, las dos cosas con la
     * fecha/hora actual y en una sola transacción. Queda registrado que la recibió el
     * administrador de la sesión actual. Si sale bien, muestra el modal de éxito.
     */
    fun onConfirmarClick() {
        val s = uiState
        val prestamo = s.prestamoEncontrado
        if (s.isLoading || prestamo == null) return

        val administradorId = SesionActual.administradorId
        if (administradorId == null) {
            uiState = s.copy(errorConfirmacion = ERROR_SIN_SESION)
            return
        }

        uiState = s.copy(isLoading = true, errorConfirmacion = null)
        viewModelScope.launch {
            try {
                devolucionRepository.registrarDevolucion(
                    idPrestamo = prestamo.idPrestamo,
                    enciende = s.enciende,
                    pantallaOk = s.pantallaOk,
                    cargador = s.incluyeCargador,
                    observaciones = s.observaciones.trim().ifEmpty { null },
                    fecha = ahora(),
                    idAdministradorDevolucion = administradorId,
                )
                uiState = uiState.copy(mostrarModalExito = true)
            } catch (e: PrestamoYaDevueltoException) {
                uiState = uiState.copy(errorConfirmacion = ERROR_PRESTAMO_YA_DEVUELTO)
            } catch (e: SQLiteConstraintException) {
                // Ya había una revisión para este préstamo.
                uiState = uiState.copy(errorConfirmacion = ERROR_PRESTAMO_YA_DEVUELTO)
            } finally {
                uiState = uiState.copy(isLoading = false)
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
        /** Crea el ViewModel con su repositorio (Room) a partir del Application. */
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                val db = AppDatabase.getInstance(app)
                RegistrarDevolucionViewModel(
                    DevolucionRepository(
                        prestamoDao = db.prestamoDao(),
                        alumnoDao = db.alumnoDao(),
                        estadoComputadoraDao = db.estadoComputadoraDao(),
                        enTransaccion = { bloque -> db.withTransaction { bloque() } },
                    ),
                )
            }
        }
    }
}

/** "24/06/2026 · 17:00 hs". */
fun formatearFechaHoraDevolucion(epochMs: Long): String =
    SimpleDateFormat("dd/MM/yyyy · HH:mm 'hs'", Locale.US).format(Date(epochMs))
