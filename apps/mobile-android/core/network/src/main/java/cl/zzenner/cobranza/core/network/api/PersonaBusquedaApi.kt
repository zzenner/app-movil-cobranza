package cl.zzenner.cobranza.core.network.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface PersonaBusquedaApi {

    @POST("api/v1/personas/busquedas")
    suspend fun buscarPersona(@Body solicitud: SolicitudBusquedaDto): Response<RespuestaBusquedaDto>
}
