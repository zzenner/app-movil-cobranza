package cl.zzenner.cobranza.core.network.api

import kotlinx.serialization.Serializable

/**
 * DTOs del endpoint POST /api/v1/gestiones.
 *
 * SolicitudGestionDto mapea exactamente SolicitudCrearGestion.java del servidor.
 * Campos excluidos intencionalmente:
 * - ejecutivoId: el servidor lo extrae del JWT
 * - fechaCreacionServidor: lo asigna el servidor
 *
 * precisionMetros se envía como Double (aunque se almacena como Float en Room).
 * fechaGestion / fechaCapturaGps: Instant serializado como String ISO-8601.
 * fechaCompromiso: LocalDate serializado como String "YYYY-MM-DD", nullable.
 */
@Serializable
data class SolicitudGestionDto(
    val id: String,
    val personaId: String,
    val origenGestion: String,
    val asignacionDiariaId: String?,
    val tipoGestion: String,
    val fechaGestion: String,
    val fechaCapturaGps: String,
    val observacion: String?,
    val observacionDireccion: String?,
    val latitud: Double,
    val longitud: Double,
    val precisionMetros: Double,
    val ubicacionSimulada: Boolean,
    val proveedorGps: String?,
    val fechaCompromiso: String?,
)

@Serializable
data class RespuestaGestionDto(
    val id: String,
    val resultado: String,
)
