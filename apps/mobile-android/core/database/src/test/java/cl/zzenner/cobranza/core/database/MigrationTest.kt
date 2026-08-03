package cl.zzenner.cobranza.core.database

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import cl.zzenner.cobranza.core.database.migration.MIGRATION_1_2
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation(),
        CobranzaDatabase::class.java,
    )

    @Test
    fun `migration 1 a 2 crea tabla gestion_local`() {
        val dbName = "test_migration"

        helper.createDatabase(dbName, 1).apply {
            execSQL(
                "INSERT INTO sync_metadata (id, estado, ultimoIntento, datosAnterioresDisponibles, datosMarcadosComoDesactualizados) " +
                    "VALUES (1, 'NONE', 0, 0, 0)",
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(dbName, 2, true, MIGRATION_1_2)

        val cursor = db.query("SELECT COUNT(*) FROM gestion_local")
        assertTrue(cursor.moveToFirst())
        assertEquals(0, cursor.getInt(0))
        cursor.close()

        db.close()
    }

    @Test
    fun `migration 1 a 2 preserva datos previos de sync_metadata`() {
        val dbName = "test_migration_preserva"

        helper.createDatabase(dbName, 1).apply {
            execSQL(
                "INSERT INTO sync_metadata (id, estado, ultimoIntento, datosAnterioresDisponibles, datosMarcadosComoDesactualizados) " +
                    "VALUES (1, 'EXITOSA', 1234567890, 0, 0)",
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(dbName, 2, true, MIGRATION_1_2)

        val cursor = db.query("SELECT estado FROM sync_metadata WHERE id = 1")
        assertTrue(cursor.moveToFirst())
        assertEquals("EXITOSA", cursor.getString(0))
        cursor.close()

        db.close()
    }

    @Test
    fun `migration 1 a 2 crea todos los indices de gestion_local`() {
        val dbName = "test_migration_indices"

        helper.createDatabase(dbName, 1).close()

        val db = helper.runMigrationsAndValidate(dbName, 2, true, MIGRATION_1_2)

        val cursor = db.query(
            "SELECT name FROM sqlite_master WHERE type='index' AND tbl_name='gestion_local'",
        )
        val indices = mutableSetOf<String>()
        cursor.use { while (it.moveToNext()) indices.add(it.getString(0)) }

        assertTrue(
            "Faltan índices en gestion_local: $indices",
            indices.containsAll(
                setOf(
                    "index_gestion_local_personaId",
                    "index_gestion_local_estadoSincronizacion",
                    "index_gestion_local_fechaCreacionLocalEpoch",
                    "index_gestion_local_estadoSincronizacion_fechaProximoIntentoEpoch",
                ),
            ),
        )

        db.close()
    }

    @Test
    fun `database v2 contiene tabla gestion_local`() = runTest {
        val db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            CobranzaDatabase::class.java,
        ).allowMainThreadQueries().build()

        val cursor = db.openHelper.readableDatabase.query(
            "SELECT name FROM sqlite_master WHERE type='table' AND name='gestion_local'",
            emptyArray(),
        )
        assertTrue("gestion_local debe existir en v2", cursor.moveToFirst())
        cursor.close()
        db.close()
    }
}
