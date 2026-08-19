package cl.zzenner.cobranza.ui

import cl.zzenner.cobranza.feature.asignacion.worker.AsignacionSyncScheduler
import cl.zzenner.cobranza.feature.auth.data.SessionRepository
import cl.zzenner.cobranza.feature.auth.domain.AuthState
import cl.zzenner.cobranza.feature.gestion.data.GestionRepository
import cl.zzenner.cobranza.feature.gestion.domain.ResultadoProcesamiento
import cl.zzenner.cobranza.feature.gestion.worker.GestionSyncScheduler
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Regresión DT-012: una gestión en ERROR_PERMANENTE/CONFLICTO no debe bloquear el logout
 * indefinidamente. Antes de este fix, `contarNoResueltas() > 0` bastaba para bloquear, sin
 * distinguir entre "el worker todavía puede enviarla sola" y "nadie la va a enviar sola".
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var sessionRepository: SessionRepository
    private lateinit var logoutUseCase: LogoutUseCase
    private lateinit var asignacionScheduler: AsignacionSyncScheduler
    private lateinit var gestionScheduler: GestionSyncScheduler
    private lateinit var gestionRepository: GestionRepository
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        sessionRepository = mockk {
            every { authState } returns MutableStateFlow(AuthState.Autenticado("ej_demo_133"))
        }
        logoutUseCase = mockk()
        coEvery { logoutUseCase.invoke() } just runs
        asignacionScheduler = mockk(relaxed = true)
        gestionScheduler = mockk(relaxed = true)
        gestionRepository = mockk()

        viewModel = HomeViewModel(
            sessionRepository, logoutUseCase, asignacionScheduler, gestionScheduler, gestionRepository,
        )
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `sin pendientes solicitarLogout cierra sesion de inmediato`() = runTest {
        coEvery { gestionRepository.contarReintentables() } returns 0
        coEvery { gestionRepository.contarNoRecuperables() } returns 0

        viewModel.solicitarLogout()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { logoutUseCase.invoke() }
        assertEquals(EstadoLogout.Inactivo, viewModel.estadoLogout.value)
    }

    @Test
    fun `reintentable bloquea logout y no cierra sesion`() = runTest {
        coEvery { gestionRepository.contarReintentables() } returns 1

        viewModel.solicitarLogout()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { logoutUseCase.invoke() }
        assertEquals(EstadoLogout.GestionesPendientes(1), viewModel.estadoLogout.value)
    }

    @Test
    fun `permanente sin reintentables no cierra sesion automaticamente y pide confirmacion`() = runTest {
        coEvery { gestionRepository.contarReintentables() } returns 0
        coEvery { gestionRepository.contarNoRecuperables() } returns 1

        viewModel.solicitarLogout()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { logoutUseCase.invoke() }
        assertEquals(EstadoLogout.ConfirmarLogoutConNoRecuperables(1), viewModel.estadoLogout.value)
    }

    @Test
    fun `combinacion reintentable mas permanente no salta la reintentable`() = runTest {
        coEvery { gestionRepository.contarReintentables() } returns 1
        coEvery { gestionRepository.contarNoRecuperables() } returns 1

        viewModel.solicitarLogout()
        testDispatcher.scheduler.advanceUntilIdle()

        // Debe quedar en el estado de "hay que sincronizar", NO saltar directo a "confirmar salida".
        assertEquals(EstadoLogout.GestionesPendientes(1), viewModel.estadoLogout.value)
        coVerify(exactly = 0) { gestionRepository.contarNoRecuperables() }
    }

    @Test
    fun `tras sincronizar sin reintentables pero con permanentes pide confirmacion`() = runTest {
        coEvery { gestionRepository.procesarOutbox() } returns ResultadoProcesamiento.Completado
        coEvery { gestionRepository.contarReintentables() } returns 0
        coEvery { gestionRepository.contarNoRecuperables() } returns 2

        viewModel.sincronizarYLogout()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { logoutUseCase.invoke() }
        assertEquals(EstadoLogout.ConfirmarLogoutConNoRecuperables(2), viewModel.estadoLogout.value)
    }

    @Test
    fun `tras sincronizar si siguen quedando reintentables no ofrece confirmar permanentes`() = runTest {
        coEvery { gestionRepository.procesarOutbox() } returns ResultadoProcesamiento.ErroresParciales(1, 1)
        coEvery { gestionRepository.contarReintentables() } returns 1

        viewModel.sincronizarYLogout()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.estadoLogout.value is EstadoLogout.ErrorSincronizacion)
        coVerify(exactly = 0) { gestionRepository.contarNoRecuperables() }
    }

    @Test
    fun `confirmarLogoutConNoRecuperables cierra sesion explicitamente`() = runTest {
        viewModel.confirmarLogoutConNoRecuperables()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { logoutUseCase.invoke() }
        assertEquals(EstadoLogout.Inactivo, viewModel.estadoLogout.value)
    }

    @Test
    fun `cancelarLogout no cierra sesion`() = runTest {
        coEvery { gestionRepository.contarReintentables() } returns 0
        coEvery { gestionRepository.contarNoRecuperables() } returns 1
        viewModel.solicitarLogout()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.cancelarLogout()

        coVerify(exactly = 0) { logoutUseCase.invoke() }
        assertEquals(EstadoLogout.Inactivo, viewModel.estadoLogout.value)
    }
}
