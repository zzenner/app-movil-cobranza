# Modelo de datos

Tablas candidatas para los esquemas `cobranza` y `auditoria` de PostgreSQL. Basado en las decisiones funcionales confirmadas. **No es un esquema SQL definitivo:** las tablas se crearán con Flyway al iniciar la Fase 1.

## Convenciones

- Nombres en minúsculas y `snake_case`, sin tildes ni eñes.
- Claves primarias: `UUID` generado por la base de datos (excepto gestiones, cuyo UUID se genera en el dispositivo).
- Fechas con zona horaria: `TIMESTAMPTZ`.
- Columnas de auditoría comunes: `created_at TIMESTAMPTZ`, `updated_at TIMESTAMPTZ`.

---

## Esquema `cobranza`

### `usuarios`
| Columna         | Tipo         | Descripción                                          |
|-----------------|--------------|------------------------------------------------------|
| `id`            | UUID PK      | Clave primaria.                                      |
| `nombre`        | VARCHAR(200) | Nombre completo.                                     |
| `username`      | VARCHAR(100) | Nombre de usuario único.                             |
| `email`         | VARCHAR(200) | Correo electrónico único.                            |
| `password_hash` | VARCHAR(255) | Hash de contraseña.                                  |
| `rol`           | VARCHAR(50)  | `JEFE_SUPERVISORES`, `TECNOLOGIA`, `SUPERVISOR`, `EJECUTIVO_TERRENO`. |
| `activo`        | BOOLEAN      | Si puede iniciar sesión.                             |
| `created_at`    | TIMESTAMPTZ  |                                                      |
| `updated_at`    | TIMESTAMPTZ  |                                                      |

### `supervision`
Historial de relaciones supervisor-ejecutivo.

| Columna          | Tipo        | Descripción                                          |
|------------------|-------------|------------------------------------------------------|
| `id`             | UUID PK     |                                                      |
| `supervisor_id`  | UUID FK     | Referencia a `usuarios` (rol `SUPERVISOR`).          |
| `ejecutivo_id`   | UUID FK     | Referencia a `usuarios` (rol `EJECUTIVO_TERRENO`).   |
| `fecha_inicio`   | DATE        | Inicio de la relación de supervisión.                |
| `fecha_fin`      | DATE        | Fin de la relación (NULL = relación activa).         |
| `created_at`     | TIMESTAMPTZ |                                                      |

### `carteras`
| Columna       | Tipo         | Descripción                  |
|---------------|--------------|------------------------------|
| `id`          | UUID PK      |                              |
| `nombre`      | VARCHAR(200) | Nombre descriptivo.          |
| `descripcion` | TEXT         |                              |
| `activa`      | BOOLEAN      |                              |
| `created_at`  | TIMESTAMPTZ  |                              |
| `updated_at`  | TIMESTAMPTZ  |                              |

### `personas`
| Columna           | Tipo         | Descripción                                          |
|-------------------|--------------|------------------------------------------------------|
| `id`              | UUID PK      |                                                      |
| `rut_numero`      | VARCHAR(10)  | Parte numérica del RUT (sin dígito verificador).     |
| `rut_dv`          | VARCHAR(1)   | Dígito verificador del RUT.                          |
| `nombre`          | VARCHAR(200) | Nombre completo.                                     |
| `created_at`      | TIMESTAMPTZ  |                                                      |
| `updated_at`      | TIMESTAMPTZ  |                                                      |

Índice: `(rut_numero, rut_dv)` UNIQUE.

> `cartera_id` fue eliminado en V008. La relación persona–cartera se gestiona ahora en `carteras_personas`.

### `carteras_personas`
Relación N:M entre personas y carteras. Conserva historial de altas y bajas.

