package cl.zzenner.cobranza.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "persona",
    indices = [Index(value = ["rutNumero", "rutDv"])],
)
data class PersonaEntity(
    @PrimaryKey val id: String,
    val rutNumero: String,
    val rutDv: String,
    val nombre: String,
)
