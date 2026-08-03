package cl.zzenner.cobranza.feature.gestion.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Looper
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import kotlin.coroutines.resume

/**
 * Implementación usando LocationManager del SDK de Android.
 * No depende de Google Play Services — compatible con dispositivos AOSP.
 */
class AndroidLocationProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) : LocationProvider {

    companion object {
        private const val TIMEOUT_MILLIS = 30_000L
        private const val LEASE_DURATION_MILLIS = 0L
        private const val MIN_DISTANCE_METERS = 0f
    }

    @SuppressLint("MissingPermission")
    override suspend fun obtenerUbicacion(): ResultadoUbicacion {
        if (!tienePermiso()) return ResultadoUbicacion.PermisoDenegado

        val locationManager =
            context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val provider = elegirProveedor(locationManager)
            ?: return ResultadoUbicacion.ProveedorDeshabilitado

        return withTimeoutOrNull(TIMEOUT_MILLIS) {
            suspendCancellableCoroutine { cont ->
                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        locationManager.removeUpdates(this)
                        if (cont.isActive) cont.resume(ResultadoUbicacion.Exito(location))
                    }

                    @Deprecated("Deprecated in API 29+", ReplaceWith(""))
                    override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) = Unit

                    override fun onProviderDisabled(provider: String) {
                        locationManager.removeUpdates(this)
                        if (cont.isActive) cont.resume(ResultadoUbicacion.ProveedorDeshabilitado)
                    }

                    override fun onProviderEnabled(provider: String) = Unit
                }

                locationManager.requestLocationUpdates(
                    provider,
                    LEASE_DURATION_MILLIS,
                    MIN_DISTANCE_METERS,
                    listener,
                    Looper.getMainLooper(),
                )

                cont.invokeOnCancellation {
                    locationManager.removeUpdates(listener)
                }
            }
        } ?: ResultadoUbicacion.Timeout
    }

    private fun tienePermiso(): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }

    private fun elegirProveedor(locationManager: LocationManager): String? {
        val gpsHabilitado = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val networkHabilitado = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        return when {
            gpsHabilitado -> LocationManager.GPS_PROVIDER
            networkHabilitado -> LocationManager.NETWORK_PROVIDER
            else -> null
        }
    }
}

/**
 * Detecta si la ubicación proviene de una fuente simulada (modo desarrollador o app de mock GPS).
 * Cubre API 29-30 (deprecated but necessary) y API 31+.
 */
fun Location.esSimulada(): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        isMock
    } else {
        @Suppress("DEPRECATION")
        isFromMockProvider
    }
