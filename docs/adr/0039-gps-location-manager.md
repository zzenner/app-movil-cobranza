# ADR-0039 — GPS vía LocationManager (sin Google Play Services) (Fase 4C-A)

**Estado:** Aceptado — 2026-08-02
**Contexto:** Fase 4C-A — Gestiones offline desde ASIGNACION_DIARIA

---

## Contexto

La geolocalización es obligatoria para registrar una gestión (ADR-0010). Los dispositivos corporativos de Oriencoop pueden ser terminales Android sin Google Play Services. Necesitamos capturar la ubicación del ejecutivo en el momento de registrar la gestión, con un timeout razonable y detección de ubicación simulada (para detectar fraude).

## Decisión

### LocationManager en lugar de FusedLocationProviderClient

Se usa `android.location.LocationManager` (API estándar de Android) en lugar de `com.google.android.gms.location.FusedLocationProviderClient`. Razón: `FusedLocationProvider` requiere Google Play Services, que puede no estar presente en dispositivos corporativos.

```kotlin
val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
```

### Abstracción mediante interfaz

```kotlin
interface LocationProvider {
    suspend fun obtenerUbicacion(): ResultadoUbicacion
}
```

`AndroidLocationProvider` implementa la interfaz usando `LocationManager`. Los tests pueden sustituirla por un `FakeLocationProvider` sin tocar el código de producción.

### Una sola lectura; sin tracking continuo

La captura es puntual: `requestLocationUpdates` con `minTime=0`, `minDistance=0f`. El `LocationListener` se elimina inmediatamente al recibir la primera actualización. No se mantiene ningún listener activo después de la captura.

### Timeout de 30 segundos

```kotlin
withTimeoutOrNull(30_000L) {
    suspendCancellableCoroutine { cont -> ... }
} ?: ResultadoUbicacion.Timeout
```

Si transcurren 30 segundos sin recibir coordenadas, se retorna `ResultadoUbicacion.Timeout`. El formulario muestra el error y permite reintentar. El `cont.invokeOnCancellation` garantiza que el listener se elimine si el coroutine es cancelado por timeout o navegación.

### Detección de ubicación simulada

```kotlin
fun Location.esSimulada(): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) isMock
    else @Suppress("DEPRECATION") isFromMockProvider
```

API 31+ usa `Location.isMock`; API 29-30 usa el campo deprecado `isFromMockProvider`. El campo `ubicacionSimulada: Boolean` se almacena en `gestion_local` y se envía a la API para auditoría.

### Selección del proveedor

Se prefiere `LocationManager.GPS_PROVIDER` si está activo; si no, se acepta `NETWORK_PROVIDER`. Si ninguno está disponible, se retorna `ResultadoUbicacion.ProveedorDeshabilitado`.

### Permisos

Se comprueba `ACCESS_FINE_LOCATION` o `ACCESS_COARSE_LOCATION` antes de solicitar la ubicación. Si el permiso está denegado, se retorna `ResultadoUbicacion.PermisoDenegado`. La solicitud de permiso en tiempo de ejecución es responsabilidad de la UI (Compose `rememberPermissionState`).

## Consecuencias

- Compatible con dispositivos sin Google Play Services.
- La UI muestra estados claros: `Capturando`, `Capturado`, `Error(mensaje)`.
- El formulario no permite guardar una gestión hasta que se haya capturado la ubicación.
- `ubicacionSimulada = true` queda registrado en la BD y se envía a la API para auditoría de fraude.

## Alternativas descartadas

- **FusedLocationProviderClient:** descartado porque requiere Google Play Services. No todos los dispositivos corporativos lo tienen.
- **Última ubicación conocida (`getLastKnownLocation`):** descartado porque puede devolver una ubicación antigua (minutos u horas) que no refleja el punto de gestión real.
- **Tracking continuo mientras la app está en primer plano:** innecesario y costoso en batería. Solo se necesita la coordenada en el momento de guardar.
