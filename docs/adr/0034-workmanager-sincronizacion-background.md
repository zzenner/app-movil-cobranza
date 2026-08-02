# ADR-0034 — WorkManager para sincronización de background (Fase 4B)

**Estado:** Aceptado — 2026-08-02
**Contexto:** Fase 4B — Cartera offline

---

## Contexto

La descarga de la asignación diaria debe ejecutarse en segundo plano, sobrevivir reinicios del proceso y reintentar automáticamente en caso de fallo de red. Android no permite trabajo en background sin restricciones; se requiere una solución compatible con las restricciones de batería de Android 8+ (Doze, App Standby).

## Decisión

Se adopta **WorkManager 2.10.1** con **`@HiltWorker`** para inyección de dependencias en el Worker.

## Detalles técnicos

### Worker principal: `DescargaAsignacionWorker`

```
@HiltWorker
class DescargaAsignacionWorker @AssistedInject constructor(
    context: Context,
    params: WorkerParameters,
    private val repository: AsignacionRepository,
) : CoroutineWorker(context, params) {
    companion object {
        const val NOMBRE_UNICO_INMEDIATO = "descarga_asignacion_inmediato"
        const val NOMBRE_UNICO_PERIODICO = "descarga_asignacion_periodico"
    }
    override suspend fun doWork(): Result { ... }
}
```

El Worker delega toda la lógica al repositorio y mapea `ResultadoDescarga` → `Result`:

| `ResultadoDescarga` | `WorkManager.Result` |
|---|---|
| `Exito` | `success()` |
| `SinAsignacion` | `success()` |
| `ErrorReintentar` | `retry()` |
| `ErrorPermanente` | `failure()` |
| `VersionNoSoportada` | `failure()` |

### Scheduler: `AsignacionSyncScheduler`

Centraliza la programación de trabajo:
- **Inmediato** (`programarInmediato`): `OneTimeWorkRequest` con `ExistingWorkPolicy.KEEP` — si ya hay una descarga en progreso, no se encola otra.
- **Periódico** (`programarPeriodico`): `PeriodicWorkRequestBuilder` cada 4 horas con `ExistingPeriodicWorkPolicy.UPDATE` — ajustable sin recrear la política.
- **Cancelar** (`cancelarTodo`): invocado desde `LogoutUseCase`.

### Inicialización manual de WorkManager

WorkManager tiene auto-inicialización por defecto vía `WorkManagerInitializer` (ContentProvider). Se deshabilita en `AndroidManifest.xml` con `tools:node="remove"` y se inicializa manualmente en `CobranzaApp` (que implementa `Configuration.Provider`) para poder inyectar `HiltWorkerFactory`.

```xml
<provider
    android:name="androidx.startup.InitializationProvider"
    ...
    tools:node="remove" />
```

```kotlin
@HiltAndroidApp
class CobranzaApp : Application(), Configuration.Provider {
    @Inject lateinit var workerFactory: HiltWorkerFactory
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()
}
```

## Consecuencias

- Los Workers son inyectables con Hilt; no requieren una `WorkerFactory` manual.
- `ExistingWorkPolicy.KEEP` garantiza que el usuario no puede encolar múltiples descargas simultáneas.
- El trabajo periódico se registra en el `onCreate` de la actividad principal después de que la sesión ha sido verificada.
- La eliminación de Worker al hacer logout limpia cualquier trabajo pendiente.

## Alternativas descartadas

- **Foreground Service:** requiere notificación permanente; innecesario para una descarga periódica de pocos segundos.
- **AlarmManager:** no garantiza ejecución bajo restricciones de Doze; más complejo de manejar.
- **JobScheduler directo:** WorkManager lo envuelve y añade reintentos, constraints y compatibilidad hacia atrás.
