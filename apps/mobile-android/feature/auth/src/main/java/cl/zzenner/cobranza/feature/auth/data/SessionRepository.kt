package cl.zzenner.cobranza.feature.auth.data

import cl.zzenner.cobranza.core.network.api.AuthApi
import cl.zzenner.cobranza.core.network.api.RespuestaToken
import cl.zzenner.cobranza.core.network.api.SolicitudRenovacion
import cl.zzenner.cobranza.core.network.client.TokenProvider
import cl.zzenner.cobranza.core.security.InstallationIdStore
import cl.zzenner.cobranza.core.security.SecureTokenStore
import cl.zzenner.cobranza.feature.auth.domain.AuthState
import cl.zzenner.cobranza.feature.auth.domain.ErrorTipo
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import java.io.IOException
import java.time.Instant
import javax.inject.Inject

/**
 * Repositorio central de sesión. Responsabilidades:
 * - Mantener el access token en memoria (no persiste).
 * - Persistir el refresh token cifrado mediante [SecureTokenStore].
 * - Persistir sessionExpiresAt en [InstallationIdStore].
 * - Implementar [TokenProvider] para el cliente autenticado de red.
 * - Exponer [authState] como StateFlow para la navegación.
 *
 * NOTA: El nombre de usuario se conserva dentro del AuthState para la UI.
 * El usuario y contraseña NUNCA se almacenan.
 */
@ActivityRetainedScoped
class SessionRepository @Inject constructor(
    private val publicAuthApi: AuthApi,
    private val secureTokenStore: SecureTokenStore,
    private val installationIdStore: InstallationIdStore,
) : TokenProvider {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Verificando)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    @Volatile
    private var accessTokenInMemory: String? = null

    // ── TokenProvider ──────────────────────────────────────────────────────────

    override suspend fun getAccessToken(): String? = accessTokenInMemory

    /**
     * Intenta renovar los tokens usando el refresh token almacenado.
     * - Refresh 401/403: limpia la sesión.
     * - Error de red: NO limpia la sesión (puede ser transitorio).
     * @return true si la renovación fue exitosa.
     */
    override suspend fun refreshTokens(): Boolean {
        val refreshToken = secureTokenStore.getRefreshToken() ?: run {
            clearSession()
            return false
        }
        return try {
            val response = publicAuthApi.renovar(SolicitudRenovacion(refreshToken))
            if (response.isSuccessful) {
                val body = response.body()!!
                applyNewTokens(body)
                true
            } else if (response.code() == 401 || response.code() == 403) {
                clearSession()
                false
            } else {
                false
            }
        } catch (e: IOException) {
            // Error de red — sesión potencialmente válida aún; no limpiar
            false
        }
    }

    override suspend fun clearSession() {
        accessTokenInMemory = null
        secureTokenStore.clearRefreshToken()
        installationIdStore.clearSessionData()
        _authState.value = AuthState.NoAutenticado
    }

    // ── Session lifecycle ──────────────────────────────────────────────────────

    /**
     * Verifica si existe sesión al inicio de la app.
     * - sessionExpiresAt vencida → limpiar y pedir login.
     * - refresh token presente y sesión vigente → renovar silenciosamente.
     * - Sin refresh token → pedir login.
     */
    suspend fun verificarSesionInicial() {
        val expiresAt = installationIdStore.sessionExpiresAtFlow().first()
        if (expiresAt == null) {
            _authState.value = AuthState.NoAutenticado
            return
        }
        try {
            val expiry = Instant.parse(expiresAt)
            if (Instant.now().isAfter(expiry)) {
                clearSession()
                return
            }
        } catch (e: Exception) {
            clearSession()
            return
        }

        val renovado = refreshTokens()
        if (!renovado) {
            if (accessTokenInMemory == null) {
                _authState.value = AuthState.NoAutenticado
            }
        }
    }

    fun setAutenticando() {
        _authState.value = AuthState.Autenticando
    }

    fun setError(tipo: ErrorTipo, mensaje: String = "") {
        _authState.value = AuthState.Error(tipo, mensaje)
    }

    fun setNoAutenticado() {
        _authState.value = AuthState.NoAutenticado
    }

    suspend fun guardarSesion(tokens: RespuestaToken, nombreUsuario: String) {
        applyNewTokens(tokens)
        _authState.value = AuthState.Autenticado(nombreUsuario)
    }

    suspend fun logout() {
        try {
            publicAuthApi.logout()
        } catch (_: Exception) {
            // El logout remoto no es crítico; siempre limpiar localmente
        }
        clearSession()
    }

    private suspend fun applyNewTokens(tokens: RespuestaToken) {
        accessTokenInMemory = tokens.accessToken
        secureTokenStore.saveRefreshToken(tokens.refreshToken)
        installationIdStore.saveSessionExpiresAt(tokens.sessionExpiresAt)
    }
}
