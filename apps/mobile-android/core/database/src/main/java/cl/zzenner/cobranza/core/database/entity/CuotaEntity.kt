package cl.zzenner.cobranza.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "cuota",
    foreignKeys = [
        ForeignKey(
            entity = OperacionEntity::class,
            parentColumns = ["id"],
            childColumns = ["operacionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("operacionId")],
)
data class CuotaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val operacionId: String,
    val numeroCuota: Int,
    val estado: String,
    /** BigDecimal como String */
    val montoTotal: String,
    val saldo: String,
    /** LocalDate ISO YYYY-MM-DD */
    val fechaVencimiento: String,
)
