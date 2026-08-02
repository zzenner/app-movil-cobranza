# Diccionario de datos preliminar

Descripción de los atributos de las entidades principales del sistema. Este diccionario es preliminar: sirve de referencia para el diseño del esquema Flyway en la Fase 1B. Las restricciones, valores por defecto y tipos exactos se confirmarán al escribir las migraciones.

**Convenciones:** `snake_case`, sin tildes ni eñes, español para términos de dominio.

---

## `usuarios`

| Columna | Tipo | Nulo | Descripción |
|---|---|---|---|
| `id` | UUID | No | PK. Generado por la BD. |
| `nombre` | VARCHAR(200) | No | Nombre completo. |
| `username` | VARCHAR(100) | No | Nombre de usuario. Único en el sistema. |
| `email` | VARCHAR(200) | No | Correo institucional. Único en el sistema. |
| `password_hash` | VARCHAR(255) | No | Hash bcrypt de la contraseña. |
| `rol` | VARCHAR(50) | No | `JEFE_SUPERVISORES`, `TECNOLOGIA`, `SUPERVISOR`, `EJECUTIVO_TERRENO`. |
| `activo` | BOOLEAN | No | Si puede iniciar sesión. Default: `true`. |
| `created_at` | TIMESTAMPTZ | No | Fecha de creación. |
| `updated_at` | TIMESTAMPTZ | No | Fecha de última modificación. |

---

## `supervision_usuarios`

| Columna | Tipo | Nulo | Descripción |
|---|---|---|---|
| `id` | UUID | No | PK. |
| `supervisor_id` | UUID | No | FK → `usuarios`. Debe tener rol `SUPERVISOR`. |
| `ejecutivo_id` | UUID | No | FK → `usuarios`. Debe tener rol `EJECUTIVO_TERRENO`. |
| `fecha_inicio` | DATE | No | Inicio de la relación de supervisión. |
| `fecha_fin` | DATE | Sí | Fin de la relación. NULL = activa. |
| `created_at` | TIMESTAMPTZ | No | Fecha de creación. |

---

## `dispositivos`

| Columna | Tipo | Nulo | Descripción |
|---|---|---|---|
| `id` | UUID | No | PK. |
| `usuario_id` | UUID | No | FK → `usuarios`. Ejecutivo al que pertenece. |
| `identificador_dispositivo` | VARCHAR(200) | No | Android ID u otro identificador del dispositivo. |
| `ultima_sincronizacion` | TIMESTAMPTZ | Sí | Última sincronización exitosa. |
| `version_app` | VARCHAR(20) | Sí | Versión de la app al momento de la última sync. |
| `operaciones_pendientes` | INTEGER | No | Reportado por el dispositivo. Default: 0. |
| `ultimo_error` | TEXT | Sí | Último error de sincronización reportado. |
| `activo` | BOOLEAN | No | Si el dispositivo tiene acceso. `false` = revocado. Default: `true`. |
| `created_at` | TIMESTAMPTZ | No | Fecha de registro. |
| `updated_at` | TIMESTAMPTZ | No | Fecha de última actualización. |

---

## `carteras`

| Columna | Tipo | Nulo | Descripción |
|---|---|---|---|
| `id` | UUID | No | PK. |
| `nombre` | VARCHAR(200) | No | Nombre descriptivo de la cartera. |
| `descripcion` | TEXT | Sí | Descripción libre. |
| `activa` | BOOLEAN | No | Estado de la cartera. Default: `true`. |
| `created_at` | TIMESTAMPTZ | No | |
| `updated_at` | TIMESTAMPTZ | No | |

---

## `personas`

| Columna | Tipo | Nulo | Descripción |
|---|---|---|---|
| `id` | UUID | No | PK. |
| `rut_numero` | VARCHAR(10) | No | Parte numérica del RUT, sin dígito verificador ni puntos. |
| `rut_dv` | VARCHAR(1) | No | Dígito verificador del RUT (0-9 o K). |
| `nombre` | VARCHAR(200) | No | Nombre completo. |
| `created_at` | TIMESTAMPTZ | No | |
| `updated_at` | TIMESTAMPTZ | No | |

Restricción: `UNIQUE (rut_numero, rut_dv)`.

> `cartera_id` fue eliminado en V008 (RN-03 revisada). La relación persona–cartera se gestiona en `carteras_personas`.

---

## `carteras_personas`

Relación N:M entre personas y carteras. Conserva historial de altas y bajas.

