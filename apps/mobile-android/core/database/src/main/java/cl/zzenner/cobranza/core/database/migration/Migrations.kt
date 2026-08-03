package cl.zzenner.cobranza.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // SQLite no permite modificar nullability directamente.
        // Recreamos gestion_local con asignacionDiariaId nullable.
        db.execSQL(
            """
            CREATE TABLE `gestion_local_new` (
                `id` TEXT NOT NULL,
                `personaId` TEXT NOT NULL,
                `personaRutNumero` TEXT NOT NULL,
                `personaRutDv` TEXT NOT NULL,
                `personaNombre` TEXT NOT NULL,
                `asignacionDiariaId` TEXT,
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
            """
            INSERT INTO `gestion_local_new` (
                `id`, `personaId`, `personaRutNumero`, `personaRutDv`, `personaNombre`,
                `asignacionDiariaId`, `origenGestion`, `tipoGestion`,
                `fechaGestionEpoch`, `fechaCapturaGpsEpoch`,
                `observacion`, `observacionDireccion`,
                `latitud`, `longitud`, `precisionMetros`, `ubicacionSimulada`,
                `proveedorGps`, `fechaCompromiso`,
                `estadoSincronizacion`, `fechaCreacionLocalEpoch`, `cantidadIntentos`,
                `leaseHastaEpoch`, `fechaProximoIntentoEpoch`,
                `codigoErrorServidor`, `mensajeError`
            )
            SELECT
                `id`, `personaId`, `personaRutNumero`, `personaRutDv`, `personaNombre`,
                `asignacionDiariaId`, `origenGestion`, `tipoGestion`,
                `fechaGestionEpoch`, `fechaCapturaGpsEpoch`,
                `observacion`, `observacionDireccion`,
                `latitud`, `longitud`, `precisionMetros`, `ubicacionSimulada`,
                `proveedorGps`, `fechaCompromiso`,
                `estadoSincronizacion`, `fechaCreacionLocalEpoch`, `cantidadIntentos`,
                `leaseHastaEpoch`, `fechaProximoIntentoEpoch`,
                `codigoErrorServidor`, `mensajeError`
            FROM `gestion_local`
            """.trimIndent(),
        )
        db.execSQL("DROP TABLE `gestion_local`")
        db.execSQL("ALTER TABLE `gestion_local_new` RENAME TO `gestion_local`")
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
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `persona_directa` (
                `id` TEXT NOT NULL,
                `rutNumero` TEXT NOT NULL,
                `rutDv` TEXT NOT NULL,
                `nombre` TEXT NOT NULL,
                `versionContrato` INTEGER NOT NULL,
                `generadoEnEpoch` INTEGER NOT NULL,
                `fechaConsultaEpoch` INTEGER NOT NULL,
                `detalleJson` TEXT NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_persona_directa_rutNumero_rutDv` ON `persona_directa` (`rutNumero`, `rutDv`)",
        )
    }
}

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
