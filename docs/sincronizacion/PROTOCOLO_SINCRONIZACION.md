# Protocolo de sincronización

## Principios

- **Incremental:** la app envía solo gestiones con estado `PENDIENTE_ENVIO` desde la última sincronización exitosa.
- **Idempotente:** reenviar la misma gestión múltiples veces no produce duplicados (clave: UUID del dispositivo).
- **Resiliente:** los fallos de red no pierden datos; WorkManager reintenta con backoff exponencial.
- **Gestiones: unidireccional** — van del dispositivo a la API, nunca al revés.
- **Asignación y datos: bidireccional** — se descargan desde la API hacia el dispositivo.

## Endpoints implementados (Fase 3D + 4C-A + 4C-B)

Contrato completo en `contracts/openapi/cobranza-api.yaml`.

### Descarga de asignación diaria
```
GET /api/v1/asignaciones/diaria/activa
Authorization: Bearer <token>

Response 200: bundle completo para el ejecutivo autenticado:
  - asignacion_diaria (id, fecha, estado)
  - personas (~50): datos personales, direcciones vigentes, avales
  - operaciones: todas las activas por persona
  - cuotas: todas las VENCIDA/VIGENTE/FUTURA (excluye PAGADA)
  - gestiones_historicas: últimas 10 por RUT (de cualquier ejecutivo)

Response 204: no hay asignación activa para el ejecutivo hoy.
```

### Envío de gestiones (carga — individual, idempotente)
```
POST /api/v1/gestiones
Authorization: Bearer <token>
Content-Type: application/json

Body: una gestión por request (Android envía de a una, no en lote).
  - id: UUID generado en el dispositivo (garantiza idempotencia)
  - personaId, origenGestion, asignacionDiariaId, tipoGestion, fechaGestion
  - latitud, longitud, precisionMetros, ubicacionSimulada, proveedorGps, fechaCapturaGps
  - observacion, observacionDireccion, fechaCompromiso (opcionales)

Response 201 INSERTADA: gestión nueva persistida.
Response 200 IDEMPOTENTE: mismo UUID + mismo contenido (reenvío seguro).
Response 409 CONFLICTO: mismo UUID + contenido diferente (no recuperable).
```

### Búsqueda directa por RUT (Fase 4C-B)
```
POST /api/v1/personas/busquedas
Authorization: Bearer <token>
Content-Type: application/json
Cache-Control: no-store  (en la respuesta)

Body: { "rutNumero": "15000001", "rutDv": "7" }

Response 200: { "version": 1, "generadoEn": "...", "persona": { /* DatosPersonaDescarga */ } }
Response 400: RUT inválido (code: RUT_INVALIDO)
Response 404: Persona no encontrada
```

**RUT en body:** el RUT es PII; colocarlo en query string lo expone en logs de acceso y proxies. Ver ADR-0041.
**Snapshot local:** la respuesta se persiste en Room (`persona_directa`) para acceso offline posterior. Ver ADR-0042.

### Subida de fotografías
```
POST /api/v1/gestiones/{id}/fotografias
Authorization: Bearer <token>
Content-Type: multipart/form-data

Diseño exacto PENDIENTE (fuera de alcance Fase 4C-A, diferido — ADR-0030).
```

## Flujo de sincronización completo

```
WorkManager detecta conectividad (o ejecutivo inicia sincronización manual)
    │
    ├─► 1. Validar o renovar autenticación
    │       ├─► Renovar access token con refresh token
    │       ├─► Verificar usuario activo en servidor
    │       ├─► Verificar dispositivo no revocado
    │       └─► Si falla validación: terminar sesión, notificar ejecutivo, conservar pendientes
    │
    ├─► 2. Enviar gestiones pendientes
    │       └─► Leer gestiones PENDIENTE_ENVIO desde Room
    │               └─► POST /api/v1/sync/gestiones
    │                     Por cada resultado:
    │                       - sincronizada       → actualizar Room a SINCRONIZADA
    │                       - error_reintentable → mantener PENDIENTE_ENVIO, programar reintento
    │                       - error_permanente   → actualizar Room a ERROR_PERMANENTE, notificar
    │
    ├─► 3. Subir fotografías pendientes de gestiones ya SINCRONIZADA
    │
    └─► 4. Descargar asignación diaria
            └─► GET /api/v1/sync/asignacion-diaria  (solo si estado = PUBLICADA)
                    ├─► Reemplazar valores financieros en Room
                    ├─► No reemplazar gestiones PENDIENTE_ENVIO ni fotografías pendientes
                    ├─► Conservar datos de personas con pendientes (ver retención)
                    └─► Registrar timestamp de última sincronización exitosa
```

## Indicadores de estado que muestra la app

| Indicador                   | Descripción                                                  |
|-----------------------------|--------------------------------------------------------------|
| Modo offline                | Visible cuando no hay conexión activa.                       |
| Última sincronización       | Fecha y hora de la última sincronización completa exitosa.   |
| Gestiones pendientes        | Contador de gestiones con estado `PENDIENTE_ENVIO`.          |
| Error de sincronización     | Alerta cuando existe alguna gestión en `ERROR_PERMANENTE`.   |

## Manejo de errores HTTP (POST /api/v1/gestiones)

| Código HTTP | Estado Room resultante | Acción                                                       |
|-------------|------------------------|--------------------------------------------------------------|
| 201 / 200   | `SINCRONIZADA`         | Gestión confirmada. Estado terminal positivo.                |
| 401         | `PENDIENTE_ENVIO`      | Lease liberado. Outbox abortado. El worker retorna `SesionExpirada`. |
| 400, 403, 404 | `ERROR_PERMANENTE`   | Error definitivo del cliente. Sin reintento automático.      |
| 409         | `CONFLICTO`            | UUID con contenido diferente. Requiere intervención manual.  |
| 422         | `ERROR_PERMANENTE`     | Error de validación. Código interno: `VALIDACION`.           |
| 5xx         | `ERROR_REINTENTABLE`   | Error transitorio de servidor. Backoff exponencial.          |
| IOException | `ERROR_REINTENTABLE`   | Error de red. Backoff exponencial.                           |

Backoff: `min(30_000ms × 2ⁿ, 24h)` por registro. Sin límite de intentos. Ver ADR-0038.

## PENDIENTE

- Definir cómo se transmiten las fotografías: base64 en el payload o multipart separado (diferido — ADR-0030).
- Definir qué delta se incluye en la descarga incremental de la asignación.
- Definir comportamiento cuando la asignación diaria del dispositivo ya no coincide con la del servidor.
