package cl.zzenner.cobranza.ui

import cl.zzenner.cobranza.core.database.transaction.BundleReplacementTransaction
import cl.zzenner.cobranza.feature.asignacion.worker.AsignacionSyncScheduler
import cl.zzenner.cobranza.feature.auth.data.SessionRepository
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
    private val scheduler: AsignacionSyncScheduler = mockk {
        io.mockk.every { cancelarTodo() } just runs
    }

    private val useCase = LogoutUseCase(sessionRepository, bundleTransaction, scheduler)

    @Test
    fun `logout cancela workers antes de limpiar room`() = runTest {
        useCase()

        coVerifyOrder {
            scheduler.cancelarTodo()
            bundleTransaction.limpiarTodo()
            sessionRepository.logout()
        }
    }

    @Test
    fun `logout llama cancelarTodo exactamente una vez`() = runTest {
        useCase()

        io.mockk.verify(exactly = 1) { scheduler.cancelarTodo() }
    }

    @Test
    fun `logout llama limpiarTodo exactamente una vez`() = runTest {
        useCase()

        coVerify(exactly = 1) { bundleTransaction.limpiarTodo() }
    }

    @Test
    fun `logout llama sessionRepository logout exactamente una vez`() = runTest {
        useCase()

        coVerify(exactly = 1) { sessionRepository.logout() }
    }
}
