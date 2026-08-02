package cl.zzenner.cobranza.feature.auth

import cl.zzenner.cobranza.core.network.api.AuthApi
import cl.zzenner.cobranza.core.network.api.RespuestaToken
import cl.zzenner.cobranza.core.network.api.SolicitudRenovacion
import cl.zzenner.cobranza.core.security.InstallationIdStore
import cl.zzenner.cobranza.core.security.SecureTokenStore
import cl.zzenner.cobranza.feature.auth.data.SessionRepository
import cl.zzenner.cobranza.feature.auth.domain.AuthState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class SessionRepositoryTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var api: AuthApi
    private lateinit var secureTokenStore: SecureTokenStore
    private lateinit var installationIdStore: InstallationIdStore
    private lateinit var repository: SessionRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        api = mockk()
        secureTokenStore = mockk(relaxed = true)
        installationIdStore = mockk(relaxed = true)
        repository = SessionRepository(api, secureTokenStore, installationIdStore)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    // ── Access token en memoria ────────────────────────────────────────────────

    @Test
    fun `access token inicialmente nulo`() = runTest {
        assertNull(repository.getAccessToken())
    }

    @Test
    fun `guardar sesion almacena access token en memoria`() = runTest {
        val tokens = respuestaTokenValida()
        repository.guardarSesion(tokens, "usuario")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("access.token.valido", repository.getAccessToken())
    }

    // ── Almacenamiento y recuperación del refresh token ────────────────────────

    @Test
    fun `guardar sesion llama a saveRefreshToken del store`() = runTest {
        val tokens = respuestaTokenValida()
        repository.guardarSesion(tokens, "usuario")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { secureTokenStore.saveRefreshToken("refresh.token.valido") }
    }

    @Test
    fun `guardar sesion persiste sessionExpiresAt en installationIdStore`() = runTest {
        val tokens = respuestaTokenValida()
        repository.guardarSesion(tokens, "usuario")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { installationIdStore.saveSessionExpiresAt("2026-12-31T23:59:59Z") }
    }

    // ── Refresh exitoso ────────────────────────────────────────────────────────

    @Test
    fun `refresh exitoso actualiza ambos tokens`() = runTest {
        coEvery { secureTokenStore.getRefreshToken() } returns "refresh-viejo"
        coEvery { api.renovar(SolicitudRenovacion("refresh-viejo")) } returns
                Response.success(respuestaTokenValida())

        val resultado = repository.refreshTokens()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(resultado)
        assertEquals("access.token.valido", repository.getAccessToken())
        coVerify { secureTokenStore.saveRefreshToken("refresh.token.valido") }
    }

    // ── Rotación del refresh token ─────────────────────────────────────────────

    @Test
    fun `refresh exitoso guarda el nuevo refresh token rotado`() = runTest {
        coEvery { secureTokenStore.getRefreshToken() } returns "refresh-anterior"
        coEvery { api.renovar(any()) } returns Response.success(
            RespuestaToken("nuevo-access", "nuevo-refresh", 900, "2026-12-31T23:59:59Z")
        )

        repository.refreshTokens()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { secureTokenStore.saveRefreshToken("nuevo-refresh") }
    }

    // ── Refresh inválido limpia la sesión ─────────────────────────────────────

    @Test
    fun `refresh 401 limpia sesion`() = runTest {
        coEvery { secureTokenStore.getRefreshToken() } returns "refresh-expirado"
        coEvery { api.renovar(any()) } returns Response.error(
            401,
            "{}".toResponseBody("application/json".toMediaType())
        )

        val resultado = repository.refreshTokens()
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(resultado)
        coVerify { secureTokenStore.clearRefreshToken() }
        assertEquals(AuthState.NoAutenticado, repository.authState.value)
    }

    // ── Error de red NO borra sesión ───────────────────────────────────────────

    @Test
    fun `error de red durante refresh no limpia sesion`() = runTest {
        coEvery { secureTokenStore.getRefreshToken() } returns "refresh-valido"
        coEvery { api.renovar(any()) } throws java.io.IOException("sin red")

        val resultado = repository.refreshTokens()
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(resultado)
        // No debe borrar el refresh token
        coVerify(exactly = 0) { secureTokenStore.clearRefreshToken() }
    }

    // ── Sin refresh token ──────────────────────────────────────────────────────

    @Test
    fun `sin refresh token devuelve false y limpia sesion`() = runTest {
        coEvery { secureTokenStore.getRefreshToken() } returns null

        val resultado = repository.refreshTokens()
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(resultado)
        assertEquals(AuthState.NoAutenticado, repository.authState.value)
    }

    // ── clearSession ──────────────────────────────────────────────────────────

    @Test
    fun `clearSession elimina access token y refresh token`() = runTest {
        repository.guardarSesion(respuestaTokenValida(), "usuario")
        testDispatcher.scheduler.advanceUntilIdle()

        repository.clearSession()
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(repository.getAccessToken())
        coVerify { secureTokenStore.clearRefreshToken() }
        assertEquals(AuthState.NoAutenticado, repository.authState.value)
    }

    // ── Sesión vencida ────────────────────────────────────────────────────────

    @Test
    fun `sesion expirada en verificarSesionInicial limpia y muestra NoAutenticado`() = runTest {
        coEvery { installationIdStore.sessionExpiresAtFlow() } returns
                kotlinx.coroutines.flow.flowOf("2020-01-01T00:00:00Z")

        repository.verificarSesionInicial()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(AuthState.NoAutenticado, repository.authState.value)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun respuestaTokenValida() = RespuestaToken(
        accessToken = "access.token.valido",
        refreshToken = "refresh.token.valido",
        expiresInSeconds = 900,
        sessionExpiresAt = "2026-12-31T23:59:59Z",
    )
}