| Columna              | Tipo        | Descripción                                             |
|----------------------|-------------|---------------------------------------------------------|
| `id`                 | UUID PK     | Generado por Java en ejecución normal.                  |
| `cartera_id`         | UUID FK     | Referencia a `carteras`.                                |
| `persona_id`         | UUID FK     | Referencia a `personas`.                                |
| `activa`             | BOOLEAN     | TRUE = vínculo vigente; FALSE = vínculo cerrado.        |
| `fecha_inicio`       | DATE        | Fecha de inicio del vínculo.                            |
| `fecha_fin`          | DATE        | Fecha de cierre (NULL si está activo).                  |
| `fecha_creacion`     | TIMESTAMPTZ | Inmutable.                                              |
| `fecha_actualizacion`| TIMESTAMPTZ | Se actualiza al cerrar el vínculo.                      |
| `version`            | BIGINT      | Optimistic locking.                                     |

Restricciones:
- PK por `id`.
- FK a `carteras` y `personas` (RESTRICT).
- UNIQUE parcial: `(cartera_id, persona_id) WHERE activa = TRUE`.
- CHECK: `(activa=TRUE AND fecha_fin IS NULL) OR (activa=FALSE AND fecha_fin IS NOT NULL)`.
- CHECK: `fecha_fin IS NULL OR fecha_fin >= fecha_inicio`.

### `avales`
Información de solo lectura sobre quienes garantizan operaciones de una persona. Proveniente de la carga CSV o del sistema externo. No recibe gestiones ni asignaciones.

| Columna      | Tipo         | Descripción                                              |
|--------------|--------------|----------------------------------------------------------|
| `id`         | UUID PK      |                                                          |
| `persona_id` | UUID FK      | Persona a la que está asociado el aval.                  |
| `rut_numero` | VARCHAR(10)  | Parte numérica del RUT del aval.                         |
| `rut_dv`     | VARCHAR(1)   | Dígito verificador del RUT del aval.                     |
| `nombre`     | VARCHAR(200) | Nombre completo del aval.                                |
| `created_at` | TIMESTAMPTZ  |                                                          |

**PENDIENTE (no bloqueante):** confirmar si el aval se asocia a la persona o a una operación específica en el sistema externo definitivo. Para el MVP, la relación es persona → aval.

### `operaciones`
| Columna              | Tipo          | Descripción                                     |
|----------------------|---------------|-------------------------------------------------|
| `id`                 | UUID PK       |                                                 |
| `persona_id`         | UUID FK       | Titular de la operación.                        |
| `numero_operacion`   | VARCHAR(50)   | Identificador externo de la operación.          |
| `capital`            | NUMERIC(15,2) | Monto de capital original.                      |
| `interes_penal`      | NUMERIC(15,2) | Interés penal acumulado vigente.                |
| `gastos_cobranza`    | NUMERIC(15,2) | Gastos de cobranza vigentes.                    |
| `total_vigente`      | NUMERIC(15,2) | Total a pagar vigente (puede calcularse).       |
| `created_at`         | TIMESTAMPTZ   |                                                 |
| `updated_at`         | TIMESTAMPTZ   | Se actualiza con cada sincronización.           |

### `cuotas`
| Columna            | Tipo          | Descripción                                      |
|--------------------|---------------|--------------------------------------------------|
| `id`               | UUID PK       |                                                  |
| `operacion_id`     | UUID FK       |                                                  |
| `numero_cuota`     | INTEGER       | Número de la cuota dentro de la operación.       |
| `monto`            | NUMERIC(15,2) | Monto de la cuota.                               |
| `fecha_vencimiento`| DATE          |                                                  |
| `estado`           | VARCHAR(20)   | `VENCIDA`, `VIGENTE`, `FUTURA`, `PAGADA`.        |
| `interes_penal`    | NUMERIC(15,2) | Interés penal específico de la cuota.            |
| `created_at`       | TIMESTAMPTZ   |                                                  |
| `updated_at`       | TIMESTAMPTZ   | Se actualiza con cada sincronización.            |

**Alcance de descarga al teléfono:** se descargan todas las cuotas vencidas vigentes y todas las futuras vigentes de las operaciones activas. No se descargan cuotas de operaciones anuladas, cerradas sin saldo ni completamente pagadas.

