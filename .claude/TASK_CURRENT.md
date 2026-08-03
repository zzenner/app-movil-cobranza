# Tarea actual

## Identificación

- **Fase:** 4C-B — Búsqueda directa por RUT (BUSQUEDA_DIRECTA)
- **Estado:** IMPLEMENTADA ✅ — PENDIENTE COMMIT
- **Rama activa:** `feature/fase-4c-b-busqueda-directa`
- **Base (main):** `dec7b18 feat(android): implementar gestiones offline fase 4c-a`
- **Tag de fase anterior:** `v0.11.0-gestiones-offline`

## Objetivo

Permitir al ejecutivo registrar gestiones para personas **fuera de su asignación diaria**, buscando por RUT directamente en la API.

## Estado de verificación

| Suite | Resultado |
|---|---|
| API — `./mvnw clean verify` | ✅ 269 pruebas — 0 failures |
| Android — `assembleDebug` | ✅ BUILD SUCCESSFUL |
| Android — `lint` | ✅ BUILD SUCCESSFUL |
| Android — `testDebugUnitTest` | ✅ 165 pruebas — 0 failures |

## Implementado

### API (`apps/api/`)

- `POST /api/v1/personas/busquedas` — RUT en body, `Cache-Control: no-store`, rol `EJECUTIVO_TERRENO`
- `RutValidacionApi` en `personas.api` — sin exponer tipo `Rut` interno
- `BusquedaPersonaService` en módulo `sincronizacion`
- `RutInvalidoEnBusquedaException` con código `RUT_INVALIDO`
- 21 tests de integración; **269 pruebas totales**

### Android (`apps/mobile-android/`)

- Room v3: `persona_directa` (snapshot JSON), `asignacionDiariaId` nullable, `MIGRATION_2_3` no destructiva
- `:feature:busqueda` completo (RutValidator, Repository, ViewModel, Screen, Navigation)
- `GestionForm.origenGestion` explícito; GestionMapper y GestionValidator actualizados
- `HomeScreen` botón "Buscar persona por RUT"; `CobranzaNavGraph` con rutas de búsqueda
- Schema Room v3: `3.json` (11 entidades)
- **165 pruebas JVM totales — 0 failures**

### Documentación

- ADR-0041: endpoint-busqueda-privacidad-rut.md ✅
- ADR-0042: persistencia-snapshot-directo-room-v3.md ✅
- contracts/openapi/cobranza-api.yaml — v1.0.0 con nuevo endpoint ✅
- STATUS.md, CHANGELOG.md, ROADMAP.md ✅
- MODULOS.md, ESTRATEGIA_OFFLINE.md, PROTOCOLO_SINCRONIZACION.md ✅
- REGLAS_NEGOCIO.md, REQUISITOS_FUNCIONALES.md, HISTORIAS_USUARIO.md ✅
- apps/api/README.md, apps/mobile-android/README.md ✅

## No incluye

- Fotografías (diferidas — ADR-0030)
- Cambios en outbox/lease/GPS
- No push al remoto sin autorización explícita

## Constraints activos

- No fallbackToDestructiveMigration
- No push al remoto sin autorización explícita
- No commit sin autorización explícita
- GPS via LocationManager (no FusedLocationProviderClient)
- CAS atómico para lease (no bulk reset al iniciar worker)

## Fase 4C-A — CERRADA ✅

- **Commit:** `dec7b18` — rama `feature/fase-4c-gestiones-offline`
- **Tag:** `v0.11.0-gestiones-offline`
- **API:** 248 pruebas — 0 failures
- **Android JVM:** 143 pruebas — 0 failures (antes de Fase 4C-B)

## Fase 4B — CERRADA ✅

- **Tag:** `v0.10.0-descarga-offline`
- **Android JVM:** 97 pruebas — 0 failures
