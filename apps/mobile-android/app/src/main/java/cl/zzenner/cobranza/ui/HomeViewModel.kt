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
            val pendientes = gestionRepository.contarNoResueltas()
            if (pendientes == 0) {
                logoutUseCase()
                _estadoLogout.value = EstadoLogout.Inactivo
            } else {
                _estadoLogout.value = EstadoLogout.GestionesPendientes(pendientes)
            }
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
            val restantes = gestionRepository.contarNoResueltas()
            if (restantes == 0) {
                logoutUseCase()
                _estadoLogout.value = EstadoLogout.Inactivo
            } else {
                _estadoLogout.value = EstadoLogout.ErrorSincronizacion(restantes)
            }
        }
    }

    fun cancelarLogout() {
        _estadoLogout.value = EstadoLogout.Inactivo
    }
}