### `asignaciones_mensuales`
| Columna          | Tipo        | Descripción                                            |
|------------------|-------------|--------------------------------------------------------|
| `id`             | UUID PK     |                                                        |
| `ejecutivo_id`   | UUID FK     | Ejecutivo de terreno asignado.                         |
| `fecha_inicio`   | DATE        | Inicio del período mensual.                            |
| `fecha_fin`      | DATE        | Fin del período mensual (NULL = vigente).              |
| `fuente`         | VARCHAR(50) | `CSV`, `API_EXTERNA`.                                  |
| `created_at`     | TIMESTAMPTZ |                                                        |

### `asignaciones_mensuales_personas`
Personas incluidas en una asignación mensual.

| Columna                  | Tipo    | Descripción                 |
|--------------------------|---------|-----------------------------|
| `asignacion_mensual_id`  | UUID FK |                             |
| `persona_id`             | UUID FK |                             |
| `created_at`             | TIMESTAMPTZ |                         |

PK compuesta: `(asignacion_mensual_id, persona_id)`.

### `asignaciones_diarias`
| Columna                  | Tipo        | Descripción                                                       |
|--------------------------|-------------|-------------------------------------------------------------------|
| `id`                     | UUID PK     |                                                                   |
| `asignacion_mensual_id`  | UUID FK     | Asignación mensual de la que proviene.                            |
| `ejecutivo_id`           | UUID FK     | Ejecutivo destinatario de la asignación.                          |
| `supervisor_id`          | UUID FK     | Supervisor que creó y publicó la asignación.                      |
| `fecha`                  | DATE        | Día de la asignación.                                             |
| `estado`                 | VARCHAR(20) | `BORRADOR`, `PUBLICADA`, `FINALIZADA`, `CANCELADA`. Ver CICLOS_DE_VIDA.md. |
| `fecha_publicacion`      | TIMESTAMPTZ | Momento en que el supervisor la publicó (NULL si está en BORRADOR). |
| `created_at`             | TIMESTAMPTZ |                                                                   |
| `updated_at`             | TIMESTAMPTZ |                                                                   |

### `descargas_asignacion_diaria`
Registro técnico de cada descarga de una asignación diaria por un dispositivo. La descarga no es un estado funcional de la asignación; es un evento de sincronización.

| Columna                   | Tipo        | Descripción                                                          |
|---------------------------|-------------|----------------------------------------------------------------------|
| `id`                      | UUID PK     |                                                                      |
| `asignacion_diaria_id`    | UUID FK     | Asignación que se descargó.                                          |
| `dispositivo_id`          | UUID FK     | Dispositivo que realizó la descarga.                                 |
| `fecha_primera_descarga`  | TIMESTAMPTZ | Cuándo se descargó por primera vez desde este dispositivo.           |
| `fecha_ultima_descarga`   | TIMESTAMPTZ | Cuándo se descargó por última vez (puede descargarse más de una vez).|
| `version_descargada`      | VARCHAR(20) | Versión de la app al momento de la última descarga.                  |

### `asignaciones_diarias_personas`
Personas incluidas en una asignación diaria.

| Columna                  | Tipo    | Descripción |
|--------------------------|---------|-------------|
| `asignacion_diaria_id`   | UUID FK |             |
| `persona_id`             | UUID FK |             |
| `created_at`             | TIMESTAMPTZ | |

PK compuesta: `(asignacion_diaria_id, persona_id)`.

### `gestiones`

Implementada en V010. Tabla append-only (inmutable); sin `updated_at` ni `version`. Ver ADR-0026, ADR-0027, ADR-0028, ADR-0029.

