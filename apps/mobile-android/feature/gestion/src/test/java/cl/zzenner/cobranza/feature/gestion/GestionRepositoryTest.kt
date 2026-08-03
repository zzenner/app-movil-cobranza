package cl.zzenner.cobranza.feature.gestion

import cl.zzenner.cobranza.core.database.dao.GestionHistoricaDao
import cl.zzenner.cobranza.core.database.dao.GestionLocalDao
import cl.zzenner.cobranza.core.database.entity.GestionLocalEntity
import cl.zzenner.cobranza.core.network.api.GestionApi
import cl.zzenner.cobranza.core.network.api.RespuestaGestionDto
import cl.zzenner.cobranza.feature.gestion.data.GestionRepository
import cl.zzenner.cobranza.feature.gestion.domain.EstadoSincronizacion
import cl.zzenner.cobranza.feature.gestion.domain.ResultadoProcesamiento
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class GestionRepositoryTest {

    private val dao: GestionLocalDao = mockk(relaxed = true)
    private val historicaDao: GestionHistoricaDao = mockk {
        every { getGestionesDePersona(any()) } returns flowOf(emptyList())
    }
    private val api: GestionApi = mockk()
    private val repository = GestionRepository(dao, historicaDao, api)

    private fun entityPendiente(id: String = "g-1") = GestionLocalEntity(
        id = id,
        personaId = "p-1",
        personaRutNumero = "12345678",
        personaRutDv = "9",
        personaNombre = "Test",
        asignacionDiariaId = "asig-1",
        origenGestion = "ASIGNACION_DIARIA",
        tipoGestion = "SIN_CONTACTO",
        fechaGestionEpoch = 1000L,
        fechaCapturaGpsEpoch = 1000L,
        observacion = null,
        observacionDireccion = null,
        latitud = -33.45,
        longitud = -70.66,
        precisionMetros = 5.0f,
        ubicacionSimulada = false,
        proveedorGps = "gps",
        fechaCompromiso = null,
        estadoSincronizacion = "PENDIENTE_ENVIO",
        fechaCreacionLocalEpoch = 1000L,
        cantidadIntentos = 0,
        leaseHastaEpoch = null,
        fechaProximoIntentoEpoch = null,
        codigoErrorServidor = null,
        mensajeError = null,
    )

    @Test
    fun `procesarOutbox sin elegibles devuelve SinPendientes`() = runTest {
        coEvery { dao.recuperarLeasesExpirados(any()) } returns 0
        coEvery { dao.getElegibles(any(), any()) } returns emptyList()

        val resultado = repository.procesarOutbox()

        assertEquals(ResultadoProcesamiento.SinPendientes, resultado)
    }

    @Test
    fun `procesarOutbox con respuesta 201 marca SINCRONIZADA`() = runTest {
        coEvery { dao.recuperarLeasesExpirados(any()) } returns 0
        coEvery { dao.getElegibles(any(), any()) } returns listOf(entityPendiente())
        coEvery { dao.adquirirLease(any(), any(), any()) } returns 1
        coEvery { api.crearGestion(any()) } returns Response.success(
            201,
            RespuestaGestionDto("g-1", "INSERTADA"),
        )
        coEvery { dao.actualizarEstado(any(), any(), any(), any(), any(), any()) } returns Unit

        val resultado = repository.procesarOutbox()

        assertEquals(ResultadoProcesamiento.Completado, resultado)
        coVerify {
            dao.actualizarEstado("g-1", EstadoSincronizacion.SINCRONIZADA.name, 0, null, null, null)
        }
    }

    @Test
    fun `procesarOutbox con respuesta 200 marca SINCRONIZADA (idempotencia)`() = runTest {
        coEvery { dao.recuperarLeasesExpirados(any()) } returns 0
        coEvery { dao.getElegibles(any(), any()) } returns listOf(entityPendiente())
        coEvery { dao.adquirirLease(any(), any(), any()) } returns 1
        coEvery { api.crearGestion(any()) } returns Response.success(
            200,
            RespuestaGestionDto("g-1", "DUPLICADA"),
        )
        coEvery { dao.actualizarEstado(any(), any(), any(), any(), any(), any()) } returns Unit

        repository.procesarOutbox()

        coVerify {
            dao.actualizarEstado("g-1", EstadoSincronizacion.SINCRONIZADA.name, 0, null, null, null)
        }
    }

    @Test
    fun `procesarOutbox con 401 libera lease y retorna SesionExpirada`() = runTest {
        coEvery { dao.recuperarLeasesExpirados(any()) } returns 0
        coEvery { dao.getElegibles(any(), any()) } returns listOf(entityPendiente())
        coEvery { dao.adquirirLease(any(), any(), any()) } returns 1
        coEvery { api.crearGestion(any()) } returns Response.error(401, "".toResponseBody())
        coEvery { dao.actualizarEstado(any(), any(), any(), any(), any(), any()) } returns Unit

        val resultado = repository.procesarOutbox()

        assertEquals(ResultadoProcesamiento.SesionExpirada, resultado)
        coVerify {
            dao.actualizarEstado(
                "g-1", EstadoSincronizacion.PENDIENTE_ENVIO.name, 0, null, null, null,
            )
        }
    }

    @Test
    fun `procesarOutbox con 5xx marca ERROR_REINTENTABLE con backoff`() = runTest {
        coEvery { dao.recuperarLeasesExpirados(any()) } returns 0
        coEvery { dao.getElegibles(any(), any()) } returns listOf(entityPendiente())
        coEvery { dao.adquirirLease(any(), any(), any()) } returns 1
        coEvery { api.crearGestion(any()) } returns Response.error(500, "".toResponseBody())
        coEvery { dao.actualizarEstado(any(), any(), any(), any(), any(), any()) } returns Unit

        val resultado = repository.procesarOutbox()

        assertTrue(resultado is ResultadoProcesamiento.ErroresParciales)
        coVerify {
            dao.actualizarEstado(
                "g-1",
                EstadoSincronizacion.ERROR_REINTENTABLE.name,
                1,
                any(),
                "HTTP_500",
                any(),
            )
        }
    }

    @Test
    fun `procesarOutbox con CAS perdido (adquirirLease=0) salta sin enviar`() = runTest {
        coEvery { dao.recuperarLeasesExpirados(any()) } returns 0
        coEvery { dao.getElegibles(any(), any()) } returns listOf(entityPendiente())
        coEvery { dao.adquirirLease(any(), any(), any()) } returns 0

        val resultado = repository.procesarOutbox()

        assertEquals(ResultadoProcesamiento.Completado, resultado)
        coVerify(exactly = 0) { api.crearGestion(any()) }
    }

    @Test
    fun `procesarOutbox con 422 marca ERROR_PERMANENTE`() = runTest {
        coEvery { dao.recuperarLeasesExpirados(any()) } returns 0
        coEvery { dao.getElegibles(any(), any()) } returns listOf(entityPendiente())
        coEvery { dao.adquirirLease(any(), any(), any()) } returns 1
        coEvery { api.crearGestion(any()) } returns Response.error(422, "".toResponseBody())
        coEvery { dao.actualizarEstado(any(), any(), any(), any(), any(), any()) } returns Unit

        repository.procesarOutbox()

        coVerify {
            dao.actualizarEstado(
                "g-1", EstadoSincronizacion.ERROR_PERMANENTE.name, 1, null, "VALIDACION", any(),
            )
        }
    }

    @Test
    fun `procesarOutbox con 400 marca ERROR_PERMANENTE`() = runTest {
        coEvery { dao.recuperarLeasesExpirados(any()) } returns 0
        coEvery { dao.getElegibles(any(), any()) } returns listOf(entityPendiente())
        coEvery { dao.adquirirLease(any(), any(), any()) } returns 1
        coEvery { api.crearGestion(any()) } returns Response.error(400, "".toResponseBody())
        coEvery { dao.actualizarEstado(any(), any(), any(), any(), any(), any()) } returns Unit

        repository.procesarOutbox()

        coVerify {
            dao.actualizarEstado(
                "g-1", EstadoSincronizacion.ERROR_PERMANENTE.name, 1, null, "HTTP_400", any(),
            )
        }
    }

    @Test
    fun `procesarOutbox con 403 marca ERROR_PERMANENTE`() = runTest {
        coEvery { dao.recuperarLeasesExpirados(any()) } returns 0
        coEvery { dao.getElegibles(any(), any()) } returns listOf(entityPendiente())
        coEvery { dao.adquirirLease(any(), any(), any()) } returns 1
        coEvery { api.crearGestion(any()) } returns Response.error(403, "".toResponseBody())
        coEvery { dao.actualizarEstado(any(), any(), any(), any(), any(), any()) } returns Unit

        repository.procesarOutbox()

        coVerify {
            dao.actualizarEstado(
                "g-1", EstadoSincronizacion.ERROR_PERMANENTE.name, 1, null, "HTTP_403", any(),
            )
        }
    }

    @Test
    fun `procesarOutbox con 404 marca ERROR_PERMANENTE`() = runTest {
        coEvery { dao.recuperarLeasesExpirados(any()) } returns 0
        coEvery { dao.getElegibles(any(), any()) } returns listOf(entityPendiente())
        coEvery { dao.adquirirLease(any(), any(), any()) } returns 1
        coEvery { api.crearGestion(any()) } returns Response.error(404, "".toResponseBody())
        coEvery { dao.actualizarEstado(any(), any(), any(), any(), any(), any()) } returns Unit

        repository.procesarOutbox()

        coVerify {
            dao.actualizarEstado(
                "g-1", EstadoSincronizacion.ERROR_PERMANENTE.name, 1, null, "HTTP_404", any(),
            )
        }
    }
}
