package cl.zzenner.cobranza.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "asignacion_persona",
    primaryKeys = ["asignacionId", "personaId"],
    foreignKeys = [
        ForeignKey(
            entity = AsignacionDiariaEntity::class,
            parentColumns = ["id"],
            childColumns = ["asignacionId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = PersonaEntity::class,
            parentColumns = ["id"],
            childColumns = ["personaId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("asignacionId"),
        Index("personaId"),
    ],
)
data class AsignacionPersonaCrossRef(
    val asignacionId: String,
    val personaId: String,
)
