package cl.zzenner.cobranza.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "aval",
    foreignKeys = [
        ForeignKey(
            entity = PersonaEntity::class,
            parentColumns = ["id"],
            childColumns = ["personaIdTitular"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("personaIdTitular")],
)
data class AvalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val personaIdTitular: String,
    val rutNumero: String,
    val rutDv: String,
    val nombre: String,
)
