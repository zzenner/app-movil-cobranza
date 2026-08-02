package cl.zzenner.cobranza.core.network.api

import kotlinx.serialization.Serializable
import java.math.BigDecimal

/**
 * DTOs de sincronización — mapean exactamente la respuesta JSON del endpoint
 * GET /api/v1/asignaciones/diaria/activa producida por Spring Boot / Jackson.
 *
 * Tipos JSON → Kotlin:
 * - UUID (Jackson) → String con guiones en minúsculas
 * - LocalDate (Jackson JavaTimeModule) → String "YYYY-MM-DD"
 * - Instant (Jackson JavaTimeModule) → String ISO-8601 "2026-08-02T10:30:00Z"
 * - BigDecimal (Jackson) → número JSON, deserializado por BigDecimalSerializer
 * - boolean / double / float → tipos primitivos nativos
 */

@Serializable
data class RespuestaAsignacionDiariaDto(
    val id: String,
    val ejecutivoId: String,
    /** LocalDate como "YYYY-MM-DD" */
    val fecha: String,
    val estado: String,
    val personas: List<PersonaDescargaDto>,
)

@Serializable
data class PersonaDescargaDto(
    val id: String,
    val rutNumero: String,
    val rutDv: String,
    val nombre: String,
    val direcciones: List<DireccionDescargaDto>,
    val avales: List<AvalDescargaDto>,
    val operaciones: List<OperacionDescargaDto>,
    val ultimasGestiones: List<GestionDescargaDto>,
)

@Serializable
data class DireccionDescargaDto(
    val personaId: String,
    val tipo: String,
    val texto: String,
    val comuna: String?,
    val ciudad: String?,
    val vigente: Boolean,
)

@Serializable
data class AvalDescargaDto(
    val personaId: String,
    val rutNumero: String,
    val rutDv: String,
    val nombre: String,
)

@Serializable
data class OperacionDescargaDto(
    val id: String,
    val personaId: String,
    val numeroOperacion: String,
    val estado: String,
    @Serializable(with = BigDecimalSerializer::class)
    val capital: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val interesPenal: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val gastosCobranza: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val totalVigente: BigDecimal,
    val cuotas: List<CuotaDescargaDto>,
)

@Serializable
data class CuotaDescargaDto(
    val numeroCuota: Int,
    val estado: String,
    @Serializable(with = BigDecimalSerializer::class)
    val montoTotal: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val saldo: BigDecimal,
    /** LocalDate como "YYYY-MM-DD" */
    val fechaVencimiento: String,
)

@Serializable
data class GestionDescargaDto(
    val id: String,
    val origenGestion: String,
    /** UUID nullable — null cuando origenGestion != ASIGNACION_DIARIA */
    val asignacionDiariaId: String?,
    val personaId: String,
    val ejecutivoId: String,
    val tipoGestion: String,
    /** Instant ISO-8601 */
    val fechaGestion: String,
    val observacion: String?,
    val observacionDireccion: String?,
    val latitud: Double,
    val longitud: Double,
    val precisionMetros: Float,
    val proveedorGps: String?,
    val ubicacionSimulada: Boolean,
    /** Instant ISO-8601 */
    val fechaCapturaGps: String,
    /** LocalDate "YYYY-MM-DD" — nullable */
    val fechaCompromiso: String?,
    /** Instant ISO-8601 */
    val fechaCreacionServidor: String,
)