| Columna | Tipo | Nulo | Descripción |
|---|---|---|---|
| `id` | UUID | No | PK. Generado por Java en ejecución normal. |
| `cartera_id` | UUID | No | FK → `carteras`. |
| `persona_id` | UUID | No | FK → `personas`. |
| `activa` | BOOLEAN | No | TRUE = vínculo vigente. Default: `true`. |
| `fecha_inicio` | DATE | No | Fecha de inicio del vínculo. |
| `fecha_fin` | DATE | Sí | Fecha de cierre. NULL = activo. |
| `fecha_creacion` | TIMESTAMPTZ | No | Inmutable. |
| `fecha_actualizacion` | TIMESTAMPTZ | No | Se actualiza al cerrar. |
| `version` | BIGINT | No | Optimistic locking. Default: 0. |

Restricciones: `UNIQUE (cartera_id, persona_id) WHERE activa = TRUE`.

---

## `avales`

| Columna | Tipo | Nulo | Descripción |
|---|---|---|---|
| `id` | UUID | No | PK. |
| `persona_id` | UUID | No | FK → `personas`. Persona a la que está asociado. |
| `rut_numero` | VARCHAR(10) | No | Parte numérica del RUT del aval. |
| `rut_dv` | VARCHAR(1) | No | Dígito verificador del RUT del aval. |
| `nombre` | VARCHAR(200) | No | Nombre completo del aval. |
| `created_at` | TIMESTAMPTZ | No | |

Solo lectura en el MVP. No tiene `updated_at`.

---

## `direcciones`

| Columna | Tipo | Nulo | Descripción |
|---|---|---|---|
| `id` | UUID | No | PK. |
| `persona_id` | UUID | No | FK → `personas`. |
| `tipo` | VARCHAR(50) | No | Tipo (ej: `DOMICILIO`, `TRABAJO`). |
| `texto` | TEXT | No | Dirección completa en texto libre. |
| `vigente` | BOOLEAN | No | Si es la dirección activa. Default: `true`. |
| `created_at` | TIMESTAMPTZ | No | |

No se modifica después de la importación.

---

## `observaciones_direccion`

| Columna | Tipo | Nulo | Descripción |
|---|---|---|---|
| `id` | UUID | No | PK. |
| `persona_id` | UUID | No | FK → `personas`. |
| `direccion_id` | UUID | Sí | FK → `direcciones`. Dirección referenciada (opcional). |
| `observacion` | TEXT | No | Texto libre del ejecutivo. |
| `direccion_reportada` | TEXT | Sí | Dirección sugerida desde terreno (opcional). |
| `usuario_id` | UUID | No | FK → `usuarios`. Ejecutivo que registró. |
| `dispositivo_id` | UUID | No | FK → `dispositivos`. |
| `fecha_dispositivo` | TIMESTAMPTZ | No | Momento del registro en el dispositivo. |
| `fecha_servidor` | TIMESTAMPTZ | No | Momento de recepción en la API. |
| `created_at` | TIMESTAMPTZ | No | |

Inmutable tras recepción. No tiene `updated_at`.

---

## `operaciones`

| Columna | Tipo | Nulo | Descripción |
|---|---|---|---|
| `id` | UUID | No | PK. |
| `persona_id` | UUID | No | FK → `personas`. |
| `numero_operacion` | VARCHAR(50) | No | Identificador en el sistema externo. |
| `capital` | NUMERIC(15,2) | No | Capital original del crédito. |
| `interes_penal` | NUMERIC(15,2) | No | Interés penal acumulado vigente. |
| `gastos_cobranza` | NUMERIC(15,2) | No | Gastos de cobranza vigentes. |
| `total_vigente` | NUMERIC(15,2) | No | Total a pagar vigente. |
| `created_at` | TIMESTAMPTZ | No | |
| `updated_at` | TIMESTAMPTZ | No | Se actualiza en cada sincronización. |

---

## `cuotas`

| Columna | Tipo | Nulo | Descripción |
|---|---|---|---|
| `id` | UUID | No | PK. |
| `operacion_id` | UUID | No | FK → `operaciones`. |
| `numero_cuota` | INTEGER | No | Número de la cuota dentro de la operación. |
| `monto` | NUMERIC(15,2) | No | Monto de la cuota. |
| `fecha_vencimiento` | DATE | No | Fecha de vencimiento de la cuota. |
| `estado` | VARCHAR(20) | No | `VENCIDA`, `VIGENTE`, `FUTURA`, `PAGADA`. |
| `interes_penal` | NUMERIC(15,2) | No | Interés penal específico de la cuota. |
| `created_at` | TIMESTAMPTZ | No | |
| `updated_at` | TIMESTAMPTZ | No | Se actualiza en cada sincronización. |

Descarga al dispositivo: solo cuotas `VENCIDA` y `FUTURA` de operaciones activas (no anuladas, cerradas o pagadas).

---

## `asignaciones_mensuales`

