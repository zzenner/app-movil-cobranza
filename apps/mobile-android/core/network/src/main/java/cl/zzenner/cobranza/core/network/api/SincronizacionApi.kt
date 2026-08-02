package cl.zzenner.cobranza.core.network.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface SincronizacionApi {

    /**
     * Descarga la asignación diaria activa del ejecutivo autenticado.
     *
     * @param fecha fecha ISO "YYYY-MM-DD"; si es null el servidor usa la fecha actual en Santiago
     * @return 200 + bundle de asignación, o 204 si no hay asignación activa para esa fecha
     */
    @GET("api/v1/asignaciones/diaria/activa")
    suspend fun obtenerAsignacionDiariaActiva(
        @Query("fecha") fecha: String? = null,
    ): Response<RespuestaAsignacionDiariaDto>
}
