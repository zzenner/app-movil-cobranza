package cl.zzenner.cobranza.feature.asignacion.worker

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
class AsignacionSyncScheduler @Inject constructor(
    private val workManager: WorkManager,
) {

    /** Descarga inmediata (KEEP — no reemplaza si ya hay una encolada) */
    fun programarInmediato() {
        val request = OneTimeWorkRequestBuilder<DescargaAsignacionWorker>()
            .setConstraints(Constraints(requiredNetworkType = NetworkType.CONNECTED))
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30L, TimeUnit.SECONDS)
            .build()
        workManager.enqueueUniqueWork(
            DescargaAsignacionWorker.NOMBRE_UNICO_INMEDIATO,
            ExistingWorkPolicy.KEEP,
            request,
        )
    }

    /** Descarga periódica cada 4 horas (KEEP — respeta el work existente) */
    fun programarPeriodico() {
        val request = PeriodicWorkRequestBuilder<DescargaAsignacionWorker>(4, TimeUnit.HOURS)
            .setConstraints(Constraints(requiredNetworkType = NetworkType.CONNECTED))
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30L, TimeUnit.SECONDS)
            .build()
        workManager.enqueueUniquePeriodicWork(
            DescargaAsignacionWorker.NOMBRE_UNICO_PERIODICO,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    fun cancelarTodo() {
        workManager.cancelUniqueWork(DescargaAsignacionWorker.NOMBRE_UNICO_INMEDIATO)
        workManager.cancelUniqueWork(DescargaAsignacionWorker.NOMBRE_UNICO_PERIODICO)
    }
}
