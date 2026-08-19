package cl.zzenner.cobranza.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.zzenner.cobranza.feature.asignacion.worker.AsignacionSyncScheduler
import cl.zzenner.cobranza.feature.auth.data.SessionRepository
import cl.zzenner.cobranza.feature.auth.domain.AuthState
import cl.zzenner.cobranza.feature.gestion.data.GestionRepository
import cl.zzenner.cobranza.feature.gestion.worker.GestionSyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class EstadoLogout {
    data object Inactivo : EstadoLogout()
    data object Procesando : EstadoLogout()
    data class GestionesPendientes(val cantidad: Int) : EstadoLogout()
    data class SincronizandoParaCerrar(val pendientesIniciales: Int) : EstadoLogout()
    data class ErrorSincronizacion(val pendientes: Int) : EstadoLogout()

    /**
     * Ya no quedan gestiones reintentables, pero sí gestiones que el servidor rechazó de forma
     * permanente (DT-012). El worker nunca volverá a intentarlas solo; se requiere una decisión
     * explícita del usuario para salir de todas formas, sin perder el registro local.
     */
    data class ConfirmarLogoutConNoRecuperables(val cantidad: Int) : EstadoLogout()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val logoutUseCase: LogoutUseCase,
    private val asignacionScheduler: AsignacionSyncScheduler,
    private val gestionScheduler: GestionSyncScheduler,
    private val gestionRepository: GestionRepository,
) : ViewModel() {

    val authState: StateFlow<AuthState> = sessionRepository.authState

    private val _estadoLogout = MutableStateFlow<EstadoLogout>(EstadoLogout.Inactivo)
    val estadoLogout: StateFlow<EstadoLogout> = _estadoLogout.asStateFlow()

    fun iniciarSincronizacion() {
        asignacionScheduler.programarInmediato()
        asignacionScheduler.programarPeriodico()
        gestionScheduler.programarPeriodico()
    }

    fun solicitarLogout() {
        _estadoLogout.value = EstadoLogout.Procesando
        viewModelScope.launch {
            val reintentables = gestionRepository.contarReintentables()
            if (reintentables > 0) {
                _estadoLogout.value = EstadoLogout.GestionesPendientes(reintentables)
                return@launch
            }
            continuarLogoutTrasReintentables()
        }
    }

    fun sincronizarYLogout() {
        val actual = _estadoLogout.value
        val pendientesIniciales = when (actual) {
            is EstadoLogout.GestionesPendientes -> actual.cantidad
            is EstadoLogout.ErrorSincronizacion -> actual.pendientes
            else -> 0
        }
        _estadoLogout.value = EstadoLogout.SincronizandoParaCerrar(pendientesIniciales)
        viewModelScope.launch {
            gestionRepository.procesarOutbox()
            val reintentables = gestionRepository.contarReintentables()
            if (reintentables > 0) {
                // Prioridad: mientras haya algo que el worker aún pueda enviar solo, no se
                // ofrece la salida "de todas formas" — evita saltarse una reintentable real.
                _estadoLogout.value = EstadoLogout.ErrorSincronizacion(reintentables)
                return@launch
            }
            continuarLogoutTrasReintentables()
        }
    }

    /** Cierra sesión aceptando explícitamente que quedan gestiones no recuperables sin enviar. */
    fun confirmarLogoutConNoRecuperables() {
        viewModelScope.launch {
            logoutUseCase()
            _estadoLogout.value = EstadoLogout.Inactivo
        }
    }

    fun cancelarLogout() {
        _estadoLogout.value = EstadoLogout.Inactivo
    }

    /** Ya se descartó que haya reintentables; solo falta decidir qué hacer con las permanentes. */
    private suspend fun continuarLogoutTrasReintentables() {
        val noRecuperables = gestionRepository.contarNoRecuperables()
        if (noRecuperables > 0) {
            _estadoLogout.value = EstadoLogout.ConfirmarLogoutConNoRecuperables(noRecuperables)
        } else {
            logoutUseCase()
            _estadoLogout.value = EstadoLogout.Inactivo
        }
    }
}
