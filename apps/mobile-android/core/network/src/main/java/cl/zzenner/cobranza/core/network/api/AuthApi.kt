package cl.zzenner.cobranza.core.network.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface AuthApi {

    @POST("api/v1/auth/login")
    suspend fun login(@Body solicitud: SolicitudLogin): Response<RespuestaToken>

    @POST("api/v1/auth/refresh")
    suspend fun renovar(@Body solicitud: SolicitudRenovacion): Response<RespuestaToken>

    @POST("api/v1/auth/logout")
    suspend fun logout(@Header("Authorization") authorization: String): Response<Unit>
}
