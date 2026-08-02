package cl.zzenner.cobranza.core.database.transaction

import androidx.room.withTransaction
import cl.zzenner.cobranza.core.database.CobranzaDatabase
import cl.zzenner.cobranza.core.database.entity.SyncMetadataEntity
import javax.inject.Inject

sealed class ResultadoPersistencia {
    data object Exito : ResultadoPersistencia()
    data class Error(val mensaje: String) : ResultadoPersistencia()
}

/**
 * Ejecuta el reemplazo atómico de todos los datos descargados.
 *
 * Si cualquier paso falla, Room hace rollback automático y los datos
 * anteriores permanecen intactos.
 */
class BundleReplacementTransaction @Inject constructor(
    private val db: CobranzaDatabase,
) {

    /**
     * Reemplaza atómicamente todos los datos descargados.
     * Orden de operaciones:
     * 1. DELETE hijos (gestiones, cuotas, operaciones, direcciones, avales, cross-refs)
     * 2. DELETE padres (persona, asignacion_diaria)
     * 3. INSERT en orden inverso (asignacion → personas → relaciones → hijos)
     * 4. UPDATE sync_metadata → EXITOSA
     */
    suspend fun reemplazar(bundle: BundleDescargado): ResultadoPersistencia {
        return try {
            db.withTransaction {
                val gestionDao = db.gestionHistoricaDao()
                val operacionDao = db.operacionDao()
                val personaDao = db.personaDao()
                val asignacionDao = db.asignacionDiariaDao()
                val syncDao = db.syncMetadataDao()

                // ── Eliminar hijos en orden dependencia ────────────────────────
                gestionDao.deleteAll()
                operacionDao.deleteAllCuotas()
                operacionDao.deleteAll()
                personaDao.deleteAllDirecciones()
                personaDao.deleteAllAvales()
                asignacionDao.deleteAllCrossRefs()
                personaDao.deleteAll()
                asignacionDao.deleteAll()

                // ── Insertar en orden dependencia ──────────────────────────────
                asignacionDao.insert(bundle.asignacion)
                personaDao.insertAll(bundle.personas)
                asignacionDao.insertAllCrossRefs(bundle.crossRefs)
                personaDao.insertAllDirecciones(bundle.direcciones)
                personaDao.insertAllAvales(bundle.avales)
                operacionDao.insertAll(bundle.operaciones)
                operacionDao.insertAllCuotas(bundle.cuotas)
                gestionDao.insertAll(bundle.gestionesHistoricas)

                // ── Actualizar metadatos ───────────────────────────────────────
                val ahora = System.currentTimeMillis()
                syncDao.upsert(
                    SyncMetadataEntity(
                        id = 1L,
                        estado = "EXITOSA",
                        fechaConsultada = bundle.asignacion.fecha,
                        asignacionIdAlmacenada = bundle.asignacion.id,
                        fechaAsignacionAlmacenada = bundle.asignacion.fecha,
                        ultimaDescargaExitosa = ahora,
                        ultimoIntento = ahora,
                        datosAnterioresDisponibles = false,
                        datosMarcadosComoDesactualizados = false,
                    ),
                )
            }
            ResultadoPersistencia.Exito
        } catch (e: Exception) {
            ResultadoPersistencia.Error(e.message ?: "Error desconocido")
        }
    }

    /**
     * Registra que hoy no hay asignación activa (respuesta 204).
     * NO borra los datos anteriores; los marca como desactualizados.
     */
    suspend fun marcarSinAsignacion(fechaConsultada: String) {
        val actual = db.syncMetadataDao().getMetadata()
        val ahora = System.currentTimeMillis()
        db.syncMetadataDao().upsert(
            SyncMetadataEntity(
                id = 1L,
                estado = "SIN_ASIGNACION",
                fechaConsultada = fechaConsultada,
                ultimoIntento = ahora,
                // Conservar datos anteriores si existían
                asignacionIdAlmacenada = actual?.asignacionIdAlmacenada,
                fechaAsignacionAlmacenada = actual?.fechaAsignacionAlmacenada,
                ultimaDescargaExitosa = actual?.ultimaDescargaExitosa,
                datosAnterioresDisponibles = actual?.asignacionIdAlmacenada != null,
                datosMarcadosComoDesactualizados = true,
            ),
        )
    }

    /**
     * Limpia toda la base de datos. Usar en logout.
     */
    suspend fun limpiarTodo() {
        db.withTransaction {
            db.gestionHistoricaDao().deleteAll()
            db.operacionDao().deleteAllCuotas()
            db.operacionDao().deleteAll()
            db.personaDao().deleteAllDirecciones()
            db.personaDao().deleteAllAvales()
            db.asignacionDiariaDao().deleteAllCrossRefs()
            db.personaDao().deleteAll()
            db.asignacionDiariaDao().deleteAll()
            db.syncMetadataDao().deleteAll()
        }
    }
}