| Columna                  | Tipo              | Nulo | Descripción                                                                            |
|--------------------------|-------------------|------|----------------------------------------------------------------------------------------|
| `id`                     | UUID PK           | No   | **Generado en el dispositivo Android** (ADR-0027). Sin `@GeneratedValue`.              |
| `origen_gestion`         | VARCHAR(30)       | No   | `ASIGNACION_DIARIA` o `BUSQUEDA_DIRECTA` (ADR-0026).                                   |
| `asignacion_diaria_id`   | UUID FK           | Sí   | Referencia a `asignaciones_diarias`. NULL si `origen_gestion = BUSQUEDA_DIRECTA`.      |
| `persona_id`             | UUID FK           | No   | Persona sobre la que se realizó la gestión.                                            |
| `ejecutivo_id`           | UUID FK           | No   | Ejecutivo que registró la gestión (debe tener rol `EJECUTIVO_TERRENO`).                |
| `tipo_gestion`           | VARCHAR(30)       | No   | `CONTACTO_FAMILIAR`, `COMPROMISO_PAGO`, `SIN_CONTACTO`. CHECK constraint (no FK).     |
| `fecha_gestion`          | TIMESTAMPTZ       | No   | Fecha y hora registrada en el **dispositivo** (ADR-0029).                              |
| `observacion`            | TEXT              | Sí   | Texto libre del ejecutivo.                                                             |
| `observacion_direccion`  | TEXT              | Sí   | Dirección reportada como incorrecta o incompleta desde terreno.                        |
| `latitud`                | DOUBLE PRECISION  | No   | Coordenada geográfica capturada al momento del registro.                               |
| `longitud`               | DOUBLE PRECISION  | No   | Coordenada geográfica capturada al momento del registro.                               |
| `precision_metros`       | REAL              | No   | Precisión GPS en metros (≥ 0).                                                         |
| `proveedor_gps`          | VARCHAR(50)       | Sí   | Proveedor de ubicación (GPS, NETWORK, FUSED, etc.).                                    |
| `ubicacion_simulada`     | BOOLEAN           | No   | Si Android detectó mock location. Sin DEFAULT: el dispositivo lo informa explícitamente. |
| `fecha_captura_gps`      | TIMESTAMPTZ       | No   | Momento en que se capturó la ubicación GPS.                                            |
| `fecha_compromiso`       | DATE              | Sí   | Obligatorio si `tipo_gestion = COMPROMISO_PAGO`. NULL en otros tipos.                  |
| `fecha_creacion_servidor`| TIMESTAMPTZ       | No   | Momento de recepción en la API (generado en servidor, ADR-0029).                       |

**Restricciones CHECK:**
- Coherencia `origen_gestion` ↔ `asignacion_diaria_id`: si `ASIGNACION_DIARIA` entonces FK no nulo; si `BUSQUEDA_DIRECTA` entonces FK nulo.
- Coherencia `tipo_gestion` ↔ `fecha_compromiso`: si `COMPROMISO_PAGO` entonces fecha no nula; en otros tipos debe ser nula.
- Rangos: `latitud BETWEEN -90 AND 90`, `longitud BETWEEN -180 AND 180`, `precision_metros >= 0`.

**Nota:** No se almacena columna `ubicacion GEOMETRY`. La latitud y longitud se almacenan como `DOUBLE PRECISION`; PostGIS no se usa en gestiones en el MVP.

### `fotografias_gestiones`
| Columna        | Tipo         | Descripción                                           |
|----------------|--------------|-------------------------------------------------------|
| `id`           | UUID PK      |                                                       |
| `gestion_id`   | UUID FK      |                                                       |
| `referencia`   | TEXT         | Ruta o clave en almacenamiento externo (futuro: S3).  |
| `fecha_captura`| TIMESTAMPTZ  |                                                       |
| `created_at`   | TIMESTAMPTZ  |                                                       |

### `direcciones`
Direcciones importadas del sistema externo. No se sobrescriben.

| Columna      | Tipo         | Descripción                                        |
|--------------|--------------|----------------------------------------------------|
| `id`         | UUID PK      |                                                    |
| `persona_id` | UUID FK      |                                                    |
| `tipo`       | VARCHAR(50)  | Tipo de dirección (ej: `DOMICILIO`, `TRABAJO`).    |
| `texto`      | TEXT         | Dirección completa en texto.                       |
| `vigente`    | BOOLEAN      |                                                    |
| `created_at` | TIMESTAMPTZ  |                                                    |

