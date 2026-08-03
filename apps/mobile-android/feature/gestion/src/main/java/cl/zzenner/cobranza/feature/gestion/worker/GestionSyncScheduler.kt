package cl.zzenner.cobranza.feature.gestion.worker

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GestionSyncScheduler @Inject constructor(
    private val workManager: WorkManager,
) {

    private val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    fun programarEnvioInmediato() {
        val request = OneTimeWorkRequestBuilder<EnvioGestionWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        workManager.enqueueUniqueWork(
            EnvioGestionWorker.NOMBRE_UNICO_INMEDIATO,
            ExistingWorkPolicy.KEEP,
            request,
        )
    }

    fun programarPeriodico() {
        val request = PeriodicWorkRequestBuilder<EnvioGestionWorker>(1, TimeUnit.HOURS)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        workManager.enqueueUniquePeriodicWork(
            EnvioGestionWorker.NOMBRE_UNICO_PERIODICO,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    fun cancelarTodo() {
        workManager.cancelUniqueWork(EnvioGestionWorker.NOMBRE_UNICO_INMEDIATO)
        workManager.cancelUniqueWork(EnvioGestionWorker.NOMBRE_UNICO_PERIODICO)
    }
}
