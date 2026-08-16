package cl.zzenner.cobranza.feature.auth

import app.cash.turbine.test
import cl.zzenner.cobranza.core.network.api.ApiError
import cl.zzenner.cobranza.core.network.api.AuthApi
import cl.zzenner.cobranza.core.network.api.RespuestaToken
import cl.zzenner.cobranza.core.network.api.SolicitudLogin
import cl.zzenner.cobranza.core.security.InstallationIdStore
import cl.zzenner.cobranza.core.security.SecureTokenStore
import cl.zzenner.cobranza.feature.auth.data.SessionRepository
import cl.zzenner.cobranza.feature.auth.domain.AuthState
import cl.zzenner.cobranza.feature.auth.domain.ErrorTipo
import cl.zzenner.cobranza.feature.auth.ui.LoginViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var api: AuthApi
    private lateinit var sessionRepository: SessionRepository
    private lateinit var installationIdStore: InstallationIdStore
    private lateinit var secureTokenStore: SecureTokenStore
    private lateinit var viewModel: LoginViewModel
    private val json = Json { ignoreUnknownKeys = true }

    private val idInstalacion = "550e8400-e29b-41d4-a716-446655440000"

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        api = mockk()
        secureTokenStore = mockk(relaxed = true)
        installationIdStore = mockk(relaxed = true)
        coEvery { installationIdStore.getOrCreateInstallationId() } returns idInstalacion

        sessionRepository = SessionRepository(api, secureTokenStore, installationIdStore)
        viewModel = LoginViewModel(api, sessionRepository, installationIdStore, json)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── Validación del formulario ──────────────────────────────────────────────

    @Test
    fun `usuario vacio muestra error de campo`() = runTest {
        viewModel.onContrasenaChanged("clave123")
        viewModel.login()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("El nombre de usuario es obligatorio", viewModel.uiState.value.errorUsuario)
    }

    @Test
    fun `contrasena vacia muestra error de campo`() = runTest {
        viewModel.onUsuarioChanged("usuario")
        viewModel.login()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("La contraseña es obligatoria", viewModel.uiState.value.errorContrasena)
    }

    @Test
    fun `contrasena muy corta muestra error de longitud`() = runTest {
        viewModel.onUsuarioChanged("usuario")
        viewModel.onContrasenaChanged("abc")
        viewModel.login()
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.errorContrasena)
    }

    @Test
    fun `formulario invalido no llama a la API`() = runTest {
        viewModel.login()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { api.login(any()) }
    }

    // ── Login exitoso ──────────────────────────────────────────────────────────

    @Test
    fun `login exitoso actualiza estado a Autenticado`() = runTest {
        coEvery { api.login(any()) } returns Response.success(respuestaTokenValida())
        coEvery { secureTokenStore.saveRefreshToken(any()) } returns Unit
        coEvery { installationIdStore.saveSessionExpiresAt(any()) } returns Unit

        viewModel.authState.test {
            assertEquals(AuthState.Verificando::class, awaitItem()::class)

            viewModel.onUsuarioChanged("ejecutivo")
            viewModel.onContrasenaChanged("Clave.123!")
            viewModel.login()
            testDispatcher.scheduler.advanceUntilIdle()

            val autenticado = awaitItem() as? AuthState.Autenticado
                ?: awaitItem() as? AuthState.Autenticado
            assertNotNull(autenticado)
            assertEquals("ejecutivo", autenticado!!.nombreUsuario)
        }
    }

    @Test
    fun `login exitoso llama a la API con identificadorInstalacion`() = runTest {
        coEvery { api.login(any()) } returns Response.success(respuestaTokenValida())
        coEvery { secureTokenStore.saveRefreshToken(any()) } returns Unit
        coEvery { installationIdStore.saveSessionExpiresAt(any()) } returns Unit

        viewModel.onUsuarioChanged("ejecutivo")
        viewModel.onContrasenaChanged("Clave.123!")
        viewModel.login()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify {
            api.login(
                match { it.identificadorInstalacion == idInstalacion }
            )
        }
    }

    // ── Credenciales incorrectas ───────────────────────────────────────────────

    @Test
    fun `credenciales incorrectas devuelven estado Error`() = runTest {
        val errorJson = """{"status":401,"code":"CREDENCIALES_INCORRECTAS","title":"","detail":"","timestamp":"","path":""}"""
        coEvery { api.login(any()) } returns Response.error(
            401,
            errorJson.toResponseBody("application/problem+json".toMediaType())
        )

        viewModel.onUsuarioChanged("malo")
        viewModel.onContrasenaChanged("clave-mala")
        viewModel.login()
        testDispatcher.scheduler.advanceUntilIdle()

        val estado = viewModel.authState.value as? AuthState.Error
        assertEquals(ErrorTipo.CREDENCIALES_INCORRECTAS, estado?.tipo)
    }

    // ── Conflicto de dispositivo ───────────────────────────────────────────────

    @Test
    fun `conflicto de dispositivo devuelve estado Error DISPOSITIVO_CONFLICTO`() = runTest {
        val errorJson = """{"status":409,"code":"CONFLICTO_DISPOSITIVO","title":"","detail":"","timestamp":"","path":""}"""
        coEvery { api.login(any()) } returns Response.error(
            409,
            errorJson.toResponseBody("application/problem+json".toMediaType())
        )

        viewModel.onUsuarioChanged("usuario")
        viewModel.onContrasenaChanged("Clave.123!")
        viewModel.login()
        testDispatcher.scheduler.advanceUntilIdle()

        val estado = viewModel.authState.value as? AuthState.Error
        assertEquals(ErrorTipo.DISPOSITIVO_CONFLICTO, estado?.tipo)
    }

    // ── Error de red ───────────────────────────────────────────────────────────

    @Test
    fun `error de red devuelve estado Error SIN_CONEXION`() = runTest {
        coEvery { api.login(any()) } throws java.io.IOException("sin red")

        viewModel.onUsuarioChanged("usuario")
        viewModel.onContrasenaChanged("Clave.123!")
        viewModel.login()
        testDispatcher.scheduler.advanceUntilIdle()

        val estado = viewModel.authState.value as? AuthState.Error
        assertEquals(ErrorTipo.SIN_CONEXION, estado?.tipo)
    }

    @Test
    fun `servidor inaccesible devuelve estado Error ERROR_SERVIDOR`() = runTest {
        coEvery { api.login(any()) } throws java.net.ConnectException("Connection refused")

        viewModel.onUsuarioChanged("usuario")
        viewModel.onContrasenaChanged("Clave.123!")
        viewModel.login()
        testDispatcher.scheduler.advanceUntilIdle()

        val estado = viewModel.authState.value as? AuthState.Error
        assertEquals(ErrorTipo.ERROR_SERVIDOR, estado?.tipo)
    }

    // ── Logout ─────────────────────────────────────────────────────────────────

    @Test
    fun `logout limpia sesion y regresa a NoAutenticado`() = runTest {
        // Primero establecer una sesión
        coEvery { api.login(any()) } returns Response.success(respuestaTokenValida())
        coEvery { api.logout(any()) } returns Response.success(Unit)
        coEvery { secureTokenStore.saveRefreshToken(any()) } returns Unit
        coEvery { installationIdStore.saveSessionExpiresAt(any()) } returns Unit
        coEvery { secureTokenStore.clearRefreshToken() } returns Unit
        coEvery { installationIdStore.clearSessionData() } returns Unit

        viewModel.onUsuarioChanged("usuario")
        viewModel.onContrasenaChanged("Clave.123!")
        viewModel.login()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.logout()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(AuthState.NoAutenticado, viewModel.authState.value)
    }

    // ── Restauración de sesión ─────────────────────────────────────────────────

    @Test
    fun `verificar sesion con sesion expirada navega a NoAutenticado`() = runTest {
        coEvery { installationIdStore.sessionExpiresAtFlow() } returns
                kotlinx.coroutines.flow.flowOf("2020-01-01T00:00:00Z")
        coEvery { secureTokenStore.clearRefreshToken() } returns Unit
        coEvery { installationIdStore.clearSessionData() } returns Unit

        viewModel.verificarSesion()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(AuthState.NoAutenticado, viewModel.authState.value)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun respuestaTokenValida() = RespuestaToken(
        accessToken = "access.token.valido",
        refreshToken = "refresh.token.valido",
        expiresInSeconds = 900,
        sessionExpiresAt = "2026-12-31T23:59:59Z",
    )
}
