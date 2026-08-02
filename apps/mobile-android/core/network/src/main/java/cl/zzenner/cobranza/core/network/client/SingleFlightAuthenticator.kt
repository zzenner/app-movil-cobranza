package cl.zzenner.cobranza.core.network.client

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

/**
 * OkHttp Authenticator que implementa renovación de token con garantía de single-flight:
 * - Solo una renovación concurrente en progreso a la vez (Mutex).
 * - Si otro hilo ya renovó, reutiliza el nuevo token sin llamar al servidor.
 * - No reintenta si la solicitud que falló ES el endpoint de refresh (evita ciclos).
 * - Error 401 en refresh limpia la sesión.
 * - Error de red NO limpia la sesión (puede ser temporal).
 */
internal class SingleFlightAuthenticator(
    private val tokenProvider: TokenProvider,
) : Authenticator {

    private val mutex = Mutex()

    override fun authenticate(route: Route?, response: Response): Request? {
        // No reintentar si este request YA es el endpoint de refresh
        if (response.request.url.encodedPath.endsWith("/auth/refresh")) return null

        // Capturar el token que usó la solicitud original (puede estar obsoleto)
        val tokenUsado = response.request.header("Authorization")
            ?.removePrefix("Bearer ")?.trim()

        return runBlocking {
            mutex.withLock {
                val tokenActual = tokenProvider.getAccessToken()

                if (tokenActual != null && tokenActual != tokenUsado) {
                    // Otro hilo ya renovó — reutilizar el token nuevo sin llamar al servidor
                    return@withLock response.request.newBuilder()
                        .header("Authorization", "Bearer $tokenActual")
                        .build()
                }

                // Necesitamos renovar
                val renovado = tokenProvider.refreshTokens()
                if (renovado) {
                    val nuevoToken = tokenProvider.getAccessToken()
                    response.request.newBuilder()
                        .header("Authorization", "Bearer $nuevoToken")
                        .build()
                } else {
                    null // refresh inválido: la sesión ya fue limpiada dentro de refreshTokens()
                }
            }
        }
    }
}
