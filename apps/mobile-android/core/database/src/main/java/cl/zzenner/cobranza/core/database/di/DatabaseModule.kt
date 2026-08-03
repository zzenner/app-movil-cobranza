package cl.zzenner.cobranza.core.database.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import cl.zzenner.cobranza.core.database.CobranzaDatabase
import cl.zzenner.cobranza.core.database.dao.AsignacionDiariaDao
import cl.zzenner.cobranza.core.database.dao.GestionHistoricaDao
import cl.zzenner.cobranza.core.database.dao.GestionLocalDao
import cl.zzenner.cobranza.core.database.dao.OperacionDao
import cl.zzenner.cobranza.core.database.dao.PersonaDao
import cl.zzenner.cobranza.core.database.dao.SyncMetadataDao
import cl.zzenner.cobranza.core.database.migration.MIGRATION_1_2
import cl.zzenner.cobranza.core.database.transaction.BundleReplacementTransaction
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): CobranzaDatabase =
        Room.databaseBuilder(ctx, CobranzaDatabase::class.java, "cobranza.db")
            .addMigrations(MIGRATION_1_2)
            .addCallback(object : RoomDatabase.Callback() {
                override fun onOpen(db: SupportSQLiteDatabase) {
                    super.onOpen(db)
                    db.execSQL("PRAGMA foreign_keys = ON")
                }
            })
            .build()

    @Provides
    fun provideAsignacionDiariaDao(db: CobranzaDatabase): AsignacionDiariaDao =
        db.asignacionDiariaDao()

    @Provides
    fun providePersonaDao(db: CobranzaDatabase): PersonaDao =
        db.personaDao()

    @Provides
    fun provideOperacionDao(db: CobranzaDatabase): OperacionDao =
        db.operacionDao()

    @Provides
    fun provideGestionHistoricaDao(db: CobranzaDatabase): GestionHistoricaDao =
        db.gestionHistoricaDao()

    @Provides
    fun provideGestionLocalDao(db: CobranzaDatabase): GestionLocalDao =
        db.gestionLocalDao()

    @Provides
    fun provideSyncMetadataDao(db: CobranzaDatabase): SyncMetadataDao =
        db.syncMetadataDao()

    @Provides
    @Singleton
    fun provideBundleReplacementTransaction(db: CobranzaDatabase): BundleReplacementTransaction =
        BundleReplacementTransaction(db)
}
