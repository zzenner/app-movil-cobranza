package cl.zzenner.cobranza.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cl.zzenner.cobranza.core.database.entity.GestionHistoricaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GestionHistoricaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(gestiones: List<GestionHistoricaEntity>)

    @Query("DELETE FROM gestion_historica")
    suspend fun deleteAll()

    @Query("SELECT * FROM gestion_historica WHERE personaId = :personaId ORDER BY fechaGestion DESC")
    fun getGestionesDePersona(personaId: String): Flow<List<GestionHistoricaEntity>>
}
