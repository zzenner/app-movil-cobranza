package cl.zzenner.cobranza.core.network.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface GestionApi {

    @POST("gestiones")
    suspend fun crearGestion(@Body dto: SolicitudGestionDto): Response<RespuestaGestionDto>
}
