# ADR-0035 — Reemplazo atómico del bundle descargado (Fase 4B)

**Estado:** Aceptado — 2026-08-02
**Contexto:** Fase 4B — Cartera offline

---

## Contexto

Cuando el ejecutivo descarga su asignación diaria, el bundle incluye todas las personas asignadas con sus direcciones, avales, operaciones, cuotas y últimas 10 gestiones. Los datos anteriores deben reemplazarse completamente para reflejar la asignación del día. Una actualización parcial podría dejar la BD en un estado incoherente (personas del día anterior mezcladas con las del día actual).

## Decisión

La persistencia del bundle utiliza una **transacción de reemplazo total** dentro de una única `db.withTransaction { }` de Room. La transacción nunca deja la base de datos en un estado parcial.

## Protocolo de reemplazo (`BundleReplacementTransaction.reemplazar`)

```
db.withTransaction {
    // 1. Eliminar en orden respetando FK (hijos antes que padres)
    gestionHistoricaDao.deleteAll()
    cuotaDao.deleteAll()
    operacionDao.deleteAll()
    avalDao.deleteAll()
    direccionDao.deleteAll()
    asignacionPersonaDao.deleteAll()
    personaDao.deleteAll()
    asignacionDiariaDao.deleteAll()

    // 2. Insertar en orden inverso
    asignacionDiariaDao.insert(bundle.asignacion)
    personaDao.insertAll(bundle.personas)
    asignacionPersonaDao.insertAll(bundle.asignacionPersonas)
    direccionDao.insertAll(bundle.direcciones)
    avalDao.insertAll(bundle.avales)
    operacionDao.insertAll(bundle.operaciones)
    cuotaDao.insertAll(bundle.cuotas)
    gestionHistoricaDao.insertAll(bundle.gestiones)

    // 3. Actualizar metadata de sincronización
    syncMetadataDao.upsert(SyncMetadataEntity(estado = "EXITOSA", ...))
}
```

## Protocolo para 204 No Content (`marcarSinAsignacion`)

Cuando la API responde 204, no hay asignación activa hoy. En este caso:

- No se eliminan los datos anteriores — podrían ser útiles para el ejecutivo.
- Se actualiza `SyncMetadataEntity` con `estado = "SIN_ASIGNACION"`, `datosMarcadosComoDesactualizados = true`.
- La UI muestra un aviso indicando que los datos mostrados corresponden a una fecha anterior.

## Protocolo de limpieza en logout (`limpiarTodo`)

Al cerrar sesión, se eliminan todos los datos locales en orden FK-seguro y se resetea `SyncMetadataEntity` a `estado = "NONE"`.

## Consecuencias

- El ejecutivo nunca ve datos parciales o mezclados entre días.
- Si la transacción falla a mitad, Room hace rollback y los datos anteriores permanecen intactos.
- La UI observa `SyncMetadata` vía `Flow` y reacciona inmediatamente al cambio de estado.
- `GestionHistoricaEntity` almacena solo las últimas 10 gestiones; no es la fuente de verdad para las gestiones del día actual (esas las gestiona la Fase 4C con la tabla `gestion_pendiente`).

## Alternativas descartadas

- **Upsert incremental:** requiere rastrear qué personas/operaciones fueron removidas de la asignación; complejo y propenso a errores de integridad.
- **Eliminación selectiva por ID:** require comparar IDs del bundle con los almacenados en cada tabla; más complejo sin beneficio práctico para el MVP.
- **Dos bases de datos (actual / anterior):** complejidad operativa sin beneficio claro para el MVP.
