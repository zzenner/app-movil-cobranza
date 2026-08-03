package cl.zzenner.cobranza.ui

import cl.zzenner.cobranza.core.database.transaction.BundleReplacementTransaction
import cl.zzenner.cobranza.feature.asignacion.worker.AsignacionSyncScheduler
import cl.zzenner.cobranza.feature.auth.data.SessionRepository
import cl.zzenner.cobranza.feature.gestion.worker.GestionSyncScheduler
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.test.runTest
import org.junit.Test

class LogoutUseCaseTest {

    private val sessionRepository: SessionRepository = mockk {
        io.mockk.coEvery { logout() } just runs
    }
    private val bundleTransaction: BundleReplacementTransaction = mockk {
        io.mockk.coEvery { limpiarTodo() } just runs
    }
    private val asignacionScheduler: AsignacionSyncScheduler = mockk {
        io.mockk.every { cancelarTodo() } just runs
    }
    private val gestionScheduler: GestionSyncScheduler = mockk {
        io.mockk.every { cancelarTodo() } just runs
    }

    private val useCase = LogoutUseCase(
        sessionRepository, bundleTransaction, asignacionScheduler, gestionScheduler,
    )

    @Test
    fun `logout cancela ambos schedulers antes de limpiar room`() = runTest {
        useCase()

        coVerifyOrder {
            asignacionScheduler.cancelarTodo()
            gestionScheduler.cancelarTodo()
            bundleTransaction.limpiarTodo()
            sessionRepository.logout()
        }
    }

    @Test
    fun `logout cancela asignacion scheduler exactamente una vez`() = runTest {
        useCase()
        io.mockk.verify(exactly = 1) { asignacionScheduler.cancelarTodo() }
    }

    @Test
    fun `logout cancela gestion scheduler exactamente una vez`() = runTest {
        useCase()
        io.mockk.verify(exactly = 1) { gestionScheduler.cancelarTodo() }
    }

    @Test
    fun `logout llama limpiarTodo exactamente una vez`() = runTest {
        useCase()
        coVerify(exactly = 1) { bundleTransaction.limpiarTodo() }
    }
}
