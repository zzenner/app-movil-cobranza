package cl.zzenner.cobranza.feature.asignacion.data

import cl.zzenner.cobranza.core.database.dao.AsignacionDiariaDao
import cl.zzenner.cobranza.core.database.dao.PersonaConDetalle
import cl.zzenner.cobranza.core.database.dao.SyncMetadataDao
import cl.zzenner.cobranza.core.database.entity.SyncMetadataEntity
import cl.zzenner.cobranza.core.database.transaction.BundleReplacementTransaction
import cl.zzenner.cobranza.core.database.transaction.ResultadoPersistencia
import cl.zzenner.cobranza.core.network.api.SincronizacionApi
import cl.zzenner.cobranza.feature.asignacion.domain.ResultadoDescarga
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerializationException
import java.io.IOException
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AsignacionRepository @Inject constructor(
    private val sincronizacionApi: SincronizacionApi,
    private val bundleTransaction: BundleReplacementTransaction,
    private val asignacionDiariaDao: AsignacionDiariaDao,
    private val personaDao: cl.zzenner.cobranza.core.database.dao.PersonaDao,
    private val syncMetadataDao: SyncMetadataDao,
    private val mapper: AsignacionMapper,
) {

    /** Single-flight: evita dos reemplazos concurrentes */
    private val mutex = Mutex()

    // ── Descarga ───────────────────────────────────────────────────────────────

    suspend fun descargarAsignacion(): ResultadoDescarga = mutex.withLock {
        val ahora = System.currentTimeMillis()
        val metadataActual = syncMetadataDao.getMetadata()

        try {
            val response = sincronizacionApi.obtenerAsignacionDiariaActiva(fecha = null)

            when {
                response.code() == 204 -> {
                    val fechaHoy = LocalDate.now().toString()
                    bundleTransaction.marcarSinAsignacion(fechaHoy)
                    ResultadoDescarga.SinAsignacion(fechaHoy)
                }

                response.isSuccessful -> {
                    val dto = response.body()
                        ?: return@withLock ResultadoDescarga.ErrorPermanente("Cuerpo vacío inesperado en respuesta 200")

                    val bundle = mapper.map(dto, ahora)
                    when (val resultado = bundleTransaction.reemplazar(bundle)) {
                        is ResultadoPersistencia.Exito ->
                            ResultadoDescarga.Exito(bundle.asignacion.fecha)

                        is ResultadoPersistencia.Error ->
                            ResultadoDescarga.ErrorReintentar(resultado.mensaje)
                    }
                }

                response.code() == 401 -> {
                    // El autenticador de OkHttp (SingleFlightAuthenticator) ya intentó renovar.
                    // Si llegamos aquí, el refresh también falló → sesión inválida.
                    registrarError(ahora, "401", "Sesión expirada")
                    ResultadoDescarga.ErrorPermanente("Sesión inválida — vuelva a iniciar sesión")
                }

                response.code() == 403 ->
                    ResultadoDescarga.ErrorPermanente("Acceso denegado — sin rol EJECUTIVO_TERRENO")

                response.code() in 500..599 -> {
                    registrarError(ahora, response.code().toString(), "Error del servidor")
                    ResultadoDescarga.ErrorReintentar("Error del servidor (${response.code()})")
                }

                else -> {
                    registrarError(ahora, response.code().toString(), "Error HTTP inesperado")
                    ResultadoDescarga.ErrorPermanente("Error HTTP ${response.code()}")
                }
            }
        } catch (e: SerializationException) {
            registrarError(ahora, "JSON_INVALIDO", "Error de formato en la respuesta")
            ResultadoDescarga.ErrorPermanente("JSON inválido: ${e.message}")
        } catch (e: IOException) {
            registrarError(ahora, "IO_ERROR", "Sin conexión a internet")
            ResultadoDescarga.ErrorReintentar("Error de red: ${e.message}")
        }
    }

    private suspend fun registrarError(ahora: Long, codigo: String, mensaje: String) {
        val actual = syncMetadataDao.getMetadata() ?: SyncMetadataEntity()
        syncMetadataDao.upsert(
            actual.copy(
                estado = "ERROR",
                ultimoIntento = ahora,
                codigoUltimoError = codigo,
                mensajeSeguro = mensaje,
            ),
        )
    }

    // ── Observación de datos locales ───────────────────────────────────────────

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observePersonasDeAsignacion(): Flow<List<PersonaConDetalle>> =
        asignacionDiariaDao.observeActiva().flatMapLatest { asignacion ->
            if (asignacion != null) {
                personaDao.getPersonasDeAsignacion(asignacion.id)
            } else {
                flowOf(emptyList())
            }
        }

    fun observeSyncMetadata(): Flow<SyncMetadataEntity?> =
        syncMetadataDao.observeMetadata()
}
