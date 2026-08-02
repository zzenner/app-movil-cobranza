package cl.zzenner.cobranza.core.network

import cl.zzenner.cobranza.core.network.api.AuthApi
import cl.zzenner.cobranza.core.network.api.SolicitudLogin
import cl.zzenner.cobranza.core.network.api.SolicitudRenovacion
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class AuthApiTest {

    private val server = MockWebServer()
    private lateinit var api: AuthApi
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
        api = retrofit.create(AuthApi::class.java)
    }

    @After
    fun tearDown() = server.shutdown()

    // ── Serialización de SolicitudLogin ──────────────────────────────────────

    @Test
    fun `login envia identificadorInstalacion en el cuerpo`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(respuestaTokenJson())
                .addHeader("Content-Type", "application/json")
        )

        val idInstalacion = "550e8400-e29b-41d4-a716-446655440000"
        api.login(SolicitudLogin("usuario", "clave", idInstalacion))

        val request = server.takeRequest()
        val body = request.body.readUtf8()
        assertTrue("El cuerpo debe contener identificadorInstalacion", body.contains("identificadorInstalacion"))
        assertTrue("El cuerpo debe contener el UUID", body.contains(idInstalacion))
        assertFalse("El cuerpo NO debe contener dispositivoId", body.contains("dispositivoId"))
    }

    @Test
    fun `login exitoso devuelve RespuestaToken con campos correctos`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(respuestaTokenJson())
                .addHeader("Content-Type", "application/json")
        )

        val response = api.login(SolicitudLogin("usuario", "clave", "550e8400-e29b-41d4-a716-446655440000"))
        assertTrue(response.isSuccessful)
        val body = response.body()!!
        assertNotNull(body.accessToken)
        assertNotNull(body.refreshToken)
        assertTrue(body.expiresInSeconds > 0)
    }

    @Test
    fun `login con credenciales incorrectas devuelve 401`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setBody(problemDetailJson("CREDENCIALES_INCORRECTAS"))
                .addHeader("Content-Type", "application/problem+json")
        )

        val response = api.login(SolicitudLogin("malo", "malo", "550e8400-e29b-41d4-a716-446655440000"))
        assertFalse(response.isSuccessful)
        assertEquals(401, response.code())
    }

    @Test
    fun `login con dispositivo de otro usuario devuelve 409`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(409)
                .setBody(problemDetailJson("CONFLICTO_DISPOSITIVO"))
                .addHeader("Content-Type", "application/problem+json")
        )

        val response = api.login(SolicitudLogin("usuario", "clave", "550e8400-e29b-41d4-a716-446655440001"))
        assertEquals(409, response.code())
        val errorBody = response.errorBody()?.string()
        assertNotNull(errorBody)
        assertTrue(errorBody!!.contains("CONFLICTO_DISPOSITIVO"))
    }

    // ── Serialización de RespuestaToken ──────────────────────────────────────

    @Test
    fun `RespuestaToken se deserializa correctamente`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(respuestaTokenJson())
                .addHeader("Content-Type", "application/json")
        )

        val response = api.login(SolicitudLogin("u", "p", "550e8400-e29b-41d4-a716-446655440000"))
        val token = response.body()!!
        assertEquals("access.token.aqui", token.accessToken)
        assertEquals("refresh-token-aqui", token.refreshToken)
        assertEquals(900L, token.expiresInSeconds)
        assertEquals("2026-12-31T23:59:59Z", token.sessionExpiresAt)
    }

    // ── Serialización de SolicitudRenovacion ─────────────────────────────────

    @Test
    fun `renovar envia refreshToken en el cuerpo`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(respuestaTokenJson())
                .addHeader("Content-Type", "application/json")
        )

        api.renovar(SolicitudRenovacion("mi-refresh-token"))

        val request = server.takeRequest()
        val body = request.body.readUtf8()
        assertTrue(body.contains("refreshToken"))
        assertTrue(body.contains("mi-refresh-token"))
    }

    @Test
    fun `renovar con token invalido devuelve 401`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setBody(problemDetailJson("TOKEN_INVALIDO"))
                .addHeader("Content-Type", "application/problem+json")
        )

        val response = api.renovar(SolicitudRenovacion("token-expirado"))
        assertFalse(response.isSuccessful)
        assertEquals(401, response.code())
    }

    // ── ProblemDetail ─────────────────────────────────────────────────────────

    @Test
    fun `error body contiene code de ProblemDetail`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setBody(problemDetailJson("CREDENCIALES_INCORRECTAS"))
                .addHeader("Content-Type", "application/problem+json")
        )

        val response = api.login(SolicitudLogin("u", "p", "550e8400-e29b-41d4-a716-446655440000"))
        val errorJson = response.errorBody()?.string()
        assertNotNull(errorJson)
        val error = json.decodeFromString<cl.zzenner.cobranza.core.network.api.ApiError>(errorJson!!)
        assertEquals("CREDENCIALES_INCORRECTAS", error.code)
        assertEquals(401, error.status)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun respuestaTokenJson() = """
        {
            "accessToken": "access.token.aqui",
            "refreshToken": "refresh-token-aqui",
            "expiresInSeconds": 900,
            "sessionExpiresAt": "2026-12-31T23:59:59Z"
        }
    """.trimIndent()

    private fun problemDetailJson(code: String) = """
        {
            "status": ${if (code == "CONFLICTO_DISPOSITIVO") 409 else 401},
            "title": "Error de autenticación",
            "detail": "Operación no permitida",
            "code": "$code",
            "timestamp": "2026-08-02T12:00:00Z",
            "path": "/api/v1/auth/login"
        }
    """.trimIndent()
}
