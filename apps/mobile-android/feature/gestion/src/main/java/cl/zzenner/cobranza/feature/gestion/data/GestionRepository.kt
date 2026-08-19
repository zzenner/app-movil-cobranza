package cl.zzenner.cobranza.feature.gestion.data

import cl.zzenner.cobranza.core.database.dao.GestionHistoricaDao
import cl.zzenner.cobranza.core.database.dao.GestionLocalDao
import cl.zzenner.cobranza.core.database.entity.GestionLocalEntity
import cl.zzenner.cobranza.core.network.api.GestionApi
import cl.zzenner.cobranza.feature.gestion.domain.EstadoSincronizacion
import cl.zzenner.cobranza.feature.gestion.domain.GestionForm
import cl.zzenner.cobranza.feature.gestion.domain.GestionResumen
import cl.zzenner.cobranza.feature.gestion.domain.ResultadoEnvio
import cl.zzenner.cobranza.feature.gestion.domain.ResultadoProcesamiento
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

@Singleton
class GestionRepository @Inject constructor(
    private val gestionLocalDao: GestionLocalDao,
    private val gestionHistoricaDao: GestionHistoricaDao,
    private val gestionApi: GestionApi,
) {

    companion object {
        private const val LEASE_DURATION_MILLIS = 5 * 60 * 1000L
        private const val BACKOFF_BASE_MILLIS = 30_000L
        private const val MAX_BACKOFF_MILLIS = 24 * 60 * 60 * 1000L
        private const val LOTE_MAX = 20
    }

    private val mutex = Mutex()

    suspend fun guardarLocal(form: GestionForm): String {
        val id = UUID.randomUUID()
        val ahora = System.currentTimeMillis()
        gestionLocalDao.insert(GestionMapper.formToEntity(form, id, ahora))
        return id.toString()
    }

    suspend fun procesarOutbox(): ResultadoProcesamiento = mutex.withLock {
        val ahora = System.currentTimeMillis()

        gestionLocalDao.recuperarLeasesExpirados(ahora)

        val elegibles = gestionLocalDao.getElegibles(ahora, LOTE_MAX)
        if (elegibles.isEmpty()) return ResultadoProcesamiento.SinPendientes

        var exitosas = 0
        var fallidas = 0

        for (candidata in elegibles) {
            val leaseHasta = System.currentTimeMillis() + LEASE_DURATION_MILLIS
            val adquirida = gestionLocalDao.adquirirLease(candidata.id, leaseHasta, System.currentTimeMillis())
            if (adquirida == 0) continue

            when (val resultado = enviarGestion(candidata)) {
                is ResultadoEnvio.Exito -> {
                    gestionLocalDao.actualizarEstado(
                        candidata.id, EstadoSincronizacion.SINCRONIZADA.name,
                        candidata.cantidadIntentos, null, null, null,
                    )
                    exitosas++
                }

                is ResultadoEnvio.SesionRequerida -> {
                    gestionLocalDao.actualizarEstado(
                        candidata.id, EstadoSincronizacion.PENDIENTE_ENVIO.name,
                        candidata.cantidadIntentos, null, null, null,
                    )
                    return ResultadoProcesamiento.SesionExpirada
                }

                is ResultadoEnvio.ErrorReintentar -> {
                    val intentos = candidata.cantidadIntentos + 1
                    val delay = min(BACKOFF_BASE_MILLIS * (1L shl intentos), MAX_BACKOFF_MILLIS)
                    gestionLocalDao.actualizarEstado(
                        candidata.id, EstadoSincronizacion.ERROR_REINTENTABLE.name,
                        intentos, System.currentTimeMillis() + delay,
                        resultado.codigo, resultado.mensaje,
                    )
                    fallidas++
                }

                is ResultadoEnvio.ErrorPermanente -> {
                    gestionLocalDao.actualizarEstado(
                        candidata.id, EstadoSincronizacion.ERROR_PERMANENTE.name,
                        candidata.cantidadIntentos + 1, null,
                        resultado.codigo, resultado.mensaje,
                    )
                    fallidas++
                }

                is ResultadoEnvio.Conflicto -> {
                    gestionLocalDao.actualizarEstado(
                        candidata.id, EstadoSincronizacion.CONFLICTO.name,
                        candidata.cantidadIntentos + 1, null,
                        resultado.codigo, resultado.mensaje,
                    )
                    fallidas++
                }
            }
        }

        return if (fallidas > 0) ResultadoProcesamiento.ErroresParciales(fallidas, elegibles.size)
        else ResultadoProcesamiento.Completado
    }

    private suspend fun enviarGestion(entity: GestionLocalEntity): ResultadoEnvio {
        return try {
            val response = gestionApi.crearGestion(GestionMapper.entityToDto(entity))
            when {
                response.code() == 201 || response.code() == 200 -> {
                    val body = response.body()
                    ResultadoEnvio.Exito(
                        idServidor = body?.id ?: entity.id,
                        resultado = body?.resultado ?: "INSERTADA",
                    )
                }
                response.code() == 401 -> ResultadoEnvio.SesionRequerida
                response.code() == 409 -> ResultadoEnvio.Conflicto(
                    codigo = "CONFLICTO",
                    mensaje = response.errorBody()?.string(),
                )
                response.code() == 422 -> ResultadoEnvio.ErrorPermanente(
                    codigo = "VALIDACION",
                    mensaje = response.errorBody()?.string(),
                )
                response.code() >= 500 -> ResultadoEnvio.ErrorReintentar(
                    codigo = "HTTP_${response.code()}",
                    mensaje = response.message(),
                )
                else -> ResultadoEnvio.ErrorPermanente(
                    codigo = "HTTP_${response.code()}",
                    mensaje = response.message(),
                )
            }
        } catch (e: IOException) {
            ResultadoEnvio.ErrorReintentar(codigo = "IO_ERROR", mensaje = e.message)
        }
    }

    fun observarGestionesPorPersona(personaId: String): Flow<List<GestionResumen>> =
        combine(
            gestionLocalDao.getGestionesPorPersona(personaId),
            gestionHistoricaDao.getGestionesDePersona(personaId),
        ) { locales, historicas ->
            val idsLocales = locales.map { it.id }.toSet()
            val resultado = mutableListOf<GestionResumen>()

            locales.forEach { resultado += GestionMapper.localEntityToResumen(it) }
            historicas.forEach { if (it.id !in idsLocales) resultado += GestionMapper.historicaEntityToResumen(it) }

            resultado.sortedByDescending { it.fechaGestionEpoch }
        }

    suspend fun contarNoResueltas(): Int = gestionLocalDao.contarNoResueltas()

    /** Aún elegibles para el worker (backoff/reintento automático). */
    suspend fun contarReintentables(): Int = gestionLocalDao.contarReintentables()

    /** ERROR_PERMANENTE/CONFLICTO — el worker nunca las vuelve a seleccionar (ver getElegibles). */
    suspend fun contarNoRecuperables(): Int = gestionLocalDao.contarNoRecuperables()
}
