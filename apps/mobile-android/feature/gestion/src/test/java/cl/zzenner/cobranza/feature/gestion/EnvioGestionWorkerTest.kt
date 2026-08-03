package cl.zzenner.cobranza.feature.gestion

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import cl.zzenner.cobranza.feature.gestion.data.GestionRepository
import cl.zzenner.cobranza.feature.gestion.domain.ResultadoProcesamiento
import cl.zzenner.cobranza.feature.gestion.worker.EnvioGestionWorker
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EnvioGestionWorkerTest {

    private lateinit var context: Context
    private lateinit var repository: GestionRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        repository = mockk()
    }

    private fun buildWorker() = TestListenableWorkerBuilder<EnvioGestionWorker>(context)
        .setWorkerFactory(object : androidx.work.WorkerFactory() {
            override fun createWorker(
                appContext: Context,
                workerClassName: String,
                workerParameters: androidx.work.WorkerParameters,
            ) = EnvioGestionWorker(appContext, workerParameters, repository)
        })
        .build()

    @Test
    fun `Completado devuelve Result success`() = runTest {
        coEvery { repository.procesarOutbox() } returns ResultadoProcesamiento.Completado
        val result = buildWorker().doWork()
        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun `SinPendientes devuelve Result success`() = runTest {
        coEvery { repository.procesarOutbox() } returns ResultadoProcesamiento.SinPendientes
        val result = buildWorker().doWork()
        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun `ErroresParciales devuelve Result success (backoff gestionado por entidad)`() = runTest {
        coEvery { repository.procesarOutbox() } returns ResultadoProcesamiento.ErroresParciales(1, 3)
        val result = buildWorker().doWork()
        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun `SesionExpirada devuelve Result failure`() = runTest {
        coEvery { repository.procesarOutbox() } returns ResultadoProcesamiento.SesionExpirada
        val result = buildWorker().doWork()
        assertEquals(ListenableWorker.Result.failure(), result)
    }
}
