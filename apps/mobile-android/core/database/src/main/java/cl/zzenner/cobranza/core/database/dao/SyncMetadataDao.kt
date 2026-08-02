package cl.zzenner.cobranza.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cl.zzenner.cobranza.core.database.entity.SyncMetadataEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncMetadataDao {

    /** INSERT OR REPLACE — mantiene el singleton con id=1 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SyncMetadataEntity)

    @Query("SELECT * FROM sync_metadata WHERE id = 1")
    suspend fun getMetadata(): SyncMetadataEntity?

    @Query("SELECT * FROM sync_metadata WHERE id = 1")
    fun observeMetadata(): Flow<SyncMetadataEntity?>

    @Query("DELETE FROM sync_metadata")
    suspend fun deleteAll()
}
