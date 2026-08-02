package cl.zzenner.cobranza.feature.asignacion

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import cl.zzenner.cobranza.feature.asignacion.data.AsignacionRepository
import cl.zzenner.cobranza.feature.asignacion.domain.ResultadoDescarga
import cl.zzenner.cobranza.feature.asignacion.worker.DescargaAsignacionWorker
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class DescargaAsignacionWorkerTest {

    private val context: Context = mockk(relaxed = true)
    private val params: WorkerParameters = mockk(relaxed = true)
    private val repository: AsignacionRepository = mockk()

    private fun crearWorker() = DescargaAsignacionWorker(context, params, repository)

    @Test
    fun `Exito retorna Result_success`() = runTest {
        coEvery { repository.descargarAsignacion() } returns ResultadoDescarga.Exito("2026-08-02")
        assertEquals(ListenableWorker.Result.success(), crearWorker().doWork())
    }

    @Test
    fun `SinAsignacion retorna Result_success`() = runTest {
        coEvery { repository.descargarAsignacion() } returns ResultadoDescarga.SinAsignacion("2026-08-02")
        assertEquals(ListenableWorker.Result.success(), crearWorker().doWork())
    }

    @Test
    fun `ErrorReintentar retorna Result_retry`() = runTest {
        coEvery { repository.descargarAsignacion() } returns ResultadoDescarga.ErrorReintentar("Sin red")
        assertEquals(ListenableWorker.Result.retry(), crearWorker().doWork())
    }

    @Test
    fun `ErrorPermanente retorna Result_failure`() = runTest {
        coEvery { repository.descargarAsignacion() } returns ResultadoDescarga.ErrorPermanente("403")
        assertEquals(ListenableWorker.Result.failure(), crearWorker().doWork())
    }

    @Test
    fun `VersionNoSoportada retorna Result_failure`() = runTest {
        coEvery { repository.descargarAsignacion() } returns ResultadoDescarga.VersionNoSoportada("v2")
        assertEquals(ListenableWorker.Result.failure(), crearWorker().doWork())
    }
}
