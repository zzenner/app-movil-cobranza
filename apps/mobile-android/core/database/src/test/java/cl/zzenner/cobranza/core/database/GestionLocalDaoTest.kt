package cl.zzenner.cobranza.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import cl.zzenner.cobranza.core.database.entity.GestionLocalEntity
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
class GestionLocalDaoTest {

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

    private fun gestion(
        id: String = "g-1",
        personaId: String = "p-1",
        estado: String = "PENDIENTE_ENVIO",
        fechaCreacion: Long = 1000L,
        intentos: Int = 0,
        leaseHasta: Long? = null,
        proximoIntento: Long? = null,
    ) = GestionLocalEntity(
        id = id,
        personaId = personaId,
        personaRutNumero = "12345678",
        personaRutDv = "9",
        personaNombre = "Test Persona",
        asignacionDiariaId = "asig-1",
        origenGestion = "ASIGNACION_DIARIA",
        tipoGestion = "SIN_CONTACTO",
        fechaGestionEpoch = 1000L,
        fechaCapturaGpsEpoch = 1000L,
        observacion = null,
        observacionDireccion = null,
        latitud = -33.45,
        longitud = -70.66,
        precisionMetros = 5.0f,
        ubicacionSimulada = false,
        proveedorGps = "gps",
        fechaCompromiso = null,
        estadoSincronizacion = estado,
        fechaCreacionLocalEpoch = fechaCreacion,
        cantidadIntentos = intentos,
        leaseHastaEpoch = leaseHasta,
        fechaProximoIntentoEpoch = proximoIntento,
        codigoErrorServidor = null,
        mensajeError = null,
    )

    @Test
    fun `insert y getElegibles devuelve PENDIENTE_ENVIO sin backoff`() = runTest {
        val dao = db.gestionLocalDao()
        dao.insert(gestion())

        val elegibles = dao.getElegibles(ahora = 2000L, limite = 10)
        assertEquals(1, elegibles.size)
        assertEquals("g-1", elegibles[0].id)
    }

    @Test
    fun `getElegibles excluye registros con backoff activo`() = runTest {
        val dao = db.gestionLocalDao()
        dao.insert(gestion(estado = "ERROR_REINTENTABLE", proximoIntento = 99999L))

        val elegibles = dao.getElegibles(ahora = 1000L, limite = 10)
        assertTrue(elegibles.isEmpty())
    }

    @Test
    fun `getElegibles incluye ERROR_REINTENTABLE cuando backoff expiró`() = runTest {
        val dao = db.gestionLocalDao()
        dao.insert(gestion(id = "g-2", estado = "ERROR_REINTENTABLE", proximoIntento = 500L))

        val elegibles = dao.getElegibles(ahora = 1000L, limite = 10)
        assertEquals(1, elegibles.size)
    }

    @Test
    fun `adquirirLease devuelve 1 en adquisición exitosa`() = runTest {
        val dao = db.gestionLocalDao()
        dao.insert(gestion())

        val adquirida = dao.adquirirLease("g-1", leaseHasta = 60000L, ahora = 1000L)
        assertEquals(1, adquirida)

        val rows = dao.getElegibles(ahora = 1000L, limite = 10)
        assertTrue("Registro en ENVIANDO no debe aparecer como elegible", rows.isEmpty())
    }

    @Test
    fun `adquirirLease devuelve 0 si ya está ENVIANDO`() = runTest {
        val dao = db.gestionLocalDao()
        dao.insert(gestion(estado = "ENVIANDO", leaseHasta = 99999L))

        val adquirida = dao.adquirirLease("g-1", leaseHasta = 200000L, ahora = 1000L)
        assertEquals(0, adquirida)
    }

