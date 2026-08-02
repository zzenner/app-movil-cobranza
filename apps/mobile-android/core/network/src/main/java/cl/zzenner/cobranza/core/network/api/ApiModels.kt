package cl.zzenner.cobranza.core.network.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SolicitudLogin(
    val nombreUsuario: String,
    val contrasena: String,
    val identificadorInstalacion: String,
)

@Serializable
data class SolicitudRenovacion(
    val refreshToken: String,
)

@Serializable
data class RespuestaToken(
    val accessToken: String,
    val refreshToken: String,
    val expiresInSeconds: Long,
    @SerialName("sessionExpiresAt")
    val sessionExpiresAt: String,
)

/**
 * Mapeado desde ProblemDetail (RFC 7807) que devuelve la API.
 * El campo [code] es estable entre versiones; no depender del mensaje de texto.
 */
@Serializable
data class ApiError(
    val status: Int = 0,
    val title: String = "",
    val detail: String = "",
    val code: String = "",
    val timestamp: String = "",
    val path: String = "",
)
