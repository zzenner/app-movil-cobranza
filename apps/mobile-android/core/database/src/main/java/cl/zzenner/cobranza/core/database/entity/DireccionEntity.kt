package cl.zzenner.cobranza.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "direccion",
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
data class DireccionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val personaId: String,
    val tipo: String,
    val texto: String,
    val comuna: String?,
    val ciudad: String?,
    val vigente: Boolean,
)
