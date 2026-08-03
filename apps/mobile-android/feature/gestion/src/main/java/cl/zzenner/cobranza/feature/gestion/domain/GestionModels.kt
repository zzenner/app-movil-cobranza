package cl.zzenner.cobranza.feature.gestion.domain

enum class TipoGestion {
    SIN_CONTACTO,
    CONTACTO_FAMILIAR,
    COMPROMISO_PAGO,
}

enum class OrigenGestion {
    ASIGNACION_DIARIA,
    BUSQUEDA_DIRECTA,
}

enum class EstadoSincronizacion {
    PENDIENTE_ENVIO,
    ENVIANDO,
    SINCRONIZADA,
    ERROR_REINTENTABLE,
    ERROR_PERMANENTE,
    CONFLICTO,
}

data class UbicacionCapturada(
    val latitud: Double,
    val longitud: Double,
    val precisionMetros: Float,
    val ubicacionSimulada: Boolean,
    val proveedorGps: String?,
    val fechaCapturaEpoch: Long,
)

data class GestionForm(
    val personaId: String,
    val personaRutNumero: String,
    val personaRutDv: String,
    val personaNombre: String,
    val origenGestion: OrigenGestion,
    val asignacionDiariaId: String?,
    val tipoGestion: TipoGestion,
    val observacion: String?,
    val observacionDireccion: String?,
    val fechaCompromiso: String?,
    val ubicacion: UbicacionCapturada,
)

data class GestionResumen(
    val id: String,
    val tipoGestion: TipoGestion,
    val fechaGestionEpoch: Long,
    val observacion: String?,
    val estadoSincronizacion: EstadoSincronizacion,
    val fechaCompromiso: String?,
    val esLocal: Boolean,
)

sealed class ResultadoEnvio {
    data class Exito(val idServidor: String, val resultado: String) : ResultadoEnvio()
    data object SesionRequerida : ResultadoEnvio()
    data class ErrorReintentar(val codigo: String?, val mensaje: String?) : ResultadoEnvio()
    data class ErrorPermanente(val codigo: String?, val mensaje: String?) : ResultadoEnvio()
    data class Conflicto(val codigo: String?, val mensaje: String?) : ResultadoEnvio()
}

sealed class ResultadoProcesamiento {
    data object Completado : ResultadoProcesamiento()
    data class ErroresParciales(val fallidas: Int, val total: Int) : ResultadoProcesamiento()
    data object SinPendientes : ResultadoProcesamiento()
    data object SesionExpirada : ResultadoProcesamiento()
}
