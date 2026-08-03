package cl.zzenner.cobranza.feature.gestion.location

import android.location.Location

sealed class ResultadoUbicacion {
    data class Exito(val location: Location) : ResultadoUbicacion()
    data object PermisoDenegado : ResultadoUbicacion()
    data object Timeout : ResultadoUbicacion()
    data object ProveedorDeshabilitado : ResultadoUbicacion()
}

interface LocationProvider {
    suspend fun obtenerUbicacion(): ResultadoUbicacion
}
