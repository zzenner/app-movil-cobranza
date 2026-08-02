package cl.zzenner.cobranza.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Operación de crédito de una persona.
 * Los importes se almacenan como String para preservar la precisión exacta de BigDecimal.
 */
@Entity(
    tableName = "operacion",
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
data class OperacionEntity(
    @PrimaryKey val id: String,
    val personaId: String,
    val numeroOperacion: String,
    val estado: String,
    /** BigDecimal almacenado como String — usar BigDecimal(capital) para aritmética */
    val capital: String,
    val interesPenal: String,
    val gastosCobranza: String,
    val totalVigente: String,
)
