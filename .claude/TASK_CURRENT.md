# Tarea actual

## Identificación

- **Fase:** 4C-A — Gestiones offline desde ASIGNACION_DIARIA — AUDITADA ✅ PENDIENTE COMMIT
- **Estado:** PENDIENTE COMMIT — 2026-08-02
- **Rama activa:** `feature/fase-4c-gestiones-offline`
- **Base (main):** `f9382a2 feat(android): implementar descarga y consulta offline fase 4b`
- **Tag de fase anterior:** `v0.10.0-descarga-offline`

## Resultados de auditoría final

| Suite | Resultado |
|---|---|
| API — `./mvnw clean verify` | ✅ **248 tests — 0 failures — BUILD SUCCESS** |
| Android `assembleDebug` | ✅ BUILD SUCCESSFUL |
| Android `lint` | ✅ BUILD SUCCESSFUL — sin errores |
| Android `testDebugUnitTest` | ✅ **143 tests — 0 failures — 0 errors** |

### Desglose de los 143 tests Android JVM

| Módulo | Clase | Tests |
|---|---|---|
| core:database | MigrationTest | 4 |
| core:database | GestionLocalDaoTest | 14 |
| core:database | BundleReplacementTransactionTest | 7 |
| feature:gestion | GestionValidatorTest | 7 |
| feature:gestion | GestionMapperTest | 5 |
| feature:gestion | GestionRepositoryTest | 10 |
| feature:gestion | EnvioGestionWorkerTest | 4 |
| app | LogoutUseCaseTest | 4 |
| app | LogoutIntegrationTest | 4 |
| *Fase 4B y anteriores* | *(97 tests)* | 97 |
| **Total** | | **143** |

## Auditoría completada

- ✅ HTTP 400/403/404 → ERROR_PERMANENTE (3 tests explícitos en GestionRepositoryTest)
- ✅ BundleReplacementTransaction.reemplazar() preserva gestion_local (test explícito)
- ✅ 5 estados no-SINCRONIZADA individualmente testeados en contarNoResueltas
- ✅ ADR-0037..0040 creados
- ✅ MODULOS.md, STATUS.md, CHANGELOG.md, ROADMAP.md actualizados

## Constraints activos

- No commit ni push sin autorización explícita
- No implementar BUSQUEDA_DIRECTA (sin endpoint API)
- No agregar fotografías
- No fallbackToDestructiveMigration
- No convertir ERROR_REINTENTABLE → ERROR_PERMANENTE por conteo
- GPS via LocationManager (no FusedLocationProviderClient)
- Logout bloquea para TODOS los estados no-SINCRONIZADA (sin "salir igualmente")
- CAS atómico para lease (no bulk reset al iniciar worker)

## Fase 4C-A — Alcance implementado

### Completado ✅
- Room v1 → v2: tabla `gestion_local` con lease, campos desnormalizados persona
- `GestionLocalDao` con adquisición atómica CAS + lease recovery FIFO
- Migration `MIGRATION_1_2` con SQL exacto + 4 índices
- `:core:network` — `GestionApi`, `GestionDtos`
- `:feature:gestion` — módulo completo (domain, data, GPS, worker, DI, UI)
- `LogoutUseCase` — con `GestionSyncScheduler`
- `HomeViewModel` — `EstadoLogout` sealed class, política bloqueo
- `HomeScreen` — diálogo (sin "salir igualmente")
- `CobranzaNavGraph` — rutas gestionNavGraph
- `AsignacionNavigation` — ruta con `asignacionDiariaId`
- `PersonaDetalleScreen` — botón real + SeccionGestionesLocales
- ADR-0037..0040

### No incluye en esta entrega
- BUSQUEDA_DIRECTA (Fase 4C-B, requiere endpoint API)
- Fotografías (ADR-0030, diferidas)

## Fase 4B — CERRADA ✅

- **Commit:** `f9382a2` — rama `feature/fase-4b-descarga-offline`
- **Tag:** `v0.10.0-descarga-offline`
- **API:** 248 pruebas — 0 failures
- **Android JVM:** 97 pruebas — 0 failures
