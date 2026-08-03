# ADR-0042 — Persistencia de snapshot en Room v3 para búsqueda directa

**Estado:** Aceptado
**Fecha:** 2026-08-03
**Fase:** 4C-B — Búsqueda directa por RUT

---

## Contexto

La búsqueda directa por RUT requiere que la app Android pueda:

1. Registrar una gestión para la persona encontrada, **incluyendo después de perder conexión** (el ejecutivo puede buscar en zona con cobertura y registrar la gestión en zona sin cobertura minutos después).
2. Pre-rellenar el formulario de gestión con los datos de la persona (nombre, RUT) sin hacer una segunda llamada a la API.
3. Mostrar las operaciones y gestiones históricas de la persona en el momento de la búsqueda.

La tabla `persona` de Room no sirve como fuente de datos para búsquedas directas porque solo contiene personas de la asignación diaria activa; una persona buscada puede no pertenecer a esa asignación.

---

## Decisión

**Crear la tabla `persona_directa` en Room v3, que almacena un snapshot JSON completo de la respuesta de la API para cada persona consultada por búsqueda directa.**

La migración de Room v2→v3 es obligatoriamente no destructiva (sin `fallbackToDestructiveMigration`).

---

## Diseño de `PersonaDirectaEntity`

```kotlin
@Entity(tableName = "persona_directa",
        indices = [Index(value = ["rutNumero", "rutDv"])])
data class PersonaDirectaEntity(
    @PrimaryKey val id: String,         // UUID de la persona (personaId)
    val rutNumero: String,
    val rutDv: String,
    val nombre: String,
    val versionContrato: Int,           // version del contrato de respuesta (actualmente 1)
    val generadoEnEpoch: Long,          // Instant.generadoEn de la respuesta API
    val fechaConsultaEpoch: Long,       // Instant.now() en el dispositivo al guardar
    val detalleJson: String,            // JSON completo de DatosPersonaDescarga
)
```

El campo `detalleJson` serializa el objeto completo `DatosPersonaDescarga` recibido de la API, incluyendo operaciones, cuotas, direcciones, avales y últimas gestiones históricas. Esto permite al formulario de gestión mostrar información rica sin una segunda llamada de red.

---

## Migración Room v2→v3 (MIGRATION_2_3)

SQLite no soporta `ALTER COLUMN` para cambiar la nullabilidad de una columna existente. La migración requiere:

1. **Crear `gestion_local_new`** con `asignacionDiariaId TEXT` (nullable) en lugar de `TEXT NOT NULL`.
2. **Copiar los 25 campos** de `gestion_local` a `gestion_local_new` con columnas explícitas (nunca `SELECT *`).
3. **DROP + RENAME**: eliminar la tabla original y renombrar la nueva.
4. **Recrear los 4 índices** de `gestion_local`.
5. **Crear `persona_directa`** con su índice en `(rutNumero, rutDv)`.

El orden es crítico: la migración completa sucede en una sola llamada a `execSQL` por instrucción, dentro de la misma transacción que Room gestiona automáticamente.

---

## Comportamiento del snapshot

- La tabla `persona_directa` actúa como **caché de búsquedas recientes**.
- Cada nueva búsqueda hace un `upsert` por `id` (UUID de la persona): si la persona ya está en caché, se actualiza con los datos más recientes.
- `BundleReplacementTransaction.limpiarTodo()` (logout) elimina `persona_directa` junto con todas las demás tablas.
- `BundleReplacementTransaction.reemplazar()` (descarga de asignación) **no toca** `persona_directa`, para no perder snapshots de búsquedas directas.

---

## Cambio en `GestionLocalEntity`

El campo `asignacionDiariaId` cambia de `String` (NOT NULL) a `String?` (nullable):

```kotlin
// ANTES (v2):
val asignacionDiariaId: String

// DESPUÉS (v3):
val asignacionDiariaId: String?
```

Este cambio es el que obliga a recrear la tabla (SQLite no permite ALTER COLUMN).

---

## Alternativas descartadas

| Alternativa | Razón de descarte |
|---|---|
| Guardar solo el `personaId` y rellamada a la API al abrir el formulario | Requiere conexión en el momento de registrar la gestión. Rompe offline-first. |
| Reutilizar la tabla `persona` de Room | Esa tabla solo contiene personas de la asignación diaria activa. Una persona encontrada por búsqueda puede no estar en esa tabla y sería sobrescrita en la próxima descarga de bundle. |
| Tabla separada con campos individuales | El esquema de DatosPersonaDescarga incluye arrays anidados (operaciones, cuotas, avales). Normalizarlos requeriría 5+ tablas adicionales para un caso de uso de caché temporal. |
| `fallbackToDestructiveMigration` | Prohibido explícitamente en los constraints del proyecto. Borraría datos de usuario. |

---

## Consecuencias

**Positivas:**
- El formulario de gestión BUSQUEDA_DIRECTA no requiere conexión después de la búsqueda.
- El snapshot incluye operaciones y cuotas, lo que permite al ejecutivo tomar decisiones informadas.
- La tabla es una caché (no fuente de verdad financiera): se actualiza en cada nueva búsqueda del mismo RUT.

**Negativas:**
- El `detalleJson` puede volverse obsoleto si los datos financieros de la persona cambian en el servidor entre la búsqueda y el envío de la gestión. Este es un trade-off aceptado: los datos son de referencia, no operativos.
- La migración v2→v3 es compleja (25 columnas explícitas). Un error en los nombres de columna silenciaría datos. Se mitiga con pruebas de migración en `MigrationTest`.

---

## Referencias

- [ADR-0037](0037-outbox-migracion-room-v2.md) — Outbox pattern y migración Room v2
- [ADR-0041](0041-endpoint-busqueda-privacidad-rut.md) — Endpoint de búsqueda con POST
- `apps/mobile-android/core/database/src/main/java/cl/zzenner/cobranza/core/database/migration/Migrations.kt` — MIGRATION_2_3
