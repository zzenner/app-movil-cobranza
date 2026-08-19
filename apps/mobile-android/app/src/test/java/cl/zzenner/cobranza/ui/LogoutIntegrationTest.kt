package cl.zzenner.cobranza.ui

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import cl.zzenner.cobranza.core.database.CobranzaDatabase
import cl.zzenner.cobranza.core.database.entity.AsignacionDiariaEntity
import cl.zzenner.cobranza.core.database.entity.AsignacionPersonaCrossRef
import cl.zzenner.cobranza.core.database.entity.CuotaEntity
import cl.zzenner.cobranza.core.database.entity.DireccionEntity
import cl.zzenner.cobranza.core.database.entity.GestionHistoricaEntity
import cl.zzenner.cobranza.core.database.entity.GestionLocalEntity
import cl.zzenner.cobranza.core.database.entity.OperacionEntity
import cl.zzenner.cobranza.core.database.entity.PersonaEntity
import cl.zzenner.cobranza.core.database.transaction.BundleDescargado
import cl.zzenner.cobranza.core.database.transaction.BundleReplacementTransaction
import cl.zzenner.cobranza.feature.asignacion.worker.AsignacionSyncScheduler
import cl.zzenner.cobranza.feature.auth.data.SessionRepository
import cl.zzenner.cobranza.feature.gestion.worker.GestionSyncScheduler
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.first
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
class LogoutIntegrationTest {

