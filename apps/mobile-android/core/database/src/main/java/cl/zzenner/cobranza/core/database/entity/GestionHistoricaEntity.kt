package cl.zzenner.cobranza.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Gestión histórica descargada desde el servidor.
 * Solo lectura — no se modifican localmente.
 * Las gestiones creadas en el dispositivo (Fase 4C) usarán una entidad diferente.
 */
@Entity(
    tableName = "gestion_historica",
    foreignKeys = [
        ForeignKey(
            entity = PersonaEntity::class,
            parentColumns = ["id"],
            childColumns = ["personaId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("personaId")],
)
data class GestionHistoricaEntity(
    @PrimaryKey val id: String,
    val personaId: String,
    val asignacionDiariaId: String?,
    val ejecutivoId: String,
    val origenGestion: String,
    val tipoGestion: String,
    /** Epoch millis */
    val fechaGestion: Long,
    val observacion: String?,
    val observacionDireccion: String?,
    val latitud: Double,
    val longitud: Double,
    val precisionMetros: Float,
    val proveedorGps: String?,
    val ubicacionSimulada: Boolean,
    /** Epoch millis */
    val fechaCapturaGps: Long,
    /** LocalDate ISO YYYY-MM-DD — nullable */
    val fechaCompromiso: String?,
    /** Epoch millis */
    val fechaCreacionServidor: Long,
)
