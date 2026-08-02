package cl.zzenner.cobranza.feature.asignacion

import cl.zzenner.cobranza.core.database.dao.AsignacionDiariaDao
import cl.zzenner.cobranza.core.database.dao.PersonaDao
import cl.zzenner.cobranza.core.database.dao.SyncMetadataDao
import cl.zzenner.cobranza.core.database.entity.SyncMetadataEntity
import cl.zzenner.cobranza.core.database.transaction.BundleReplacementTransaction
import cl.zzenner.cobranza.core.database.transaction.ResultadoPersistencia
import cl.zzenner.cobranza.core.network.api.RespuestaAsignacionDiariaDto
import cl.zzenner.cobranza.core.network.api.SincronizacionApi
import cl.zzenner.cobranza.feature.asignacion.data.AsignacionMapper
import cl.zzenner.cobranza.feature.asignacion.data.AsignacionRepository
import cl.zzenner.cobranza.feature.asignacion.domain.ResultadoDescarga
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class AsignacionRepositoryTest {

    private lateinit var api: SincronizacionApi
    private lateinit var bundleTransaction: BundleReplacementTransaction
    private lateinit var asignacionDao: AsignacionDiariaDao
    private lateinit var personaDao: PersonaDao
    private lateinit var syncMetadataDao: SyncMetadataDao
    private lateinit var mapper: AsignacionMapper
    private lateinit var repository: AsignacionRepository

    @Before
    fun setup() {
        api = mockk()
        bundleTransaction = mockk(relaxed = true)
        asignacionDao = mockk(relaxed = true)
        personaDao = mockk(relaxed = true)
        syncMetadataDao = mockk(relaxed = true)
        mapper = mockk()

        every { asignacionDao.observeActiva() } returns flowOf(null)
        every { syncMetadataDao.observeMetadata() } returns flowOf(null)
        coEvery { syncMetadataDao.getMetadata() } returns null

        repository = AsignacionRepository(
            sincronizacionApi = api,
            bundleTransaction = bundleTransaction,
            asignacionDiariaDao = asignacionDao,
            personaDao = personaDao,
            syncMetadataDao = syncMetadataDao,
            mapper = mapper,
        )
    }

    private fun dtoFake() = mockk<RespuestaAsignacionDiariaDto>(relaxed = true) {
        every { id } returns "asig-1"
        every { fecha } returns "2026-08-02"
        every { ejecutivoId } returns "eje-1"
        every { estado } returns "PUBLICADA"
        every { personas } returns emptyList()
    }

    @Test
    fun `200 llama a reemplazar y retorna Exito`() = runTest {
        val bundle = mockk<cl.zzenner.cobranza.core.database.transaction.BundleDescargado>(relaxed = true) {
            every { asignacion } returns mockk(relaxed = true) { every { fecha } returns "2026-08-02" }
        }
        coEvery { api.obtenerAsignacionDiariaActiva(null) } returns Response.success(dtoFake())
        every { mapper.map(any(), any()) } returns bundle
        coEvery { bundleTransaction.reemplazar(any()) } returns ResultadoPersistencia.Exito

        val result = repository.descargarAsignacion()

        assertTrue(result is ResultadoDescarga.Exito)
        coVerify(exactly = 1) { bundleTransaction.reemplazar(bundle) }
    }

    @Test
    fun `204 llama a marcarSinAsignacion y retorna SinAsignacion`() = runTest {
        val response204: Response<RespuestaAsignacionDiariaDto> = mockk {
            every { code() } returns 204
            every { isSuccessful } returns true
            every { body() } returns null
        }
        coEvery { api.obtenerAsignacionDiariaActiva(null) } returns response204

        val result = repository.descargarAsignacion()

        assertTrue(result is ResultadoDescarga.SinAsignacion)
        coVerify(exactly = 1) { bundleTransaction.marcarSinAsignacion(any()) }
    }

    @Test
    fun `IOException retorna ErrorReintentar`() = runTest {
        coEvery { api.obtenerAsignacionDiariaActiva(null) } throws IOException("Sin red")

        val result = repository.descargarAsignacion()

        assertTrue(result is ResultadoDescarga.ErrorReintentar)
    }

    @Test
    fun `500 retorna ErrorReintentar`() = runTest {
        coEvery { api.obtenerAsignacionDiariaActiva(null) } returns
            Response.error(500, "Internal Server Error".toResponseBody())

        val result = repository.descargarAsignacion()

        assertTrue(result is ResultadoDescarga.ErrorReintentar)
    }

    @Test
    fun `403 retorna ErrorPermanente`() = runTest {
        coEvery { api.obtenerAsignacionDiariaActiva(null) } returns
            Response.error(403, "Forbidden".toResponseBody())

        val result = repository.descargarAsignacion()

        assertTrue(result is ResultadoDescarga.ErrorPermanente)
    }

    @Test
    fun `401 retorna ErrorPermanente`() = runTest {
        coEvery { api.obtenerAsignacionDiariaActiva(null) } returns
            Response.error(401, "Unauthorized".toResponseBody())

        val result = repository.descargarAsignacion()

        assertTrue(result is ResultadoDescarga.ErrorPermanente)
    }

    @Test
    fun `SerializationException retorna ErrorPermanente`() = runTest {
        coEvery { api.obtenerAsignacionDiariaActiva(null) } returns Response.success(dtoFake())
        every { mapper.map(any(), any()) } throws
            kotlinx.serialization.SerializationException("JSON inválido")

        val result = repository.descargarAsignacion()

        assertTrue(result is ResultadoDescarga.ErrorPermanente)
    }
}
