package cl.zzenner.cobranza.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cl.zzenner.cobranza.core.database.entity.PersonaDirectaEntity

@Dao
interface PersonaDirectaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PersonaDirectaEntity)

    @Query("SELECT * FROM persona_directa WHERE id = :id")
    suspend fun findById(id: String): PersonaDirectaEntity?

    @Query("SELECT * FROM persona_directa WHERE rutNumero = :rutNumero AND rutDv = :rutDv LIMIT 1")
    suspend fun findByRut(rutNumero: String, rutDv: String): PersonaDirectaEntity?

    @Query("DELETE FROM persona_directa")
    suspend fun deleteAll()
}
