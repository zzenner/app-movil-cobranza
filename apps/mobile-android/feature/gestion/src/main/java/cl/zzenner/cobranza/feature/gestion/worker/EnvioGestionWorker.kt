package cl.zzenner.cobranza.feature.gestion.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import cl.zzenner.cobranza.feature.gestion.data.GestionRepository
import cl.zzenner.cobranza.feature.gestion.domain.ResultadoProcesamiento
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class EnvioGestionWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: GestionRepository,
) : CoroutineWorker(context, params) {

    companion object {
        const val NOMBRE_UNICO_INMEDIATO = "envio_gestion_inmediato"
        const val NOMBRE_UNICO_PERIODICO = "envio_gestion_periodico"
    }

    override suspend fun doWork(): Result {
        return when (repository.procesarOutbox()) {
            ResultadoProcesamiento.Completado -> Result.success()
            ResultadoProcesamiento.SinPendientes -> Result.success()
            is ResultadoProcesamiento.ErroresParciales -> Result.success()
            ResultadoProcesamiento.SesionExpirada -> Result.failure()
        }
    }
}