    @Test
    fun `recuperarLeasesExpirados restaura PENDIENTE_ENVIO solo en leases caducados`() = runTest {
        val dao = db.gestionLocalDao()
        dao.insert(gestion(id = "g-caducado", estado = "ENVIANDO", leaseHasta = 500L))
        dao.insert(gestion(id = "g-vigente", estado = "ENVIANDO", leaseHasta = 99999L))

        val recuperados = dao.recuperarLeasesExpirados(ahora = 1000L)
        assertEquals(1, recuperados)

        val elegibles = dao.getElegibles(ahora = 1000L, limite = 10)
        assertEquals(1, elegibles.size)
        assertEquals("g-caducado", elegibles[0].id)
    }

    @Test
    fun `actualizarEstado a SINCRONIZADA limpia lease y backoff`() = runTest {
        val dao = db.gestionLocalDao()
        dao.insert(gestion(estado = "ENVIANDO", leaseHasta = 99999L))
        dao.actualizarEstado("g-1", "SINCRONIZADA", 1, null, null, null)

        val elegibles = dao.getElegibles(ahora = 99999L, limite = 10)
        assertTrue("SINCRONIZADA no debe ser elegible", elegibles.isEmpty())
    }

    @Test
    fun `contarNoResueltas excluye SINCRONIZADA`() = runTest {
        val dao = db.gestionLocalDao()
        dao.insert(gestion(id = "g-ok", estado = "SINCRONIZADA"))
        dao.insert(gestion(id = "g-err", estado = "ERROR_REINTENTABLE"))
        dao.insert(gestion(id = "g-pend", estado = "PENDIENTE_ENVIO"))

        assertEquals(2, dao.contarNoResueltas())
    }

    @Test
    fun `getElegibles respeta orden FIFO por fechaCreacionLocalEpoch`() = runTest {
        val dao = db.gestionLocalDao()
        dao.insert(gestion(id = "g-tarde", fechaCreacion = 3000L))
        dao.insert(gestion(id = "g-temprana", fechaCreacion = 1000L))
        dao.insert(gestion(id = "g-media", fechaCreacion = 2000L))

        val elegibles = dao.getElegibles(ahora = 5000L, limite = 10)
        assertEquals("g-temprana", elegibles[0].id)
        assertEquals("g-media", elegibles[1].id)
        assertEquals("g-tarde", elegibles[2].id)
    }

    @Test
    fun `deleteAll elimina todos los registros`() = runTest {
        val dao = db.gestionLocalDao()
        dao.insert(gestion(id = "g-1"))
        dao.insert(gestion(id = "g-2"))
        dao.deleteAll()

        assertEquals(0, dao.contarNoResueltas())
    }

    @Test
    fun `contarNoResueltas cuenta ENVIANDO`() = runTest {
        val dao = db.gestionLocalDao()
        dao.insert(gestion(estado = "ENVIANDO", leaseHasta = 99999L))
        assertEquals(1, dao.contarNoResueltas())
    }

    @Test
    fun `contarNoResueltas cuenta ERROR_PERMANENTE`() = runTest {
        val dao = db.gestionLocalDao()
        dao.insert(gestion(estado = "ERROR_PERMANENTE"))
        assertEquals(1, dao.contarNoResueltas())
    }

    @Test
    fun `contarNoResueltas cuenta CONFLICTO`() = runTest {
        val dao = db.gestionLocalDao()
        dao.insert(gestion(estado = "CONFLICTO"))
        assertEquals(1, dao.contarNoResueltas())
    }

    @Test
    fun `contarNoResueltas cuenta los cinco estados no resueltos y excluye SINCRONIZADA`() = runTest {
        val dao = db.gestionLocalDao()
        dao.insert(gestion(id = "g-1", estado = "PENDIENTE_ENVIO"))
        dao.insert(gestion(id = "g-2", estado = "ENVIANDO", leaseHasta = 99999L))
        dao.insert(gestion(id = "g-3", estado = "ERROR_REINTENTABLE"))
        dao.insert(gestion(id = "g-4", estado = "ERROR_PERMANENTE"))
        dao.insert(gestion(id = "g-5", estado = "CONFLICTO"))
        dao.insert(gestion(id = "g-6", estado = "SINCRONIZADA"))

        assertEquals("Todos los estados no resueltos deben contar", 5, dao.contarNoResueltas())
    }
}
