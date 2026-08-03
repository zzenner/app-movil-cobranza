package cl.zzenner.cobranza.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import cl.zzenner.cobranza.core.database.entity.GestionLocalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GestionLocalDao {

    @Insert
    suspend fun insert(entity: GestionLocalEntity)

    /**
     * Adquisición atómica CAS. Devuelve 1 si se adquirió el lease, 0 si otro worker se adelantó.
     * Solo adquiere si el estado es elegible y el tiempo de espera ha expirado.
     */
    @Query("""
        UPDATE gestion_local
        SET estadoSincronizacion = 'ENVIANDO', leaseHastaEpoch = :leaseHasta
        WHERE id = :id
        AND estadoSincronizacion IN ('PENDIENTE_ENVIO', 'ERROR_REINTENTABLE')
        AND (fechaProximoIntentoEpoch IS NULL OR fechaProximoIntentoEpoch <= :ahora)
    """)
    suspend fun adquirirLease(id: String, leaseHasta: Long, ahora: Long): Int

    /**
     * Recupera registros ENVIANDO cuyo lease expiró (app crasheó o fue terminada).
     * Solo recupera leases expirados — no hace bulk reset de todos los ENVIANDO.
     */
    @Query("""
        UPDATE gestion_local
        SET estadoSincronizacion = 'PENDIENTE_ENVIO', leaseHastaEpoch = NULL
        WHERE estadoSincronizacion = 'ENVIANDO'
        AND leaseHastaEpoch IS NOT NULL
        AND leaseHastaEpoch < :ahora
    """)
    suspend fun recuperarLeasesExpirados(ahora: Long): Int

    /**
     * Devuelve candidatos elegibles en orden FIFO de creación.
     * Los registros con fechaProximoIntentoEpoch en el futuro no se incluyen (backoff activo).
     */
    @Query("""
        SELECT * FROM gestion_local
        WHERE estadoSincronizacion IN ('PENDIENTE_ENVIO', 'ERROR_REINTENTABLE')
        AND (fechaProximoIntentoEpoch IS NULL OR fechaProximoIntentoEpoch <= :ahora)
        ORDER BY fechaCreacionLocalEpoch ASC
        LIMIT :limite
    """)
    suspend fun getElegibles(ahora: Long, limite: Int): List<GestionLocalEntity>

    @Query("""
        UPDATE gestion_local
        SET estadoSincronizacion = :estado,
            leaseHastaEpoch = NULL,
            cantidadIntentos = :intentos,
            fechaProximoIntentoEpoch = :proximoIntento,
            codigoErrorServidor = :codigoError,
            mensajeError = :mensajeError
        WHERE id = :id
    """)
    suspend fun actualizarEstado(
        id: String,
        estado: String,
        intentos: Int,
        proximoIntento: Long?,
        codigoError: String?,
        mensajeError: String?,
    )

    @Query("SELECT * FROM gestion_local WHERE personaId = :personaId ORDER BY fechaCreacionLocalEpoch DESC")
    fun getGestionesPorPersona(personaId: String): Flow<List<GestionLocalEntity>>

    @Query("SELECT COUNT(*) FROM gestion_local WHERE estadoSincronizacion != 'SINCRONIZADA'")
    suspend fun contarNoResueltas(): Int

    @Query("DELETE FROM gestion_local")
    suspend fun deleteAll()
}
