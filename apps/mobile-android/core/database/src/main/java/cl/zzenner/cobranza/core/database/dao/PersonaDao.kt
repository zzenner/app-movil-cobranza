package cl.zzenner.cobranza.core.database.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import cl.zzenner.cobranza.core.database.entity.AvalEntity
import cl.zzenner.cobranza.core.database.entity.DireccionEntity
import cl.zzenner.cobranza.core.database.entity.OperacionEntity
import cl.zzenner.cobranza.core.database.entity.PersonaEntity
import kotlinx.coroutines.flow.Flow

data class PersonaConDetalle(
    @Embedded val persona: PersonaEntity,
    @Relation(parentColumn = "id", entityColumn = "personaId")
    val direcciones: List<DireccionEntity>,
    @Relation(parentColumn = "id", entityColumn = "personaIdTitular")
    val avales: List<AvalEntity>,
    @Relation(parentColumn = "id", entityColumn = "personaId")
    val operaciones: List<OperacionEntity>,
)

@Dao
interface PersonaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(personas: List<PersonaEntity>)

    @Query("DELETE FROM persona")
    suspend fun deleteAll()

    // ── Queries con relaciones ─────────────────────────────────────────────────

    @Transaction
    @Query(
        "SELECT p.* FROM persona p " +
        "INNER JOIN asignacion_persona ap ON p.id = ap.personaId " +
        "WHERE ap.asignacionId = :asignacionId",
    )
    fun getPersonasDeAsignacion(asignacionId: String): Flow<List<PersonaConDetalle>>

    /**
     * Búsqueda por RUT normalizado (sin puntos, sin guion, K→k).
     * El parámetro [rutNormalizado] debe estar ya normalizado por el llamador.
     */
    @Query(
        "SELECT * FROM persona WHERE LOWER(rutNumero || rutDv) LIKE '%' || LOWER(:rutNormalizado) || '%'",
    )
    suspend fun buscarPorRut(rutNormalizado: String): List<PersonaEntity>

    @Transaction
    @Query("SELECT * FROM persona WHERE id = :personaId")
    fun getPersonaConDetalle(personaId: String): Flow<PersonaConDetalle?>

    // ── Direcciones ───────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllDirecciones(direcciones: List<DireccionEntity>)

    @Query("DELETE FROM direccion")
    suspend fun deleteAllDirecciones()

    // ── Avales ────────────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllAvales(avales: List<AvalEntity>)

    @Query("DELETE FROM aval")
    suspend fun deleteAllAvales()
}
