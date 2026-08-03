# Tarea actual

## Identificación

- **Fase:** 4C-B — Búsqueda directa por RUT (BUSQUEDA_DIRECTA)
- **Estado:** PENDIENTE — 2026-08-03
- **Rama activa:** `feature/fase-4c-b-busqueda-directa`
- **Base (main):** `dec7b18 feat(android): implementar gestiones offline fase 4c-a`
- **Tag de fase anterior:** `v0.11.0-gestiones-offline`

## Objetivo

Permitir al ejecutivo registrar gestiones para personas **fuera de su asignación diaria**, buscando por RUT directamente en la API.

## Alcance

### API (`apps/api/`)

- Nuevo endpoint: `GET /api/v1/personas/buscar?rut=<numero>-<dv>`
  - Autenticación: `hasRole('EJECUTIVO_TERRENO')`
  - Respuesta 200: datos de persona (nombre, RUT, operaciones activas)
  - Respuesta 404: persona no encontrada
  - Respuesta 400: RUT inválido
- La persona encontrada puede no estar en la asignación diaria del ejecutivo.
- Tests de integración.

### Android (`apps/mobile-android/`)

- UI de búsqueda global por RUT (nueva pantalla o flujo desde Home).
- Llamada a `GET /api/v1/personas/buscar?rut=...` al confirmar.
- Formulario de gestión con `origenGestion = BUSQUEDA_DIRECTA` y `asignacionDiariaId = null`.
- Validación: `origenGestion = BUSQUEDA_DIRECTA` ↔ `asignacionDiariaId = null` (ya existe en `GestionValidator`).
- El outbox procesa gestiones BUSQUEDA_DIRECTA igual que ASIGNACION_DIARIA.

## No incluye

- Fotografías (diferidas — ADR-0030)
- Cambios en el modelo Room (ya soporta `asignacionDiariaId = null`)
- Cambios en `BundleReplacementTransaction`
- Cambios en el outbox o lease

## Constraints activos

- No fallbackToDestructiveMigration
- No convertir ERROR_REINTENTABLE → ERROR_PERMANENTE por conteo
- GPS via LocationManager (no FusedLocationProviderClient)
- Logout bloquea para TODOS los estados no-SINCRONIZADA (sin "salir igualmente")
- CAS atómico para lease (no bulk reset al iniciar worker)
- No push al remoto sin autorización explícita

## Fase 4C-A — CERRADA ✅

- **Commit:** `dec7b18` — rama `feature/fase-4c-gestiones-offline`
- **Tag:** `v0.11.0-gestiones-offline`
- **API:** 248 pruebas — 0 failures
- **Android JVM:** 143 pruebas — 0 failures

## Fase 4B — CERRADA ✅

- **Tag:** `v0.10.0-descarga-offline`
- **Android JVM:** 97 pruebas — 0 failures
