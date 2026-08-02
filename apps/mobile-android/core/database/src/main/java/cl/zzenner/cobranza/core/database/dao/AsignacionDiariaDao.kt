package cl.zzenner.cobranza.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cl.zzenner.cobranza.core.database.entity.AsignacionDiariaEntity
import cl.zzenner.cobranza.core.database.entity.AsignacionPersonaCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface AsignacionDiariaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: AsignacionDiariaEntity)

    @Query("DELETE FROM asignacion_diaria")
    suspend fun deleteAll()

    @Query("SELECT * FROM asignacion_diaria LIMIT 1")
    suspend fun getActiva(): AsignacionDiariaEntity?

    @Query("SELECT * FROM asignacion_diaria LIMIT 1")
    fun observeActiva(): Flow<AsignacionDiariaEntity?>

    // ── Cross-reference (asignacion_persona) ─────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllCrossRefs(refs: List<AsignacionPersonaCrossRef>)

    @Query("DELETE FROM asignacion_persona")
    suspend fun deleteAllCrossRefs()
}