| Columna | Tipo | Nulo | Descripción |
|---|---|---|---|
| `id` | UUID | No | PK. |
| `ejecutivo_id` | UUID | No | FK → `usuarios`. |
| `fecha_inicio` | DATE | No | Inicio del período mensual. |
| `fecha_fin` | DATE | Sí | Fin del período. NULL = vigente. |
| `fuente` | VARCHAR(50) | No | `CSV`, `API_EXTERNA`. |
| `created_at` | TIMESTAMPTZ | No | |

---

## `asignaciones_mensuales_personas` (relación N:M)

| Columna | Tipo | Nulo | Descripción |
|---|---|---|---|
| `asignacion_mensual_id` | UUID | No | FK → `asignaciones_mensuales`. |
| `persona_id` | UUID | No | FK → `personas`. |
| `created_at` | TIMESTAMPTZ | No | |

PK compuesta: `(asignacion_mensual_id, persona_id)`.

---

## `asignaciones_diarias`

| Columna | Tipo | Nulo | Descripción |
|---|---|---|---|
| `id` | UUID | No | PK. |
| `asignacion_mensual_id` | UUID | No | FK → `asignaciones_mensuales`. |
| `ejecutivo_id` | UUID | No | FK → `usuarios`. Ejecutivo destinatario. |
| `supervisor_id` | UUID | No | FK → `usuarios`. Supervisor que creó y publicó. |
| `fecha` | DATE | No | Día de la asignación. |
| `estado` | VARCHAR(20) | No | `BORRADOR`, `PUBLICADA`, `FINALIZADA`, `CANCELADA`. |
| `fecha_publicacion` | TIMESTAMPTZ | Sí | Momento de publicación. NULL si está en BORRADOR. |
| `created_at` | TIMESTAMPTZ | No | |
| `updated_at` | TIMESTAMPTZ | No | |

---

## `asignaciones_diarias_personas` (relación N:M)

| Columna | Tipo | Nulo | Descripción |
|---|---|---|---|
| `asignacion_diaria_id` | UUID | No | FK → `asignaciones_diarias`. |
| `persona_id` | UUID | No | FK → `personas`. |
| `created_at` | TIMESTAMPTZ | No | |

PK compuesta: `(asignacion_diaria_id, persona_id)`.

---

## `descargas_asignacion_diaria`

| Columna | Tipo | Nulo | Descripción |
|---|---|---|---|
| `id` | UUID | No | PK. |
| `asignacion_diaria_id` | UUID | No | FK → `asignaciones_diarias`. |
| `dispositivo_id` | UUID | No | FK → `dispositivos`. |
| `fecha_primera_descarga` | TIMESTAMPTZ | No | Primera vez que se descargó desde este dispositivo. |
| `fecha_ultima_descarga` | TIMESTAMPTZ | No | Última descarga (puede descargarse más de una vez). |
| `version_descargada` | VARCHAR(20) | No | Versión de la app al momento de la última descarga. |

---

## `tipos_gestion` (catálogo)

| Columna | Tipo | Nulo | Descripción |
|---|---|---|---|
| `codigo` | VARCHAR(30) | No | PK. Ej: `CONTACTO_FAMILIAR`. |
| `descripcion` | VARCHAR(200) | No | Descripción legible. |
| `activo` | BOOLEAN | No | Si está disponible para nuevas gestiones. |

Valores iniciales: `CONTACTO_FAMILIAR`, `COMPROMISO_PAGO`, `SIN_CONTACTO`.

---

## `gestiones`

Implementada en V010. Append-only: sin `updated_at`. Ver ADR-0026, ADR-0027, ADR-0028, ADR-0029.

| Columna                   | Tipo              | Nulo | Descripción |
|---------------------------|-------------------|------|-------------|
| `id`                      | UUID              | No   | PK. **Generado en el dispositivo Android** (ADR-0027). |
| `origen_gestion`          | VARCHAR(30)       | No   | `ASIGNACION_DIARIA` o `BUSQUEDA_DIRECTA` (ADR-0026). CHECK constraint. |
| `asignacion_diaria_id`    | UUID              | Sí   | FK → `asignaciones_diarias`. NULL si `origen_gestion = BUSQUEDA_DIRECTA`. |
| `persona_id`              | UUID              | No   | FK → `personas`. |
| `ejecutivo_id`            | UUID              | No   | FK → `usuarios`. Debe tener rol `EJECUTIVO_TERRENO`. |
| `tipo_gestion`            | VARCHAR(30)       | No   | `CONTACTO_FAMILIAR`, `COMPROMISO_PAGO`, `SIN_CONTACTO`. CHECK constraint. |
| `fecha_gestion`           | TIMESTAMPTZ       | No   | Fecha y hora en el reloj del **dispositivo** (ADR-0029). |
| `observacion`             | TEXT              | Sí   | Texto libre del ejecutivo. |
| `observacion_direccion`   | TEXT              | Sí   | Dirección reportada como incorrecta desde terreno. |
| `latitud`                 | DOUBLE PRECISION  | No   | Coordenada capturada al momento del registro. |
| `longitud`                | DOUBLE PRECISION  | No   | Coordenada capturada al momento del registro. |
| `precision_metros`        | REAL              | No   | Precisión GPS en metros (≥ 0). |
| `proveedor_gps`           | VARCHAR(50)       | Sí   | Proveedor (GPS, NETWORK, FUSED, etc.). |
| `ubicacion_simulada`      | BOOLEAN           | No   | Si Android detectó mock location. Sin DEFAULT. |
| `fecha_captura_gps`       | TIMESTAMPTZ       | No   | Momento en que se capturó la ubicación GPS. |
| `fecha_compromiso`        | DATE              | Sí   | Obligatorio si `tipo_gestion = COMPROMISO_PAGO`. Sin monto. |
| `fecha_creacion_servidor` | TIMESTAMPTZ       | No   | Momento de recepción en la API (generado en servidor, ADR-0029). |

