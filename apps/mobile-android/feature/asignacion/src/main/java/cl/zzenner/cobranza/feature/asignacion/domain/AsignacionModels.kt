package cl.zzenner.cobranza.feature.asignacion.domain

/**
 * Modelos de dominio para la feature de asignación.
 * Sin dependencias de Android — solo Kotlin puro.
 */

/** Resumen de persona para la lista de asignación */
data class PersonaResumen(
    val id: String,
    val nombre: String,
    val rutFormateado: String,
    val numOperaciones: Int,
)

/** Estado de la sincronización para mostrar en UI */
data class SyncMetadataUi(
    val estado: String = "NONE",
    val ultimaDescargaExitosa: Long? = null,
    /** Fecha formateada como "dd/MM/yyyy HH:mm", lista para mostrar directamente. */
    val ultimaDescargaFormateada: String? = null,
    val datosAnterioresDisponibles: Boolean = false,
    val datosMarcadosComoDesactualizados: Boolean = false,
    val fechaAsignacionAlmacenada: String? = null,
)

sealed class ResultadoDescarga {
    data class Exito(val fechaAsignacion: String) : ResultadoDescarga()
    data class SinAsignacion(val fechaConsultada: String) : ResultadoDescarga()
    data class ErrorReintentar(val causa: String) : ResultadoDescarga()
    data class ErrorPermanente(val causa: String) : ResultadoDescarga()
    data class VersionNoSoportada(val version: String) : ResultadoDescarga()
}

/** Normaliza un RUT para búsqueda: elimina puntos, guión y espacios; K→k */
fun normalizarRut(rut: String): String =
    rut.replace(".", "").replace("-", "").replace(" ", "").lowercase()

/** Formatea un RUT para mostrar: "12.345.678-9" */
fun formatearRut(rutNumero: String, rutDv: String): String {
    val numero = rutNumero.reversed().chunked(3).joinToString(".").reversed()
    return "$numero-${rutDv.uppercase()}"
}
