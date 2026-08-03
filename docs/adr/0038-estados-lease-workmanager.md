# ADR-0038 — Estados de sincronización, lease atómico y WorkManager (Fase 4C-A)

**Estado:** Aceptado — 2026-08-02
**Contexto:** Fase 4C-A — Gestiones offline desde ASIGNACION_DIARIA

---

## Contexto

El envío de gestiones al servidor puede ser interrumpido por fallos de red, reinicios de la app o el sistema operativo matando el proceso. Necesitamos garantizar que:

1. Ninguna gestión se pierda ni se envíe dos veces.
2. Dos workers concurrentes no envíen la misma gestión simultáneamente.
3. Un worker que muere a mitad de un envío no deja gestiones bloqueadas en `ENVIANDO` para siempre.
4. Los reintentos no saturen la API (backoff exponencial).

## Decisión

### Estados de sincronización

`gestion_local.estadoSincronizacion` puede tomar 6 valores:

| Estado | Descripción |
|---|---|
| `PENDIENTE_ENVIO` | Gestión guardada localmente, aún no enviada |
| `ENVIANDO` | Lease activo; un worker la está procesando ahora |
| `SINCRONIZADA` | Confirmada por la API (201 o 200 idempotente) |
| `ERROR_REINTENTABLE` | Error transitorio (5xx, IOException); se reintentará |
| `ERROR_PERMANENTE` | Error definitivo (4xx excl. 401/409); sin reintento automático |
| `CONFLICTO` | La API respondió 409; requiere revisión manual |

Mapeo de códigos HTTP:
- `201`, `200` → `SINCRONIZADA`
- `401` → liberar lease → `PENDIENTE_ENVIO`; abortar outbox (sesión expirada)
- `409` → `CONFLICTO`
- `422` → `ERROR_PERMANENTE` (código `VALIDACION`)
- `400`, `403`, `404` y cualquier otro 4xx → `ERROR_PERMANENTE` (código `HTTP_<N>`)
- `5xx` → `ERROR_REINTENTABLE` (código `HTTP_<N>`)
- `IOException` → `ERROR_REINTENTABLE` (código `IO_ERROR`)

### Adquisición atómica de lease (CAS)

Para evitar que dos workers envíen la misma gestión, la adquisición del derecho de envío es un `UPDATE` atómico que solo tiene efecto si el estado actual es elegible:

```sql
UPDATE gestion_local
SET estadoSincronizacion = 'ENVIANDO', leaseHastaEpoch = :leaseHasta
WHERE id = :id
  AND estadoSincronizacion IN ('PENDIENTE_ENVIO', 'ERROR_REINTENTABLE')
  AND (fechaProximoIntentoEpoch IS NULL OR fechaProximoIntentoEpoch <= :ahora)
```

`GestionLocalDao.adquirirLease()` retorna `Int` (filas afectadas): 1 = adquirida, 0 = carrera perdida. Si retorna 0, el worker salta esa gestión sin enviarla.

### Recuperación de leases expirados

`leaseHastaEpoch` es la marca de tiempo hasta la que el lease está vigente (5 minutos). Al inicio de `procesarOutbox()`, antes de consultar elegibles:

```sql
UPDATE gestion_local SET estadoSincronizacion = 'PENDIENTE_ENVIO', leaseHastaEpoch = NULL
WHERE estadoSincronizacion = 'ENVIANDO'
  AND leaseHastaEpoch IS NOT NULL AND leaseHastaEpoch < :ahora
```

Solo se recuperan leases expirados. **Nunca** se hace un reset masivo de todos los `ENVIANDO` al inicio del worker.

### Backoff exponencial por registro

`fechaProximoIntentoEpoch = System.currentTimeMillis() + min(30_000 * 2^intentos, 24h)`

El backoff es por registro, no global. Un registro en `ERROR_REINTENTABLE` con `intentos=3` espera `min(240s, 24h) = 240s` antes de reaparecer como elegible. No existe límite máximo de intentos: la gestión reintenta indefinidamente hasta que la red y el servidor respondan correctamente o el ejecutivo cierra sesión.

### WorkManager — ExistingWorkPolicy.KEEP

`EnvioGestionWorker` usa `ExistingWorkPolicy.KEEP`. Si ya hay un worker en cola o ejecutándose, no se programa otro. Esto evita la acumulación de workers redundantes después de guardar varias gestiones en ráfaga.

El worker usa `NetworkType.CONNECTED` y `BackoffPolicy.EXPONENTIAL` a nivel de WorkManager (fallo de proceso/kill), que se combina con el backoff por registro a nivel de DAO (fallo de API).

### Protección de single-flight con Mutex

`GestionRepository.procesarOutbox()` está protegido por un `kotlinx.coroutines.sync.Mutex`. Dentro de un mismo proceso, solo un coroutine puede ejecutar el outbox a la vez. Combinado con el CAS del DAO, esto garantiza idempotencia incluso con múltiples threads.

## Consecuencias

- No se pierden gestiones por caída del worker o del proceso.
- No se envían gestiones duplicadas incluso con workers concurrentes.
- Los gestiones en `ENVIANDO` con lease expirado se recuperan automáticamente en el siguiente ciclo.
- El backoff exponencial protege la API de tormentas de reintentos.
- `CONFLICTO` y `ERROR_PERMANENTE` requieren intervención humana; la app los muestra distinguidos en el historial.

## Alternativas descartadas

- **Reset masivo de ENVIANDO al iniciar worker:** un worker puede morir durante el envío pero antes de actualizar el estado. Un reset masivo al inicio devolvería también las gestiones que otro worker activo está procesando ahora mismo (en un escenario multi-proceso). El lease por tiempo es más seguro.
- **Convertir ERROR_REINTENTABLE a ERROR_PERMANENTE tras N intentos:** descartado explícitamente. La app puede estar sin red durante días; no hay un número de intentos que justifique declarar error permanente sin respuesta definitiva del servidor.
