package cl.zzenner.cobranza.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `gestion_local` (
                `id` TEXT NOT NULL,
                `personaId` TEXT NOT NULL,
                `personaRutNumero` TEXT NOT NULL,
                `personaRutDv` TEXT NOT NULL,
                `personaNombre` TEXT NOT NULL,
                `asignacionDiariaId` TEXT NOT NULL,
                `origenGestion` TEXT NOT NULL,
                `tipoGestion` TEXT NOT NULL,
                `fechaGestionEpoch` INTEGER NOT NULL,
                `fechaCapturaGpsEpoch` INTEGER NOT NULL,
                `observacion` TEXT,
                `observacionDireccion` TEXT,
                `latitud` REAL NOT NULL,
                `longitud` REAL NOT NULL,
                `precisionMetros` REAL NOT NULL,
                `ubicacionSimulada` INTEGER NOT NULL,
                `proveedorGps` TEXT,
                `fechaCompromiso` TEXT,
                `estadoSincronizacion` TEXT NOT NULL,
                `fechaCreacionLocalEpoch` INTEGER NOT NULL,
                `cantidadIntentos` INTEGER NOT NULL,
                `leaseHastaEpoch` INTEGER,
                `fechaProximoIntentoEpoch` INTEGER,
                `codigoErrorServidor` TEXT,
                `mensajeError` TEXT,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_gestion_local_personaId` ON `gestion_local` (`personaId`)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_gestion_local_estadoSincronizacion` ON `gestion_local` (`estadoSincronizacion`)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_gestion_local_fechaCreacionLocalEpoch` ON `gestion_local` (`fechaCreacionLocalEpoch`)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_gestion_local_estadoSincronizacion_fechaProximoIntentoEpoch` ON `gestion_local` (`estadoSincronizacion`, `fechaProximoIntentoEpoch`)",
        )
    }
}
