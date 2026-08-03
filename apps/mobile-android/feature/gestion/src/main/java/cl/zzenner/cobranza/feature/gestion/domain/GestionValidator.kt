package cl.zzenner.cobranza.feature.gestion.domain

sealed class ErrorValidacion {
    data object TipoGestionRequerido : ErrorValidacion()
    data object FechaCompromisoRequerida : ErrorValidacion()
    data object FechaCompromisoInvalida : ErrorValidacion()
    data object UbicacionRequerida : ErrorValidacion()
    data object OrigenIncoherente : ErrorValidacion()
    data class ObservacionDemasiadoLarga(val max: Int) : ErrorValidacion()
    data class ObservacionDireccionDemasiadoLarga(val max: Int) : ErrorValidacion()
}

object GestionValidator {

    private const val MAX_OBSERVACION = 500
    private const val MAX_OBSERVACION_DIRECCION = 200

    fun validar(form: GestionForm, fechaHoyIso: String): List<ErrorValidacion> {
        val errores = mutableListOf<ErrorValidacion>()

        if (form.origenGestion == OrigenGestion.ASIGNACION_DIARIA && form.asignacionDiariaId == null) {
            errores += ErrorValidacion.OrigenIncoherente
        }
        if (form.origenGestion == OrigenGestion.BUSQUEDA_DIRECTA && form.asignacionDiariaId != null) {
            errores += ErrorValidacion.OrigenIncoherente
        }

        if (form.observacion != null && form.observacion.length > MAX_OBSERVACION) {
            errores += ErrorValidacion.ObservacionDemasiadoLarga(MAX_OBSERVACION)
        }

        if (form.observacionDireccion != null && form.observacionDireccion.length > MAX_OBSERVACION_DIRECCION) {
            errores += ErrorValidacion.ObservacionDireccionDemasiadoLarga(MAX_OBSERVACION_DIRECCION)
        }

        if (form.tipoGestion == TipoGestion.COMPROMISO_PAGO) {
            val fecha = form.fechaCompromiso
            if (fecha.isNullOrBlank()) {
                errores += ErrorValidacion.FechaCompromisoRequerida
            } else if (fecha < fechaHoyIso) {
                errores += ErrorValidacion.FechaCompromisoInvalida
            }
        }

        return errores
    }
}
