# ADR-0037 — Outbox pattern y migración Room v1→v2 (Fase 4C-A)

**Estado:** Aceptado — 2026-08-02
**Contexto:** Fase 4C-A — Gestiones offline desde ASIGNACION_DIARIA

---

## Contexto

Las gestiones de cobranza se registran sin conexión y deben enviarse a la API una vez disponible la red. Necesitamos una tabla de salida (outbox) en Room que persista las gestiones pendientes de envío y sobreviva tanto al reinicio de la app como a la descarga de un nuevo bundle de asignación.

La base de datos Room ya existe en v1. Agregar la nueva tabla requiere una migración explícita (sin destrucción) para que los usuarios que actualicen la app no pierdan sus datos financieros ya descargados.

## Decisión

### Patrón Outbox

Se crea la tabla `gestion_local` como outbox local. Cada gestión nace en `PENDIENTE_ENVIO` y transiciona de estado hasta llegar a `SINCRONIZADA`, `ERROR_PERMANENTE` o `CONFLICTO` como estado terminal.

### Sin clave foránea a `persona`

`gestion_local` **no** tiene FK a la tabla `persona`. El motivo: `BundleReplacementTransaction.reemplazar()` borra y recrea la tabla `persona` en cada descarga. Una FK con `ON DELETE RESTRICT` bloquearía la descarga si hay gestiones pendientes; con `ON DELETE CASCADE` eliminaría silenciosamente las gestiones no enviadas. Ambas consecuencias son inaceptables.

Solución: campos desnormalizados `personaRutNumero`, `personaRutDv` y `personaNombre` copiados al insertar la gestión. Los datos de persona son inmutables durante la vida de una asignación diaria.

### Migración no destructiva v1→v2

```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `gestion_local` (...)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_gestion_local_personaId` ...")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_gestion_local_estadoSincronizacion` ...")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_gestion_local_fechaCreacionLocalEpoch` ...")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_gestion_local_estadoSincronizacion_fechaProximoIntentoEpoch` ...")
    }
}
```

No se usa `fallbackToDestructiveMigration`. Si Room detecta un esquema incompatible en producción, la app lanzará una excepción explícita en lugar de borrar datos silenciosamente.

### El outbox sobrevive a `reemplazar()`

`BundleReplacementTransaction.reemplazar()` borra `gestion_historica`, `cuotas`, `operaciones`, `direcciones`, `avales`, `personas` y `asignacion_diaria`, pero **no** toca `gestion_local`. Las gestiones pendientes de envío se conservan intactas entre descargas de asignación.

`BundleReplacementTransaction.limpiarTodo()` (logout) sí borra `gestion_local` como primera operación dentro de la transacción.

## Consecuencias

- Los datos financieros de los usuarios que actualicen la app se conservan (migración aditiva).
- Las gestiones pendientes sobreviven a la descarga de una nueva asignación diaria.
- El logout elimina todos los datos locales, incluidas las gestiones no enviadas.
- El modelo de datos de `gestion_local` no requiere joins a `persona` para el envío a la API.

## Alternativas descartadas

- **FK con ON DELETE CASCADE:** eliminaría gestiones pendientes en cada descarga. Pérdida de datos inaceptable.
- **FK con DEFERRABLE INITIALLY DEFERRED:** SQLite lo soporta, pero añade complejidad sin eliminar el problema fundamental.
- **`fallbackToDestructiveMigration`:** descartado como regla general del proyecto (ADR-0033).
