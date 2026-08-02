package cl.zzenner.cobranza.feature.asignacion.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import cl.zzenner.cobranza.feature.asignacion.data.AsignacionRepository
import cl.zzenner.cobranza.feature.asignacion.domain.ResultadoDescarga
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class DescargaAsignacionWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: AsignacionRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return when (repository.descargarAsignacion()) {
            is ResultadoDescarga.Exito -> Result.success()
            is ResultadoDescarga.SinAsignacion -> Result.success()
            is ResultadoDescarga.ErrorReintentar -> Result.retry()
            is ResultadoDescarga.ErrorPermanente -> Result.failure()
            is ResultadoDescarga.VersionNoSoportada -> Result.failure()
        }
    }

    companion object {
        const val NOMBRE_UNICO_INMEDIATO = "descarga_asignacion_inmediata"
        const val NOMBRE_UNICO_PERIODICO = "descarga_asignacion_periodica"
    }
}
