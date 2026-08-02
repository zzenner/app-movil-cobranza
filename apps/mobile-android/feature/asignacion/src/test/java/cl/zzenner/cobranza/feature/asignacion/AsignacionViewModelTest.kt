package cl.zzenner.cobranza.feature.asignacion

import app.cash.turbine.test
import cl.zzenner.cobranza.core.database.dao.PersonaConDetalle
import cl.zzenner.cobranza.core.database.entity.PersonaEntity
import cl.zzenner.cobranza.core.database.entity.SyncMetadataEntity
import cl.zzenner.cobranza.feature.asignacion.data.AsignacionRepository
import cl.zzenner.cobranza.feature.asignacion.ui.AsignacionViewModel
import cl.zzenner.cobranza.feature.asignacion.worker.AsignacionSyncScheduler
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AsignacionViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: AsignacionRepository
    private lateinit var scheduler: AsignacionSyncScheduler

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        repository = mockk()
        scheduler = mockk(relaxed = true)
        every { repository.observePersonasDeAsignacion() } returns flowOf(emptyList())
        every { repository.observeSyncMetadata() } returns flowOf(null)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    private fun crearViewModel() = AsignacionViewModel(repository, scheduler)

    /** Obtiene el último estado emitido ignorando el estado inicial de carga */
    private suspend fun AsignacionViewModel.estadoFinal(): cl.zzenner.cobranza.feature.asignacion.ui.AsignacionUiState {
        var ultimo: cl.zzenner.cobranza.feature.asignacion.ui.AsignacionUiState? = null
        uiState.test {
            // Consume all available items
            while (true) {
                val event = awaitItem()
                ultimo = event
                // After the first non-loading item, we have the settled state
                if (!event.cargando || event.syncMetadata.estado != "NONE") break
            }
            cancelAndIgnoreRemainingEvents()
        }
        return ultimo!!
    }

    @Test
    fun `estado inicial tiene personas vacias`() = runTest {
        val vm = crearViewModel()
        vm.uiState.test {
            val estado = awaitItem()
            assertTrue(estado.personas.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `sincronizarManual llama al scheduler`() {
        crearViewModel().sincronizarManual()
        verify(exactly = 1) { scheduler.programarInmediato() }
    }

    @Test
    fun `busqueda filtra personas por RUT`() = runTest {
        val personas = listOf(
            personaConDetalle("p-1", "27000001", "0", "Juan Perez"),
            personaConDetalle("p-2", "18000000", "K", "Maria Soto"),
        )
        every { repository.observePersonasDeAsignacion() } returns flowOf(personas)
        val vm = crearViewModel()
        vm.onBusquedaChanged("2700000")

        vm.uiState.test {
            var estado = awaitItem()
            // May need to skip intermediate states
            while (estado.textoBusqueda.isBlank() && !estado.personas.any { "27" in it.rutFormateado }) {
                estado = awaitItem()
            }
            assertEquals(1, estado.personas.size)
            assertTrue(estado.personas[0].rutFormateado.contains("27"))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `metadata EXITOSA se mapea a SyncMetadataUi`() = runTest {
        every { repository.observeSyncMetadata() } returns flowOf(
            SyncMetadataEntity(
                estado = "EXITOSA",
                ultimaDescargaExitosa = 99999L,
                fechaAsignacionAlmacenada = "2026-08-02",
            ),
        )
        val vm = crearViewModel()

        vm.uiState.test {
            var estado = awaitItem()
            // Skip initial NONE state if needed
            if (estado.syncMetadata.estado == "NONE") estado = awaitItem()
            assertEquals("EXITOSA", estado.syncMetadata.estado)
            assertEquals(99999L, estado.syncMetadata.ultimaDescargaExitosa)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `metadata SIN_ASIGNACION con datos anteriores`() = runTest {
        every { repository.observeSyncMetadata() } returns flowOf(
            SyncMetadataEntity(
                estado = "SIN_ASIGNACION",
                datosMarcadosComoDesactualizados = true,
                datosAnterioresDisponibles = true,
                fechaAsignacionAlmacenada = "2026-08-01",
            ),
        )
        val vm = crearViewModel()

        vm.uiState.test {
            var estado = awaitItem()
            if (estado.syncMetadata.estado == "NONE") estado = awaitItem()
            assertEquals("SIN_ASIGNACION", estado.syncMetadata.estado)
            assertTrue(estado.syncMetadata.datosMarcadosComoDesactualizados)
            assertTrue(estado.syncMetadata.datosAnterioresDisponibles)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun personaConDetalle(id: String, rutNum: String, rutDv: String, nombre: String) =
        PersonaConDetalle(
            persona = PersonaEntity(id, rutNum, rutDv, nombre),
            direcciones = emptyList(),
            avales = emptyList(),
            operaciones = emptyList(),
        )
}