    private lateinit var db: CobranzaDatabase
    private lateinit var bundleTransaction: BundleReplacementTransaction
    private lateinit var asignacionScheduler: AsignacionSyncScheduler
    private lateinit var gestionScheduler: GestionSyncScheduler
    private lateinit var sessionRepository: SessionRepository
    private lateinit var useCase: LogoutUseCase

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            CobranzaDatabase::class.java,
        ).allowMainThreadQueries().build()

        bundleTransaction = BundleReplacementTransaction(db)
        asignacionScheduler = mockk { every { cancelarTodo() } just runs }
        gestionScheduler = mockk { every { cancelarTodo() } just runs }
        sessionRepository = mockk { coEvery { logout() } just runs }

        useCase = LogoutUseCase(sessionRepository, bundleTransaction, asignacionScheduler, gestionScheduler)
    }

    @After
    fun teardown() {
        db.close()
    }

    private suspend fun insertarDatosFinancieros() {
        bundleTransaction.reemplazar(
            BundleDescargado(
                asignacion = AsignacionDiariaEntity(
                    id = "asig-1",
                    ejecutivoId = "eje-1",
                    fecha = "2026-08-02",
                    estado = "PUBLICADA",
                    fechaDescargaEpoch = 1000L,
                ),
                personas = listOf(
                    PersonaEntity(id = "p-1", rutNumero = "27000001", rutDv = "0", nombre = "Test"),
                ),
                crossRefs = listOf(AsignacionPersonaCrossRef("asig-1", "p-1")),
                direcciones = listOf(
                    DireccionEntity(
                        personaId = "p-1", tipo = "DOMICILIO", texto = "Av. Test 123",
                        comuna = null, ciudad = null, vigente = true,
                    ),
                ),
                avales = emptyList(),
                operaciones = listOf(
                    OperacionEntity(
                        id = "op-1", personaId = "p-1", numeroOperacion = "OP001",
                        estado = "VIGENTE", capital = "500000", interesPenal = "0",
                        gastosCobranza = "0", totalVigente = "500000",
                    ),
                ),
                cuotas = listOf(
                    CuotaEntity(
                        operacionId = "op-1", numeroCuota = 1, estado = "VIGENTE",
                        montoTotal = "50000", saldo = "50000", fechaVencimiento = "2026-09-01",
                    ),
                ),
                gestionesHistoricas = listOf(
                    GestionHistoricaEntity(
                        id = "g-hist-1", personaId = "p-1", asignacionDiariaId = "asig-1",
                        ejecutivoId = "eje-1", origenGestion = "ASIGNACION_DIARIA",
                        tipoGestion = "SIN_CONTACTO", fechaGestion = 1000L,
                        observacion = null, observacionDireccion = null,
                        latitud = -33.45, longitud = -70.66, precisionMetros = 10.0f,
                        proveedorGps = null, ubicacionSimulada = false,
                        fechaCapturaGps = 1000L, fechaCompromiso = null,
                        fechaCreacionServidor = 1000L,
                    ),
                ),
            ),
        )
    }

    private suspend fun insertarGestionLocal(
        id: String = "g-local-1",
        estado: String = "PENDIENTE_ENVIO",
    ) {
        db.gestionLocalDao().insert(
            GestionLocalEntity(
                id = id,
                personaId = "p-1",
                personaRutNumero = "27000001",
                personaRutDv = "0",
                personaNombre = "Test",
                asignacionDiariaId = "asig-1",
                origenGestion = "ASIGNACION_DIARIA",
                tipoGestion = "SIN_CONTACTO",
                fechaGestionEpoch = 2000L,
                fechaCapturaGpsEpoch = 2000L,
                observacion = null,
                observacionDireccion = null,
                latitud = -33.45,
                longitud = -70.66,
                precisionMetros = 5.0f,
                ubicacionSimulada = false,
                proveedorGps = "gps",
                fechaCompromiso = null,
                estadoSincronizacion = estado,
                fechaCreacionLocalEpoch = 2000L,
                cantidadIntentos = 0,
                leaseHastaEpoch = null,
                fechaProximoIntentoEpoch = null,
                codigoErrorServidor = null,
                mensajeError = null,
            ),
        )
    }

    @Test
    fun `logout elimina datos financieros pero preserva gestiones no sincronizadas`() = runTest {
        insertarDatosFinancieros()
        insertarGestionLocal(id = "g-pendiente", estado = "PENDIENTE_ENVIO")
        insertarGestionLocal(id = "g-permanente", estado = "ERROR_PERMANENTE")

        assertEquals("asig-1", db.asignacionDiariaDao().getActiva()?.id)
        assertEquals(2, db.gestionLocalDao().contarNoResueltas())

        useCase()

        // Datos financieros (caché re-descargable): se limpian por completo.
        assertNull(db.asignacionDiariaDao().getActiva())
        assertNull(db.syncMetadataDao().getMetadata())
        // Gestiones no sincronizadas (única copia local): RN-24 prohíbe perderlas en logout.
        assertEquals(2, db.gestionLocalDao().contarNoResueltas())
    }

    @Test
    fun `logout elimina gestiones ya sincronizadas`() = runTest {
        insertarDatosFinancieros()
        insertarGestionLocal(id = "g-sincronizada", estado = "SINCRONIZADA")

        assertEquals(1, db.gestionLocalDao().getGestionesPorPersona("p-1").first().size)

        useCase()

        assertEquals(0, db.gestionLocalDao().getGestionesPorPersona("p-1").first().size)
    }

    @Test
    fun `logout cancela ambos schedulers`() = runTest {
        insertarDatosFinancieros()
        useCase()

        verify(exactly = 1) { asignacionScheduler.cancelarTodo() }
        verify(exactly = 1) { gestionScheduler.cancelarTodo() }
    }

    @Test
    fun `logout cierra sesion remota`() = runTest {
        insertarDatosFinancieros()
        useCase()

        coVerify(exactly = 1) { sessionRepository.logout() }
    }

    @Test
    fun `logout fallo de red no impide limpieza local`() = runTest {
        val sessionFallida: SessionRepository = mockk {
            coEvery { logout() } throws java.io.IOException("Sin red")
        }
        val useCaseFallido = LogoutUseCase(
            sessionFallida, bundleTransaction, asignacionScheduler, gestionScheduler,
        )
        insertarDatosFinancieros()
        insertarGestionLocal()

        useCaseFallido()

        assertNull(db.asignacionDiariaDao().getActiva())
        // La gestión pendiente no se pierde aunque el logout remoto haya fallado.
        assertEquals(1, db.gestionLocalDao().contarNoResueltas())
    }
}
