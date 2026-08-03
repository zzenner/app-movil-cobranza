package cl.zzenner.cobranza.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Gestión creada en el dispositivo, pendiente de sincronizar con el servidor.
 * Sin FK a persona: BundleReplacementTransaction puede reemplazar la tabla persona libremente.
 * Los campos personaRut* y personaNombre se desnormalizan para mantener el contexto de display
 * incluso si el bundle se descarga nuevamente.
 */
@Entity(
    tableName = "gestion_local",
    indices = [
        Index("personaId"),
        Index("estadoSincronizacion"),
        Index("fechaCreacionLocalEpoch"),
        Index(value = ["estadoSincronizacion", "fechaProximoIntentoEpoch"]),
    ],
)
data class GestionLocalEntity(
    @PrimaryKey val id: String,
    val personaId: String,
    val personaRutNumero: String,
    val personaRutDv: String,
    val personaNombre: String,
    val asignacionDiariaId: String?,
    val origenGestion: String,
    val tipoGestion: String,
    val fechaGestionEpoch: Long,
    val fechaCapturaGpsEpoch: Long,
    val observacion: String?,
    val observacionDireccion: String?,
    val latitud: Double,
    val longitud: Double,
    val precisionMetros: Float,
    val ubicacionSimulada: Boolean,
    val proveedorGps: String?,
    val fechaCompromiso: String?,
    val estadoSincronizacion: String,
    val fechaCreacionLocalEpoch: Long,
    val cantidadIntentos: Int,
    val leaseHastaEpoch: Long?,
    val fechaProximoIntentoEpoch: Long?,
    val codigoErrorServidor: String?,
    val mensajeError: String?,
)
