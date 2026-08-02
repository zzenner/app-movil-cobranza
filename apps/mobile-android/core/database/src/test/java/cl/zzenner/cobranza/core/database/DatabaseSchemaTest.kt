package cl.zzenner.cobranza.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import cl.zzenner.cobranza.core.database.entity.AsignacionDiariaEntity
import cl.zzenner.cobranza.core.database.entity.PersonaEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DatabaseSchemaTest {

    private lateinit var db: CobranzaDatabase

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            CobranzaDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `todas las tablas existen`() {
        val cursor = db.openHelper.readableDatabase.query(
            "SELECT name FROM sqlite_master WHERE type='table' " +
                "AND name NOT LIKE 'sqlite_%' AND name NOT LIKE 'room_%' AND name NOT LIKE 'android_%'",
            emptyArray(),
        )
        val tables = mutableSetOf<String>()
        cursor.use {
            while (it.moveToNext()) {
                tables.add(it.getString(0))
            }
        }

        val esperadas = setOf(
            "asignacion_diaria",
            "persona",
            "asignacion_persona",
            "direccion",
            "aval",
            "operacion",
            "cuota",
            "gestion_historica",
            "sync_metadata",
        )
        assertTrue(
            "Faltan tablas: ${esperadas - tables}. Encontradas: $tables",
            tables.containsAll(esperadas),
        )
    }

    @Test
    fun `insertar asignacion y leer`() = runTest {
        val dao = db.asignacionDiariaDao()
        val entity = AsignacionDiariaEntity(
            id = "asig-1",
            ejecutivoId = "eje-1",
            fecha = "2026-08-02",
            estado = "PUBLICADA",
            fechaDescargaEpoch = System.currentTimeMillis(),
        )
        dao.insert(entity)
        val resultado = dao.getActiva()
        assertTrue(resultado != null)
        assertTrue(resultado?.id == "asig-1")
    }

    @Test
    fun `insertar persona y buscar por rut`() = runTest {
        val dao = db.personaDao()
        val entity = PersonaEntity(
            id = "p-1",
            rutNumero = "27000001",
            rutDv = "0",
            nombre = "Test Persona",
        )
        dao.insertAll(listOf(entity))
        val resultado = dao.buscarPorRut("270000010")
        assertTrue(resultado.isNotEmpty())
    }

    @Test
    fun `sync_metadata tabla existe y admite upsert`() = runTest {
        val dao = db.syncMetadataDao()
        val meta = cl.zzenner.cobranza.core.database.entity.SyncMetadataEntity(estado = "NONE")
        dao.upsert(meta)
        val resultado = dao.getMetadata()
        assertTrue(resultado != null)
        assertTrue(resultado?.estado == "NONE")
    }
}
