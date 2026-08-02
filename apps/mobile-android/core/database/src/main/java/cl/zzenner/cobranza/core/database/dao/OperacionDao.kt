package cl.zzenner.cobranza.core.database.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import cl.zzenner.cobranza.core.database.entity.CuotaEntity
import cl.zzenner.cobranza.core.database.entity.OperacionEntity
import kotlinx.coroutines.flow.Flow

data class OperacionConCuotas(
    @Embedded val operacion: OperacionEntity,
    @Relation(parentColumn = "id", entityColumn = "operacionId")
    val cuotas: List<CuotaEntity>,
)

@Dao
interface OperacionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(operaciones: List<OperacionEntity>)

    @Query("DELETE FROM operacion")
    suspend fun deleteAll()

    @Transaction
    @Query("SELECT * FROM operacion WHERE personaId = :personaId")
    fun getOperacionesConCuotas(personaId: String): Flow<List<OperacionConCuotas>>

    @Transaction
    @Query("SELECT * FROM operacion WHERE personaId = :personaId")
    suspend fun getOperacionesDePersona(personaId: String): List<OperacionEntity>

    // ── Cuotas ────────────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllCuotas(cuotas: List<CuotaEntity>)

    @Query("DELETE FROM cuota")
    suspend fun deleteAllCuotas()
}
