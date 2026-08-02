package cl.zzenner.cobranza.ui

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import cl.zzenner.cobranza.core.database.CobranzaDatabase
import cl.zzenner.cobranza.core.database.entity.AsignacionDiariaEntity
import cl.zzenner.cobranza.core.database.entity.AsignacionPersonaCrossRef
import cl.zzenner.cobranza.core.database.entity.CuotaEntity
import cl.zzenner.cobranza.core.database.entity.DireccionEntity
import cl.zzenner.cobranza.core.database.entity.GestionHistoricaEntity
import cl.zzenner.cobranza.core.database.entity.OperacionEntity
import cl.zzenner.cobranza.core.database.entity.PersonaEntity
import cl.zzenner.cobranza.core.database.transaction.BundleDescargado
import cl.zzenner.cobranza.core.database.transaction.BundleReplacementTransaction
import cl.zzenner.cobranza.feature.asignacion.worker.AsignacionSyncScheduler
import cl.zzenner.cobranza.feature.auth.data.SessionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Verifica que el logout limpia completamente Room, cancela workers y cierra sesión.
 * Usa base de datos en memoria real (Robolectric) — sin mocks de Room.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LogoutIntegrationTest {

    private lateinit var db: CobranzaDatabase
    private lateinit var bundleTransaction: BundleReplacementTransaction
    private lateinit var scheduler: AsignacionSyncScheduler
    private lateinit var sessionRepository: SessionRepository
    private lateinit var useCase: LogoutUseCase

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            CobranzaDatabase::class.java,
        ).allowMainThreadQueries().build()

        bundleTransaction = BundleReplacementTransaction(db)

        scheduler = mockk { every { cancelarTodo() } just runs }
        sessionRepository = mockk { coEvery { logout() } just runs }

        useCase = LogoutUseCase(sessionRepository, bundleTransaction, scheduler)
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
                crossRefs = listOf(
                    AsignacionPersonaCrossRef(asignacionId = "asig-1", personaId = "p-1"),
                ),
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
                        id = "g-1", personaId = "p-1", asignacionDiariaId = "asig-1",
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

    @Test
    fun `logout elimina todos los datos financieros de Room`() = runTest {
        // 1. Insertar datos financieros
        insertarDatosFinancieros()

        // Confirmar que existen antes del logout
        assertEquals("asig-1", db.asignacionDiariaDao().getActiva()?.id)
        assertEquals(1, db.personaDao().buscarPorRut("270000010").size)

        // 2. Ejecutar logout
        useCase()

        // 3. Verificar que Room queda vacío
        assertNull("asignacion debe ser null", db.asignacionDiariaDao().getActiva())
        assertEquals("personas deben ser 0", 0, db.personaDao().buscarPorRut("270000010").size)
        assertNull("metadata debe ser null", db.syncMetadataDao().getMetadata())
    }

    @Test
    fun `logout cancela workers de sincronizacion`() = runTest {
        insertarDatosFinancieros()
        useCase()

        verify(exactly = 1) { scheduler.cancelarTodo() }
    }

    @Test
    fun `logout cierra sesion local`() = runTest {
        insertarDatosFinancieros()
        useCase()

        coVerify(exactly = 1) { sessionRepository.logout() }
    }

    @Test
    fun `logout fallo de red no impide limpieza local`() = runTest {
        // sessionRepository.logout() falla con IOException (sin red, token expirado, etc.)
        val sessionFallida: SessionRepository = mockk {
            coEvery { logout() } throws java.io.IOException("Sin red")
        }
        val useCaseFallido = LogoutUseCase(sessionFallida, bundleTransaction, scheduler)
        insertarDatosFinancieros()

        // LogoutUseCase usa runCatching en logout() → no debe propagarse excepción
        useCaseFallido()

        // Room sigue limpio aunque el logout remoto falló
        assertNull("asignacion debe ser null incluso con fallo de red", db.asignacionDiariaDao().getActiva())
        assertNull("metadata debe ser null incluso con fallo de red", db.syncMetadataDao().getMetadata())
    }
}
