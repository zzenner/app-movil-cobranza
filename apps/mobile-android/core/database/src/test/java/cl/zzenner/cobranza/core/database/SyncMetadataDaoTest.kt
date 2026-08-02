package cl.zzenner.cobranza.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import cl.zzenner.cobranza.core.database.entity.SyncMetadataEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SyncMetadataDaoTest {

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
    fun `getMetadata retorna null inicialmente`() = runTest {
        assertNull(db.syncMetadataDao().getMetadata())
    }

    @Test
    fun `upsert inserta registro singleton`() = runTest {
        db.syncMetadataDao().upsert(SyncMetadataEntity(estado = "EXITOSA"))
        val result = db.syncMetadataDao().getMetadata()
        assertEquals("EXITOSA", result?.estado)
        assertEquals(1L, result?.id)
    }

    @Test
    fun `upsert reemplaza registro existente`() = runTest {
        db.syncMetadataDao().upsert(SyncMetadataEntity(estado = "NONE"))
        db.syncMetadataDao().upsert(SyncMetadataEntity(estado = "EXITOSA", ultimaDescargaExitosa = 12345L))
        val result = db.syncMetadataDao().getMetadata()
        assertEquals("EXITOSA", result?.estado)
        assertEquals(12345L, result?.ultimaDescargaExitosa)
    }

    @Test
    fun `deleteAll limpia el singleton`() = runTest {
        db.syncMetadataDao().upsert(SyncMetadataEntity(estado = "EXITOSA"))
        db.syncMetadataDao().deleteAll()
        assertNull(db.syncMetadataDao().getMetadata())
    }

    @Test
    fun `upsert SIN_ASIGNACION conserva campos de datos anteriores`() = runTest {
        db.syncMetadataDao().upsert(
            SyncMetadataEntity(
                estado = "EXITOSA",
                asignacionIdAlmacenada = "asig-1",
                fechaAsignacionAlmacenada = "2026-08-01",
                ultimaDescargaExitosa = 9999L,
            ),
        )
        db.syncMetadataDao().upsert(
            SyncMetadataEntity(
                estado = "SIN_ASIGNACION",
                datosMarcadosComoDesactualizados = true,
                datosAnterioresDisponibles = true,
                asignacionIdAlmacenada = "asig-1",
                fechaAsignacionAlmacenada = "2026-08-01",
                ultimaDescargaExitosa = 9999L,
            ),
        )
        val result = db.syncMetadataDao().getMetadata()
        assertEquals("SIN_ASIGNACION", result?.estado)
        assertEquals("asig-1", result?.asignacionIdAlmacenada)
        assertEquals(true, result?.datosAnterioresDisponibles)
        assertEquals(true, result?.datosMarcadosComoDesactualizados)
    }
}
