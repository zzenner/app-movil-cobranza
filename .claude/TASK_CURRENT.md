# Tarea actual

## Identificación

- **Fase:** 4B — Cartera offline (Room + WorkManager + descarga de asignación)
- **Estado:** pendiente de planificación — sin implementación iniciada
- **Rama activa:** `feature/fase-4b-descarga-offline`
- **Commit base:** `3d00ccf feat(android): implementar base y autenticacion fase 4a`
- **Tag base:** `v0.9.0-android-base`

## Fase 4A — CERRADA ✅

- **Commit:** `3d00ccf`
- **Tag:** `v0.9.0-android-base`
- **main local y remoto:** apuntan a `3d00ccf`
- **Pruebas API:** 247 — 0 failures
- **Pruebas Android JVM:** 38 — 0 failures
- **Instrumentadas:** compiladas — NO ejecutadas (sin emulador en WSL2)

## Objetivo actual

Planificar Fase 4B antes de implementar nada. Leer en orden:

1. `docs/gestion/ROADMAP.md` — alcance de Fase 4B
2. `docs/sincronizacion/ESTRATEGIA_OFFLINE.md`
3. `docs/sincronizacion/PROTOCOLO_SINCRONIZACION.md`
4. `docs/dominio/MODELO_DATOS.md` — tablas que necesita Room
5. ADR existentes relacionados con offline, sincronización, Room

Presentar plan al usuario antes de implementar.

## Alcance previsto (según ROADMAP)

- Room: entidades para asignación diaria, personas, operaciones, cuotas, gestiones pendientes
- WorkManager: sincronización periódica en background
- Descarga de asignación diaria desde API y persistencia local
- Cola de gestiones pendientes y sincronización hacia la API

## Fuera de alcance (siempre)

- GPS / fotografías
- Admin Web
- Despliegue VPS
- Funcionalidad de autenticación (cerrada en Fase 4A)

## connectedDebugAndroidTest pendiente

APK instrumentado compilado. Ejecutar cuando haya emulador o dispositivo físico.
