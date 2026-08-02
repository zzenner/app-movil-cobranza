# Tarea actual

## Identificación

- **Fase:** 4B — Cartera offline — COMPLETADA ✅
- **Estado:** VALIDADA — 2026-08-02
- **Rama activa:** `feature/fase-4b-descarga-offline`
- **Commit base:** `3d00ccf feat(android): implementar base y autenticacion fase 4a`
- **Nota:** Sin commit propio (usuario indicó explícitamente NO hacer commit)

## Fase 4B — COMPLETADA ✅

### Pruebas

| Suite | Resultado |
|---|---|
| API `./mvnw clean verify` | ✅ 247 tests — 0 failures |
| Android `:core:database:testDebugUnitTest` | ✅ 0 failures |
| Android `:feature:asignacion:testDebugUnitTest` | ✅ 31 tests — 0 failures |
| Android `assembleDebug` | ✅ BUILD SUCCESSFUL |
| Android `connectedDebugAndroidTest` | ⏭️ Sin emulador en WSL2 |

### Módulos entregados

- `:core:database` — Room 2.7.2; 9 entidades, 8 DAOs, BundleReplacementTransaction
- `:core:network` — SyncModels, BigDecimalSerializer, SincronizacionApi
- `:feature:asignacion` — Repository, Worker, Scheduler, ViewModel, Screens, Navigator
- `:feature:auth` — SessionRepository @Singleton, AuthModule
- `:app` — NavHost, LogoutUseCase, Configuration.Provider

## Próximo objetivo

**Fase 4C — Gestiones offline** (no iniciada):
- Entidad `GestionPendienteEntity` (outbox Room)
- Worker de envío de gestiones con idempotencia
- Pantallas de registro de gestiones (GPS obligatorio)
- Fotografías diferidas (ADR-0030)

## Fuera de alcance (siempre)

- Admin Web
- Despliegue VPS
- Funcionalidad de autenticación (cerrada en Fase 4A)
- Modificar Fase 4B (cerrada)

## connectedDebugAndroidTest pendiente

APKs de debug compilados. Ejecutar cuando haya emulador o dispositivo físico.