Inmutable: sin `updated_at`. No hay corrección ni anulación en el MVP (ADR-0028).
`tipo_gestion` y `origen_gestion` usan CHECK constraints, no FK a tabla de catálogo.

---

## `fotografias_gestion`

| Columna | Tipo | Nulo | Descripción |
|---|---|---|---|
| `id` | UUID | No | PK. |
| `gestion_id` | UUID | No | FK → `gestiones`. |
| `referencia` | TEXT | No | Clave o ruta en almacenamiento externo (futuro: S3). |
| `fecha_captura` | TIMESTAMPTZ | No | Momento en que se tomó la fotografía. |
| `created_at` | TIMESTAMPTZ | No | Momento de recepción en la API. |

---

## `operaciones_sincronizacion` (outbox técnico)

| Columna | Tipo | Nulo | Descripción |
|---|---|---|---|
| `id` | UUID | No | PK. |
| `dispositivo_id` | UUID | No | FK → `dispositivos`. |
| `gestion_id` | UUID | No | FK → `gestiones`. |
| `estado` | VARCHAR(30) | No | `PENDIENTE_ENVIO`, `ENVIANDO`, `SINCRONIZADA`, `ERROR_REINTENTABLE`, `ERROR_PERMANENTE`. |
| `intentos` | INTEGER | No | Número de intentos de envío. Default: 0. |
| `ultimo_error` | TEXT | Sí | Descripción del último error. |
| `created_at` | TIMESTAMPTZ | No | |
| `updated_at` | TIMESTAMPTZ | No | |

---

## `cargas_importacion`

| Columna | Tipo | Nulo | Descripción |
|---|---|---|---|
| `id` | UUID | No | PK. |
| `usuario_id` | UUID | No | FK → `usuarios`. Usuario que realizó la carga. |
| `tipo` | VARCHAR(50) | No | `CSV`, `XLSX`, `API_EXTERNA`. |
| `nombre_archivo` | VARCHAR(255) | No | Nombre original del archivo importado. |
| `filas_totales` | INTEGER | No | Total de filas en el archivo. |
| `filas_aceptadas` | INTEGER | No | Filas procesadas correctamente. |
| `filas_rechazadas` | INTEGER | No | Filas rechazadas por error. |
| `filas_advertencia` | INTEGER | No | Filas aceptadas con advertencia. |
| `estado` | VARCHAR(30) | No | `PROCESANDO`, `COMPLETADA`, `ERROR`. |
| `created_at` | TIMESTAMPTZ | No | |

---

## `errores_importacion`

| Columna | Tipo | Nulo | Descripción |
|---|---|---|---|
| `id` | UUID | No | PK. |
| `carga_importacion_id` | UUID | No | FK → `cargas_importacion`. |
| `numero_fila` | INTEGER | No | Número de fila en el archivo original. |
| `descripcion_error` | TEXT | No | Descripción del error o advertencia. |
| `fila_original` | TEXT | Sí | Contenido de la fila que generó el error. |

---

## PENDIENTE

- Definir columnas y estructura de `registros_auditoria` en el esquema `auditoria`.
- Definir `cursores_sincronizacion` para descarga incremental (timestamp o número de secuencia).
- Confirmar si `tipos_gestion` es una tabla de catálogo en BD o un enumerado en el dominio (Spring enum).
- Confirmar tipos exactos de `latitud` y `longitud` (DOUBLE PRECISION vs DECIMAL).
- Definir índices: `(rut_numero, rut_dv)` en `personas`, `persona_id` en `gestiones`, `ejecutivo_id` en `asignaciones_diarias`, índice espacial GIST en `gestiones.ubicacion`.
