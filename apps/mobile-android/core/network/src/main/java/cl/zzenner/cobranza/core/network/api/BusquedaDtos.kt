package cl.zzenner.cobranza.core.network.api

import kotlinx.serialization.Serializable

@Serializable
data class SolicitudBusquedaDto(
    val rutNumero: String,
    val rutDv: String,
)

@Serializable
data class RespuestaBusquedaDto(
    val version: Int,
    val generadoEn: String,
    val persona: PersonaDescargaDto,
)
