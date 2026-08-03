# Tarea actual

## Identificación

- **Fase:** 4C — Gestiones offline — PENDIENTE
- **Estado:** PREPARACIÓN — 2026-08-02
- **Rama activa:** `feature/fase-4c-gestiones-offline`
- **Base (main):** `f9382a2 feat(android): implementar descarga y consulta offline fase 4b`
- **Tag de fase anterior:** `v0.10.0-descarga-offline`

## Fase 4B — CERRADA ✅

- **Commit:** `f9382a2` — rama `feature/fase-4b-descarga-offline`
- **Tag:** `v0.10.0-descarga-offline`
- **API:** 248 pruebas — 0 failures
- **Android JVM:** 97 pruebas — 0 failures
- **assembleDebug:** BUILD SUCCESSFUL
- **lint:** BUILD SUCCESSFUL
- **assembleDebugAndroidTest:** BUILD SUCCESSFUL
- **connectedDebugAndroidTest:** ⏭️ sin emulador en WSL2
- **Esquema Room v1:** exportado y versionado en `core/database/schemas/`
- **Sin migraciones destructivas:** confirmado

## Fase 4C — Alcance pendiente de planificación

**Objetivo:** Registro y sincronización offline de gestiones de cobranza.

### Incluye (borrador)
- `:feature:gestiones` — pantallas de registro de gestión con GPS obligatorio
- `GestionPendienteEntity` en `:core:database` (patrón outbox)
- Worker de envío con backoff exponencial e idempotencia
- Estados de sincronización: `PENDIENTE_ENVIO`, `ENVIANDO`, `SINCRONIZADA`, `ERROR_REINTENTABLE`, `ERROR_PERMANENTE`
- Fotografías diferidas (ver ADR-0030)

### No incluye
- Modificar Fase 4B (cerrada)
- Admin Web
- Despliegue VPS
- Nuevas funcionalidades de autenticación

## connectedDebugAndroidTest pendiente

APKs de debug compilados en Fase 4B. Ejecutar cuando haya emulador o dispositivo físico.
