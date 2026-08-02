package cl.zzenner.cobranza.core.network.client

/**
 * Abstracción para que el cliente autenticado pueda obtener y renovar tokens
 * sin depender de los módulos de feature o security directamente.
 *
 * La implementación concreta vive en :feature:auth y se inyecta vía Hilt.
 */
interface TokenProvider {
    suspend fun getAccessToken(): String?
    suspend fun refreshTokens(): Boolean
    suspend fun clearSession()
}
