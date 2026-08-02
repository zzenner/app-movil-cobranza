# ADR-0033 — Room como base de datos local Android (Fase 4B)

**Estado:** Aceptado — 2026-08-02
**Contexto:** Fase 4B — Cartera offline

---

## Contexto

La Fase 4B requiere persistencia estructurada de la asignación diaria descargada para que el ejecutivo acceda a su cartera sin conexión. La base de datos local debe soportar consultas relacionales (personas, operaciones, cuotas, gestiones), migraciones incrementales y flujos reactivos para actualizar la UI automáticamente.

## Decisión

Se adopta **Room 2.7.2** con KSP (no KAPT) como ORM SQLite para el módulo `:core:database`.

## Detalles técnicos

### Módulo `:core:database`

Entidades y responsabilidades:

| Entidad | Tabla | Notas |
|---|---|---|
| `AsignacionDiariaEntity` | `asignacion_diaria` | UUID string, fecha, estado, ejecutivoId |
| `PersonaEntity` | `persona` | rutNumero, rutDv, nombre |
| `AsignacionPersonaEntity` | `asignacion_persona` | tabla pivote N:M |
| `DireccionEntity` | `direccion` | FK → persona, vigente boolean |
| `AvalEntity` | `aval` | FK → persona (aval de operación) |
| `OperacionEntity` | `operacion` | FK → persona, tipo, BigDecimal como String |
| `CuotaEntity` | `cuota` | FK → operacion, estado, fechas como String ISO |
| `GestionHistoricaEntity` | `gestion_historica` | Últimas 10 gestiones descargadas; solo lectura |
| `SyncMetadataEntity` | `sync_metadata` | Singleton (id=1); estado, timestamps, flags |

### Decisiones de tipo

- **UUID:** almacenado como `String` — Room no tiene tipo nativo UUID.
- **BigDecimal:** almacenado como `String` — evita pérdida de precisión con `Double`; `TypeConverter` en el DAo raíz.
- **Instant:** almacenado como `Long` (epoch millis) — `TypeConverter` transparente.
- **LocalDate:** almacenado como `String` ISO (yyyy-MM-dd) — serialización directa, legible.
- **Foreign keys:** activadas con `PRAGMA foreign_keys = ON` en `RoomDatabase.Callback.onOpen()`.

### `SyncMetadataEntity` — estados del ciclo de vida

| Estado | Significado |
|---|---|
| `NONE` | Nunca se ha intentado una descarga |
| `EXITOSA` | Última descarga completó con datos |
| `SIN_ASIGNACION` | API respondió 204; no hay asignación activa hoy |
| `ERROR` | Último intento falló; puede haber datos anteriores |
| `VERSION_NO_SOPORTADA` | API requiere una versión más reciente de la app |

Los campos `datosAnterioresDisponibles` y `datosMarcadosComoDesactualizados` permiten mostrar un aviso en la UI cuando los datos locales corresponden a un día anterior.

### Transacción atómica de reemplazo (`BundleReplacementTransaction`)

La actualización de la cartera se hace en una única transacción `db.withTransaction { }`:
1. Eliminar gestiones históricas → cuotas → operaciones → avales → direcciones → asignación_persona → personas → asignación_diaria.
2. Insertar en orden inverso (sin violar FK).
3. Actualizar `SyncMetadataEntity`.

Esto garantiza que la BD nunca queda en estado parcial.

## Consecuencias

- Room KSP genera los DAOs en tiempo de compilación; no hay reflexión en runtime.
- Los DAOs retornan `Flow<T>` para consultas reactivas; el ViewModel observa sin polling.
- `BundleReplacementTransaction` es la única clase que escribe múltiples tablas; los DAOs son de solo lectura o de inserción individual.
- La migración de Room se versiona manualmente (`autoMigrations` opcionales en fases futuras).

## Alternativas descartadas

- **SQLDelight:** más typesafe pero requiere escribir SQL manualmente; curva de aprendizaje mayor para el equipo.
- **Realm:** licencia, tamaño del SDK y acoplamiento con su ecosistema.
- **KAPT en lugar de KSP:** KAPT es lento y no compatible a largo plazo con Kotlin 2.x.
