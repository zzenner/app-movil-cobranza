package cl.zzenner.cobranza.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import cl.zzenner.cobranza.core.database.dao.AsignacionDiariaDao
import cl.zzenner.cobranza.core.database.dao.GestionHistoricaDao
import cl.zzenner.cobranza.core.database.dao.GestionLocalDao
import cl.zzenner.cobranza.core.database.dao.OperacionDao
import cl.zzenner.cobranza.core.database.dao.PersonaDao
import cl.zzenner.cobranza.core.database.dao.PersonaDirectaDao
import cl.zzenner.cobranza.core.database.dao.SyncMetadataDao
import cl.zzenner.cobranza.core.database.entity.AsignacionDiariaEntity
import cl.zzenner.cobranza.core.database.entity.AsignacionPersonaCrossRef
import cl.zzenner.cobranza.core.database.entity.AvalEntity
import cl.zzenner.cobranza.core.database.entity.CuotaEntity
import cl.zzenner.cobranza.core.database.entity.DireccionEntity
import cl.zzenner.cobranza.core.database.entity.GestionHistoricaEntity
import cl.zzenner.cobranza.core.database.entity.GestionLocalEntity
import cl.zzenner.cobranza.core.database.entity.OperacionEntity
import cl.zzenner.cobranza.core.database.entity.PersonaDirectaEntity
import cl.zzenner.cobranza.core.database.entity.PersonaEntity
import cl.zzenner.cobranza.core.database.entity.SyncMetadataEntity

@Database(
    entities = [
        AsignacionDiariaEntity::class,
        PersonaEntity::class,
        AsignacionPersonaCrossRef::class,
        DireccionEntity::class,
        AvalEntity::class,
        OperacionEntity::class,
        CuotaEntity::class,
        GestionHistoricaEntity::class,
        GestionLocalEntity::class,
        SyncMetadataEntity::class,
        PersonaDirectaEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
abstract class CobranzaDatabase : RoomDatabase() {
    abstract fun asignacionDiariaDao(): AsignacionDiariaDao
    abstract fun personaDao(): PersonaDao
    abstract fun operacionDao(): OperacionDao
    abstract fun gestionHistoricaDao(): GestionHistoricaDao
    abstract fun gestionLocalDao(): GestionLocalDao
    abstract fun syncMetadataDao(): SyncMetadataDao
    abstract fun personaDirectaDao(): PersonaDirectaDao
}
