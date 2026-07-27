# Protocolo de sincronización

## Principios

- **Incremental:** la app envía solo gestiones con estado `PENDIENTE_ENVIO` desde la última sincronización exitosa.
- **Idempotente:** reenviar la misma gestión múltiples veces no produce duplicados (clave: UUID del dispositivo).
- **Resiliente:** los fallos de red no pierden datos; WorkManager reintenta con backoff exponencial.
- **Gestiones: unidireccional** — van del dispositivo a la API, nunca al revés.
- **Asignación y datos: bidireccional** — se descargan desde la API hacia el dispositivo.

## Endpoints previstos (borrador)

Los endpoints definitivos se definirán en `contracts/openapi/` en la Fase 1.

### Descarga de asignación diaria
```
GET /api/v1/sync/asignacion-diaria
Authorization: Bearer <token>

Response: asignación en estado PUBLICADA para el ejecutivo autenticado, incluyendo:
  - Lista de personas (~50)
  - Operaciones de cada persona
  - Cuotas (~3 por operación)
  - Últimas 10 gestiones por RUT (de cualquier ejecutivo)
  - Direcciones
  - Valores financieros vigentes
```

### Envío de gestiones (carga)
```
POST /api/v1/sync/gestiones
Authorization: Bearer <token>
Content-Type: application/json

Body: lista de gestiones con estado PENDIENTE_ENVIO.
      Cada gestión incluye su UUID generado en el dispositivo (idempotencia).
      Las fotografías se referencian por UUID (subida por separado o en el mismo payload: PENDIENTE).

Response: resultado por gestión:
  - sincronizada: confirmada y persistida.
  - error_reintentable: fallo temporal, reintentar.
  - error_permanente: fallo definitivo, no reintentar.
```

### Subida de fotografías
```
POST /api/v1/sync/fotografias/{gestion_id}
Authorization: Bearer <token>
Content-Type: multipart/form-data

Body: archivo de imagen.
Nota: diseño exacto PENDIENTE según solución de almacenamiento (S3 compatible).
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

## Manejo de errores HTTP

| Código HTTP  | Acción                                                                        |
|--------------|-------------------------------------------------------------------------------|
| 200 / 201    | Éxito. Actualizar estado en Room a `SINCRONIZADA`.                            |
| 401 / 403    | Token inválido o sin acceso. Solicitar reautenticación.                       |
| 409          | Conflicto (UUID duplicado con contenido diferente). Marcar `ERROR_PERMANENTE`.|
| 422          | Error de validación permanente. Marcar `ERROR_PERMANENTE`.                   |
| 5xx / red    | Error temporal. Marcar `ERROR_REINTENTABLE`. Reintento con backoff.           |

## PENDIENTE

- Definir el contrato OpenAPI completo de los endpoints de sincronización (Fase 1).
- Definir cómo se transmiten las fotografías: base64 en el payload o multipart separado.
- Definir qué delta se incluye en la descarga incremental de la asignación (¿timestamp o secuencia?).
- Definir la política de backoff: intervalo inicial, factor multiplicador y máximo de reintentos.
- Definir si el token se renueva automáticamente durante la sincronización (refresh token).
- Definir comportamiento cuando la asignación diaria del dispositivo ya no coincide con la del servidor (nueva asignación publicada mientras el ejecutivo trabaja).
