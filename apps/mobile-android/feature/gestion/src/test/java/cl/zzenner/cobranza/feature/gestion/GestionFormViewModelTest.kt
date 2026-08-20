package cl.zzenner.cobranza.feature.gestion

import android.location.Location
import androidx.lifecycle.SavedStateHandle
import cl.zzenner.cobranza.core.database.dao.PersonaConDetalle
import cl.zzenner.cobranza.core.database.dao.PersonaDao
import cl.zzenner.cobranza.core.database.dao.PersonaDirectaDao
import cl.zzenner.cobranza.core.database.entity.PersonaDirectaEntity
import cl.zzenner.cobranza.core.database.entity.PersonaEntity
import cl.zzenner.cobranza.feature.gestion.data.GestionRepository
import cl.zzenner.cobranza.feature.gestion.domain.TipoGestion
import cl.zzenner.cobranza.feature.gestion.location.LocationProvider
import cl.zzenner.cobranza.feature.gestion.location.ResultadoUbicacion
import cl.zzenner.cobranza.feature.gestion.ui.GestionFormViewModel
import cl.zzenner.cobranza.feature.gestion.ui.PersonaIdentidadState
import cl.zzenner.cobranza.feature.gestion.worker.GestionSyncScheduler
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
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
 * Regresión: un doble-tap sobre "Registrar gestión" creaba dos gestiones locales
 * (dos UUID distintos) para la misma acción del usuario. isSubmitting vuelve a false
 * tan pronto el guardado local termina, antes de que la navegación de salida ocurra,
 * dejando una ventana donde el botón vuelve a estar habilitado. guardar() ahora también
 * se bloquea cuando guardadoExitoso ya es true.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GestionFormViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var personaDao: PersonaDao
    private lateinit var personaDirectaDao: PersonaDirectaDao
    private lateinit var repository: GestionRepository
    private lateinit var locationProvider: LocationProvider
    private lateinit var scheduler: GestionSyncScheduler
    private lateinit var viewModel: GestionFormViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        personaDao = mockk(relaxed = true)
        personaDirectaDao = mockk()
        repository = mockk()
        locationProvider = mockk()
        scheduler = mockk(relaxed = true)

        coEvery { personaDirectaDao.findById("persona-1") } returns PersonaDirectaEntity(
            id = "persona-1",
            rutNumero = "12345678",
            rutDv = "9",
            nombre = "Test Persona",
            versionContrato = 1,
            generadoEnEpoch = 0L,
            fechaConsultaEpoch = 0L,
            detalleJson = "{}",
        )

        val ubicacion: Location = mockk(relaxed = true)
        every { ubicacion.latitude } returns -33.45
        every { ubicacion.longitude } returns -70.66
        every { ubicacion.accuracy } returns 5.0f
        every { ubicacion.provider } returns "gps"
        every { ubicacion.time } returns 1000L
        coEvery { locationProvider.obtenerUbicacion() } returns ResultadoUbicacion.Exito(ubicacion)

        coEvery { repository.guardarLocal(any()) } returns "gestion-generada"

        val savedStateHandle = SavedStateHandle(
            mapOf("personaId" to "persona-1", "asignacionDiariaId" to null),
        )
        viewModel = GestionFormViewModel(
            savedStateHandle, personaDao, personaDirectaDao, repository, locationProvider, scheduler,
        )
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun prepararFormularioValido() {
        viewModel.onTipoGestionChanged(TipoGestion.SIN_CONTACTO)
        viewModel.capturarUbicacion()
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @Test
    fun `guardar exitoso persiste una sola gestion local`() = runTest {
        prepararFormularioValido()

        viewModel.guardar()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(true, viewModel.state.value.guardadoExitoso)
        coVerify(exactly = 1) { repository.guardarLocal(any()) }
    }

    @Test
    fun `doble tap tras guardado exitoso no crea una segunda gestion local`() = runTest {
        prepararFormularioValido()

        viewModel.guardar()
        testDispatcher.scheduler.advanceUntilIdle()
        // Segundo tap llega después de que el guardado ya terminó (guardadoExitoso=true),
        // simulando la ventana previa a que la UI navegue fuera de la pantalla.
        viewModel.guardar()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { repository.guardarLocal(any()) }
    }

    @Test
    fun `tap concurrente mientras isSubmitting es true no crea una segunda gestion local`() = runTest {
        prepararFormularioValido()

        viewModel.guardar()
        viewModel.guardar()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { repository.guardarLocal(any()) }
    }

    // --- DT-014: identidad de la persona visible en el formulario ---

    @Test
    fun `identidad disponible via busqueda directa expone nombre y rut`() = runTest {
        val identidad = viewModel.state.value.identidad as PersonaIdentidadState.Disponible

        assertEquals("Test Persona", identidad.nombre)
        assertEquals("12345678", identidad.rutNumero)
        assertEquals("9", identidad.rutDv)
    }

    @Test
    fun `identidad disponible via asignacion diaria expone nombre y rut`() = runTest {
        val personaConDetalle = PersonaConDetalle(
            persona = PersonaEntity(id = "persona-2", rutNumero = "9876543", rutDv = "K", nombre = "Otra Persona"),
            direcciones = emptyList(),
            avales = emptyList(),
            operaciones = emptyList(),
        )
        every { personaDao.getPersonaConDetalle("persona-2") } returns flowOf(personaConDetalle)

        val savedStateHandle = SavedStateHandle(
            mapOf("personaId" to "persona-2", "asignacionDiariaId" to "asignacion-1"),
        )
        val vmAsignacion = GestionFormViewModel(
            savedStateHandle, personaDao, personaDirectaDao, repository, locationProvider, scheduler,
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val identidad = vmAsignacion.state.value.identidad as PersonaIdentidadState.Disponible
        assertEquals("Otra Persona", identidad.nombre)
        assertEquals("9876543", identidad.rutNumero)
        assertEquals("K", identidad.rutDv)
    }

    @Test
    fun `persona no disponible bloquea guardar y no crea gestion local`() = runTest {
        coEvery { personaDirectaDao.findById("persona-ausente") } returns null

        val savedStateHandle = SavedStateHandle(
            mapOf("personaId" to "persona-ausente", "asignacionDiariaId" to null),
        )
        val vmSinPersona = GestionFormViewModel(
            savedStateHandle, personaDao, personaDirectaDao, repository, locationProvider, scheduler,
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(PersonaIdentidadState.NoDisponible, vmSinPersona.state.value.identidad)

        vmSinPersona.onTipoGestionChanged(TipoGestion.SIN_CONTACTO)
        vmSinPersona.capturarUbicacion()
        testDispatcher.scheduler.advanceUntilIdle()

        vmSinPersona.guardar()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vmSinPersona.state.value.errorGeneral != null)
        assertEquals(false, vmSinPersona.state.value.guardadoExitoso)
        coVerify(exactly = 0) { repository.guardarLocal(any()) }
    }
}
