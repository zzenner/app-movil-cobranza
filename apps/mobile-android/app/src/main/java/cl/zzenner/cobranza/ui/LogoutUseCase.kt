package cl.zzenner.cobranza.ui

import cl.zzenner.cobranza.core.database.transaction.BundleReplacementTransaction
import cl.zzenner.cobranza.feature.asignacion.worker.AsignacionSyncScheduler
import cl.zzenner.cobranza.feature.auth.data.SessionRepository
import javax.inject.Inject

/**
 * Coordina el logout completo: cancela workers, limpia BD local y cierra la sesión remota.
 * Reside en :app para poder acceder a ambas features sin crear dependencia entre ellas.
 */
class LogoutUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val bundleTransaction: BundleReplacementTransaction,
    private val scheduler: AsignacionSyncScheduler,
) {
    suspend operator fun invoke() {
        // 1. Cancelar workers de sincronización
        scheduler.cancelarTodo()
        // 2. Limpiar datos descargados en Room (antes del logout remoto — siempre ocurre)
        bundleTransaction.limpiarTodo()
        // 3. Cerrar sesión remota y local — best-effort: un fallo de red no impide la limpieza local
        runCatching { sessionRepository.logout() }
    }
}
