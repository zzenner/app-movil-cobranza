package cl.zzenner.cobranza.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Singleton (id=1) que registra el estado de la última sincronización.
 *
 * estados posibles de [estado]:
 * - NONE: nunca se sincronizó
 * - EXITOSA: última descarga fue exitosa
 * - SIN_ASIGNACION: el servidor respondió 204 (no hay asignación para hoy)
 * - ERROR: último intento falló
 * - VERSION_NO_SOPORTADA: el contrato de la API no es compatible
 */
@Entity(tableName = "sync_metadata")
data class SyncMetadataEntity(
    @PrimaryKey val id: Long = 1L,
    val estado: String = "NONE",
    /** LocalDate ISO de la última consulta */
    val fechaConsultada: String? = null,
    /** Reservado para futura versión del contrato API — el servidor no lo devuelve actualmente */
    val versionContrato: String? = null,
    /** Reservado: epoch millis del timestamp del servidor — el servidor no lo devuelve actualmente */
    val generadoEn: Long? = null,
    /** Epoch millis de la última descarga exitosa */
    val ultimaDescargaExitosa: Long? = null,
    /** Epoch millis del último intento (exitoso o fallido) */
    val ultimoIntento: Long? = null,
    val codigoUltimoError: String? = null,
    /** Mensaje de error apto para mostrar al usuario (sin detalles técnicos) */
    val mensajeSeguro: String? = null,
    val asignacionIdAlmacenada: String? = null,
    /** LocalDate ISO de la asignación almacenada en la BD */
    val fechaAsignacionAlmacenada: String? = null,
    /** true si hay datos de una asignación anterior en la BD */
    val datosAnterioresDisponibles: Boolean = false,
    /** true si los datos en BD no corresponden a la asignación de hoy */
    val datosMarcadosComoDesactualizados: Boolean = false,
)
