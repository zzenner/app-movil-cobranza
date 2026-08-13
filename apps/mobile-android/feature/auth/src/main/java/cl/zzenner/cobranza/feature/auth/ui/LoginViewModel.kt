package cl.zzenner.cobranza.feature.auth.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.zzenner.cobranza.core.network.api.AuthApi
import cl.zzenner.cobranza.core.network.api.SolicitudLogin
import cl.zzenner.cobranza.core.network.api.ApiError
import cl.zzenner.cobranza.core.security.InstallationIdStore
import cl.zzenner.cobranza.feature.auth.data.SessionRepository
import cl.zzenner.cobranza.feature.auth.domain.AuthState
import cl.zzenner.cobranza.feature.auth.domain.ErrorTipo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import javax.inject.Inject

/**
 * ViewModel de login. Orquesta validación de campos, llamada a la API,
 * manejo de errores y navegación.
 *
 * El usuario y la contraseña se retienen SOLO en UiState (en memoria del ViewModel)
 * durante la sesión de entrada; nunca se persisten.
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val publicAuthApi: AuthApi,
    private val sessionRepository: SessionRepository,
    private val installationIdStore: InstallationIdStore,
    private val json: Json,
) : ViewModel() {

    val authState: StateFlow<AuthState> = sessionRepository.authState

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    // ── Verificación inicial ───────────────────────────────────────────────────

    fun verificarSesion() {
        viewModelScope.launch {
            sessionRepository.verificarSesionInicial()
        }
    }

    // ── Validación de campos ───────────────────────────────────────────────────

    fun onUsuarioChanged(value: String) {
        _uiState.value = _uiState.value.copy(
            usuario = value,
            errorUsuario = null,
        )
    }

    fun onContrasenaChanged(value: String) {
        _uiState.value = _uiState.value.copy(
            contrasena = value,
            errorContrasena = null,
        )
    }

    private fun validarFormulario(): Boolean {
        val state = _uiState.value
        var valido = true

        val errorUsuario = when {
            state.usuario.isBlank() -> "El nombre de usuario es obligatorio"
            else -> null
        }
        val errorContrasena = when {
            state.contrasena.isBlank() -> "La contraseña es obligatoria"
            state.contrasena.length < 6 -> "La contraseña debe tener al menos 6 caracteres"
            else -> null
        }

        if (errorUsuario != null || errorContrasena != null) valido = false

        _uiState.value = state.copy(
            errorUsuario = errorUsuario,
            errorContrasena = errorContrasena,
        )
        return valido
    }

    // ── Login ──────────────────────────────────────────────────────────────────

    fun login() {
        if (!validarFormulario()) return

        val state = _uiState.value
        viewModelScope.launch {
            sessionRepository.setAutenticando()

            try {
                val idInstalacion = installationIdStore.getOrCreateInstallationId()
                val response = publicAuthApi.login(
                    SolicitudLogin(
                        nombreUsuario = state.usuario.trim(),
                        contrasena = state.contrasena,
                        identificadorInstalacion = idInstalacion,
                    )
                )

                if (response.isSuccessful) {
                    val tokens = response.body()!!
                    sessionRepository.guardarSesion(tokens, state.usuario.trim())
                } else {
                    val tipo = mapHttpErrorToTipo(response.code(), response.errorBody()?.string())
                    sessionRepository.setError(tipo)
                }
            } catch (e: SocketTimeoutException) {
                sessionRepository.setError(ErrorTipo.TIMEOUT)
            } catch (e: ConnectException) {
                sessionRepository.setError(ErrorTipo.ERROR_SERVIDOR)
            } catch (e: IOException) {
                sessionRepository.setError(ErrorTipo.SIN_CONEXION)
            } catch (e: Exception) {
                sessionRepository.setError(ErrorTipo.ERROR_DESCONOCIDO, e.message ?: "")
            }
        }
    }

    // ── Logout ─────────────────────────────────────────────────────────────────

    /**
     * Logout en Fase 4A: siempre limpia la sesión local independientemente del resultado remoto.
     * Fase 4C extenderá esto para bloquear si hay gestiones pendientes (sin conexión).
     */
    fun logout() {
        viewModelScope.launch {
            sessionRepository.logout()
        }
    }

    // ── Error mapping ──────────────────────────────────────────────────────────

    private fun mapHttpErrorToTipo(httpCode: Int, errorBody: String?): ErrorTipo {
        if (errorBody != null) {
            try {
                val apiError = json.decodeFromString<ApiError>(errorBody)
                return when (apiError.code) {
                    "CONFLICTO_DISPOSITIVO" -> ErrorTipo.DISPOSITIVO_CONFLICTO
                    else -> when (httpCode) {
                        401 -> ErrorTipo.CREDENCIALES_INCORRECTAS
                        409 -> ErrorTipo.DISPOSITIVO_CONFLICTO
                        in 500..599 -> ErrorTipo.ERROR_SERVIDOR
                        else -> ErrorTipo.ERROR_DESCONOCIDO
                    }
                }
            } catch (_: Exception) { }
        }
        return when (httpCode) {
            401 -> ErrorTipo.CREDENCIALES_INCORRECTAS
            409 -> ErrorTipo.DISPOSITIVO_CONFLICTO
            in 500..599 -> ErrorTipo.ERROR_SERVIDOR
            else -> ErrorTipo.ERROR_DESCONOCIDO
        }
    }
}

data class LoginUiState(
    val usuario: String = "",
    val contrasena: String = "",
    val errorUsuario: String? = null,
    val errorContrasena: String? = null,
)
