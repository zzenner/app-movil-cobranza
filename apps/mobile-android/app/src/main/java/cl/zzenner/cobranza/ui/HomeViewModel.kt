package cl.zzenner.cobranza.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.zzenner.cobranza.feature.asignacion.worker.AsignacionSyncScheduler
import cl.zzenner.cobranza.feature.auth.data.SessionRepository
import cl.zzenner.cobranza.feature.auth.domain.AuthState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val logoutUseCase: LogoutUseCase,
    private val scheduler: AsignacionSyncScheduler,
) : ViewModel() {

    val authState: StateFlow<AuthState> = sessionRepository.authState

    /**
     * Programa la descarga inmediata y la descarga periódica al entrar a Home.
     * ExistingWorkPolicy.KEEP evita duplicar si ya hay una encolada.
     */
    fun iniciarSincronizacion() {
        scheduler.programarInmediato()
        scheduler.programarPeriodico()
    }

    fun logout() {
        viewModelScope.launch {
            logoutUseCase()
        }
    }
}