### `observaciones_direccion`
Observaciones registradas desde terreno cuando el ejecutivo detecta que la dirección de una persona es incorrecta o incompleta. **No es una corrección activa:** no modifica la dirección original ni activa ningún proceso. Es información para revisión futura.

| Columna              | Tipo         | Descripción                                                         |
|----------------------|--------------|---------------------------------------------------------------------|
| `id`                 | UUID PK      |                                                                     |
| `persona_id`         | UUID FK      | Persona a la que refiere la observación.                            |
| `direccion_id`       | UUID FK      | Dirección original a la que refiere (opcional).                     |
| `observacion`        | TEXT         | Texto libre describiendo el problema o la corrección sugerida.      |
| `direccion_reportada`| TEXT         | Nueva dirección sugerida desde terreno (opcional).                  |
| `usuario_id`         | UUID FK      | Ejecutivo que registró la observación.                              |
| `dispositivo_id`     | UUID FK      | Dispositivo desde el que se registró.                               |
| `fecha_dispositivo`  | TIMESTAMPTZ  | Momento del registro en el dispositivo.                             |
| `fecha_servidor`     | TIMESTAMPTZ  | Momento de recepción en la API.                                     |
| `created_at`         | TIMESTAMPTZ  |                                                                     |

Inmutable una vez recibida por la API. La dirección original en `direcciones` no se modifica.

### `dispositivos`
Estado de dispositivos móviles corporativos.

| Columna                    | Tipo         | Descripción                                       |
|----------------------------|--------------|---------------------------------------------------|
| `id`                       | UUID PK      |                                                   |
| `usuario_id`               | UUID FK      |                                                   |
| `identificador_dispositivo`| VARCHAR(200) | ID único del dispositivo (Android ID u otro).     |
| `ultima_sincronizacion`    | TIMESTAMPTZ  |                                                   |
| `version_app`              | VARCHAR(20)  |                                                   |
| `operaciones_pendientes`   | INTEGER      | Reportado por el dispositivo.                     |
| `ultimo_error`             | TEXT         |                                                   |
| `activo`                   | BOOLEAN      | Si el dispositivo tiene acceso. False = revocado. |
| `created_at`               | TIMESTAMPTZ  |                                                   |
| `updated_at`               | TIMESTAMPTZ  |                                                   |

### `importaciones`
Registro de cargas de asignaciones.

| Columna             | Tipo         | Descripción                                         |
|---------------------|--------------|-----------------------------------------------------|
| `id`                | UUID PK      |                                                     |
| `usuario_id`        | UUID FK      | Usuario que realizó la carga.                       |
| `tipo`              | VARCHAR(50)  | `CSV`, `XLSX`, `API_EXTERNA`.                       |
| `nombre_archivo`    | VARCHAR(255) |                                                     |
| `filas_totales`     | INTEGER      |                                                     |
| `filas_aceptadas`   | INTEGER      |                                                     |
| `filas_rechazadas`  | INTEGER      |                                                     |
| `filas_advertencia` | INTEGER      |                                                     |
| `estado`            | VARCHAR(30)  | `PROCESANDO`, `COMPLETADA`, `ERROR`.                |
| `created_at`        | TIMESTAMPTZ  |                                                     |

---

## Esquema `auditoria`

Pendiente de diseño detallado. El esquema existe y contendrá registros de operaciones significativas del sistema.

---

## PENDIENTE

- Definir estrategia de clave primaria para todas las tablas (UUID vs BIGSERIAL por tipo de tabla).
- Confirmar si `ubicacion` (PostGIS GEOMETRY) se calcula desde latitud/longitud o si se almacena redundantemente.
- Definir índices de búsqueda: `rut_numero` en personas, `persona_id` en gestiones, `ejecutivo_id` en asignaciones diarias.
- Diseñar tabla de detalle de importaciones (errores por fila).
- Definir qué columnas de gestiones se transmiten en la sincronización incremental.
- Diseñar esquema `auditoria` con las operaciones que se deben registrar.
- (Resuelto) Alcance de descarga: todas las operaciones activas + todas las cuotas vencidas y futuras vigentes. Ver nota en tabla `cuotas`.
