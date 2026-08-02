package cl.zzenner.cobranza.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import cl.zzenner.cobranza.core.database.entity.AsignacionDiariaEntity
import cl.zzenner.cobranza.core.database.entity.AsignacionPersonaCrossRef
import cl.zzenner.cobranza.core.database.entity.AvalEntity
import cl.zzenner.cobranza.core.database.entity.CuotaEntity
import cl.zzenner.cobranza.core.database.entity.DireccionEntity
import cl.zzenner.cobranza.core.database.entity.GestionHistoricaEntity
import cl.zzenner.cobranza.core.database.entity.OperacionEntity
import cl.zzenner.cobranza.core.database.entity.PersonaEntity
import cl.zzenner.cobranza.core.database.transaction.BundleDescargado
import cl.zzenner.cobranza.core.database.transaction.BundleReplacementTransaction
import cl.zzenner.cobranza.core.database.transaction.ResultadoPersistencia
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BundleReplacementTransactionTest {

    private lateinit var db: CobranzaDatabase
    private lateinit var tx: BundleReplacementTransaction

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            CobranzaDatabase::class.java,
        ).allowMainThreadQueries().build()
        tx = BundleReplacementTransaction(db)
    }

    @After
    fun teardown() {
        db.close()
    }

    private fun bundle(
        asigId: String = "asig-1",
        fecha: String = "2026-08-02",
        personaId: String = "p-1",
    ) = BundleDescargado(
        asignacion = AsignacionDiariaEntity(
            id = asigId,
            ejecutivoId = "eje-1",
            fecha = fecha,
            estado = "PUBLICADA",
            fechaDescargaEpoch = 1000L,
        ),
        personas = listOf(
            PersonaEntity(id = personaId, rutNumero = "27000001", rutDv = "0", nombre = "Persona Test"),
        ),
        crossRefs = listOf(AsignacionPersonaCrossRef(asignacionId = asigId, personaId = personaId)),
        direcciones = listOf(
            DireccionEntity(personaId = personaId, tipo = "DOMICILIO", texto = "Calle 1", comuna = null, ciudad = null, vigente = true),
        ),
        avales = emptyList(),
        operaciones = listOf(
            OperacionEntity(
                id = "op-1", personaId = personaId, numeroOperacion = "OP001",
                estado = "VIGENTE", capital = "100000", interesPenal = "0",
                gastosCobranza = "0", totalVigente = "100000",
            ),
        ),
        cuotas = listOf(
            CuotaEntity(
                operacionId = "op-1", numeroCuota = 1, estado = "VIGENTE",
                montoTotal = "10000", saldo = "10000", fechaVencimiento = "2026-09-01",
            ),
        ),
        gestionesHistoricas = emptyList(),
    )

    @Test
    fun `reemplazo atomico inserta todo correctamente`() = runTest {
        val resultado = tx.reemplazar(bundle())
        assertTrue(resultado is ResultadoPersistencia.Exito)
        assertEquals("asig-1", db.asignacionDiariaDao().getActiva()?.id)
        val personas = db.personaDao().buscarPorRut("270000010")
        assertEquals(1, personas.size)
    }

    @Test
    fun `reemplazo atomico actualiza metadata con fechaConsultada`() = runTest {
        tx.reemplazar(bundle(fecha = "2026-08-02"))

        val meta = db.syncMetadataDao().getMetadata()
        assertEquals("EXITOSA", meta?.estado)
        assertEquals("2026-08-02", meta?.fechaConsultada)
        assertEquals("2026-08-02", meta?.fechaAsignacionAlmacenada)
        assertEquals("asig-1", meta?.asignacionIdAlmacenada)
        assertTrue(meta?.datosMarcadosComoDesactualizados == false)
    }

    @Test
    fun `segundo reemplazo borra el primero`() = runTest {
        tx.reemplazar(bundle(asigId = "asig-1", personaId = "p-1"))
        tx.reemplazar(bundle(asigId = "asig-2", fecha = "2026-08-03", personaId = "p-2"))

        val asig = db.asignacionDiariaDao().getActiva()
        assertEquals("asig-2", asig?.id)
        // persona del primer bundle no debe existir
        val personaVieja = db.personaDao().buscarPorRut("270000010")
        // p-2 tiene el mismo RUT en este test, so we check only 1 entry
        assertEquals(1, personaVieja.size)
    }

    @Test
    fun `marcarSinAsignacion actualiza metadata correctamente`() = runTest {
        tx.reemplazar(bundle())
        tx.marcarSinAsignacion("2026-08-03")

        val meta = db.syncMetadataDao().getMetadata()
        assertEquals("SIN_ASIGNACION", meta?.estado)
        assertEquals("2026-08-03", meta?.fechaConsultada)
        assertTrue(meta?.datosMarcadosComoDesactualizados == true)
        assertTrue(meta?.datosAnterioresDisponibles == true) // had previous data
        // Asignación anterior sigue en la BD
        assertEquals("asig-1", db.asignacionDiariaDao().getActiva()?.id)
    }

    @Test
    fun `limpiarTodo deja tablas vacias`() = runTest {
        tx.reemplazar(bundle())
        tx.limpiarTodo()

        assertTrue(db.asignacionDiariaDao().getActiva() == null)
        assertTrue(db.personaDao().buscarPorRut("270000010").isEmpty())
        assertTrue(db.syncMetadataDao().getMetadata() == null)
    }

    @Test
    fun `marcarSinAsignacion sin datos previos conserva estado`() = runTest {
        tx.marcarSinAsignacion("2026-08-02")

        val meta = db.syncMetadataDao().getMetadata()
        assertEquals("SIN_ASIGNACION", meta?.estado)
        assertTrue(meta?.datosAnterioresDisponibles == false)
    }
}
