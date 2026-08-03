package cl.zzenner.cobranza.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "persona_directa",
    indices = [
        Index(value = ["rutNumero", "rutDv"]),
    ],
)
data class PersonaDirectaEntity(
    @PrimaryKey val id: String,
    val rutNumero: String,
    val rutDv: String,
    val nombre: String,
    val versionContrato: Int,
    val generadoEnEpoch: Long,
    val fechaConsultaEpoch: Long,
    val detalleJson: String,
)
