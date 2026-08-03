package cl.zzenner.cobranza.ui

import cl.zzenner.cobranza.core.database.transaction.BundleReplacementTransaction
import cl.zzenner.cobranza.feature.asignacion.worker.AsignacionSyncScheduler
import cl.zzenner.cobranza.feature.auth.data.SessionRepository
import cl.zzenner.cobranza.feature.gestion.worker.GestionSyncScheduler
import javax.inject.Inject

/**
 * Coordina el logout completo: cancela workers, limpia BD local y cierra la sesión remota.
 * Reside en :app para poder acceder a todas las features sin crear dependencias entre ellas.
 */
class LogoutUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val bundleTransaction: BundleReplacementTransaction,
    private val asignacionScheduler: AsignacionSyncScheduler,
    private val gestionScheduler: GestionSyncScheduler,
) {
    suspend operator fun invoke() {
        asignacionScheduler.cancelarTodo()
        gestionScheduler.cancelarTodo()
        bundleTransaction.limpiarTodo()
        runCatching { sessionRepository.logout() }
    }
}
