package cl.zzenner.cobranza.core.network

import cl.zzenner.cobranza.core.network.api.GestionApi
import cl.zzenner.cobranza.core.network.api.SolicitudGestionDto
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Regresión: GestionApi.crearGestion() apuntaba a "gestiones" en vez de
 * "api/v1/gestiones", causando 404 real contra el backend en todo intento
 * de sincronización. Este test verifica la RUTA HTTP real (via MockWebServer),
 * no solo el comportamiento mockeado de la interfaz — un mock de interfaz
 * (como en GestionRepositoryTest) no puede detectar un path incorrecto.
 */
class GestionApiTest {

    private val server = MockWebServer()
    private lateinit var api: GestionApi
    private val json = Json { ignoreUnknownKeys = true }

    @Before
    fun setUp() {
        server.start()
        val client = OkHttpClient.Builder().build()
        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        api = retrofit.create(GestionApi::class.java)
    }

    @After
    fun tearDown() = server.shutdown()

    @Test
    fun `crearGestion envia la solicitud a api v1 gestiones`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(201)
                .setBody("""{"id":"$ID","resultado":"INSERTADA"}""")
                .addHeader("Content-Type", "application/json")
        )

        api.crearGestion(solicitudValida())

        val request = server.takeRequest()
        assertEquals("/api/v1/gestiones", request.path)
        assertEquals("POST", request.method)
    }

    @Test
    fun `crearGestion exitosa devuelve 201 con id y resultado`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(201)
                .setBody("""{"id":"$ID","resultado":"INSERTADA"}""")
                .addHeader("Content-Type", "application/json")
        )

        val response = api.crearGestion(solicitudValida())

        assertTrue(response.isSuccessful)
        assertEquals(201, response.code())
        assertEquals(ID, response.body()?.id)
        assertEquals("INSERTADA", response.body()?.resultado)
    }

    @Test
    fun `crearGestion idempotente devuelve 200`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"id":"$ID","resultado":"IDEMPOTENTE"}""")
                .addHeader("Content-Type", "application/json")
        )

        val response = api.crearGestion(solicitudValida())

        assertEquals(200, response.code())
        assertEquals("IDEMPOTENTE", response.body()?.resultado)
    }

    @Test
    fun `crearGestion con mismo id y contenido distinto devuelve 409`() = runTest {
        server.enqueue(MockResponse().setResponseCode(409))

        val response = api.crearGestion(solicitudValida())

        assertEquals(409, response.code())
    }

    private fun solicitudValida() = SolicitudGestionDto(
        id = ID,
        personaId = "persona-1",
        origenGestion = "ASIGNACION_DIARIA",
        asignacionDiariaId = "asignacion-1",
        tipoGestion = "SIN_CONTACTO",
        fechaGestion = "2026-08-16T23:00:00Z",
        fechaCapturaGps = "2026-08-16T23:00:00Z",
        observacion = null,
        observacionDireccion = null,
        latitud = -33.4569,
        longitud = -70.6483,
        precisionMetros = 5.0,
        ubicacionSimulada = true,
        proveedorGps = "gps",
        fechaCompromiso = null,
    )

    private companion object {
        const val ID = "550e8400-e29b-41d4-a716-446655440000"
    }
}
