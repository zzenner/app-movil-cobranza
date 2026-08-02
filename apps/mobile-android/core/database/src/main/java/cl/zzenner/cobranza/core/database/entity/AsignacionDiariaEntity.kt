package cl.zzenner.cobranza.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Asignación diaria descargada del servidor.
 * Solo se almacena una asignación a la vez; BundleReplacementTransaction
 * la reemplaza atómicamente en cada descarga exitosa.
 */
@Entity(tableName = "asignacion_diaria")
data class AsignacionDiariaEntity(
    @PrimaryKey val id: String,
    val ejecutivoId: String,
    /** ISO YYYY-MM-DD */
    val fecha: String,
    val estado: String,
    /** Instant ISO — null si la asignación no tiene fecha de publicación en el bundle */
    val fechaPublicacion: String? = null,
    /** Epoch millis del momento en que se descargó localmente */
    val fechaDescargaEpoch: Long,
)
