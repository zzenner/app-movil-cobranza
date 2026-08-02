# Checkpoint de sesión — cierre Fase 4B

**Fecha:** 2026-08-02
**Rama activa:** `feature/fase-4b-descarga-offline`

---

## Estado de Git

| Referencia | Hash |
|---|---|
| main local | `3d00ccf` |
| origin/main | `3d00ccf` |
| feature/fase-4b-descarga-offline | sin commit nuevo (sin `git commit` — restricción del usuario) |

La rama `feature/fase-4b-descarga-offline` tiene cambios sin confirmar (toda la Fase 4B). El usuario ha indicado explícitamente: NO hacer commit, NO hacer push, NO crear tags.

---

## Pruebas validadas — Fase 4B (verificación final 2026-08-02)

| Suite | Resultado |
|---|---|
| API `./mvnw clean verify` | ✅ 248 tests — 0 failures — BUILD SUCCESS |
| Android JVM total (todas las suites) | ✅ 97 tests — 0 failures |
| Android `assembleDebug` | ✅ BUILD SUCCESSFUL — app-debug.apk generado |
| Android `lint` | ✅ BUILD SUCCESSFUL — sin errores ni advertencias fatales |
| Android `assembleDebugAndroidTest` | ✅ BUILD SUCCESSFUL — app-debug-androidTest.apk generado |
| Android `connectedDebugAndroidTest` | ⏭️ No ejecutado — sin emulador en WSL2 |
| Android clean build completo | ✅ BUILD SUCCESSFUL in 1m 39s |

### Suites JVM detalladas (verificación final)

| Clase | Tests | Failures |
|---|---|---|
| `BigDecimalSerializerTest` | 8 | 0 |
| `AsignacionRepositoryTest` | 7 | 0 |
| `AsignacionViewModelTest` | 5 | 0 |
| `DescargaAsignacionWorkerTest` | 5 | 0 |
| `AsignacionMapperTest` | 6 | 0 |
| `DatabaseSchemaTest` | 4 | 0 |
| `BundleReplacementTransactionTest` | 6 | 0 |
| `PersonaDaoTest` | 5 | 0 |
| `SyncMetadataDaoTest` | 5 | 0 |
| `LogoutUseCaseTest` | 4 | 0 |
| `LogoutIntegrationTest` (nuevo — Room/Robolectric) | 4 | 0 |
| `TokenRefreshTest` (Fase 4A) | 5 | 0 |
| Otras Fase 4A | 33 | 0 |

---

## Archivos creados en Fase 4B

### `:core:database`
- `core/database/build.gradle.kts`
- `core/database/consumer-rules.pro`
- `core/database/src/main/java/.../entity/` — 9 entidades
- `core/database/src/main/java/.../dao/` — 8 DAOs + `PersonaConDetalle`
- `core/database/src/main/java/.../transaction/BundleReplacementTransaction.kt`
- `core/database/src/main/java/.../CobranzaDatabase.kt`
- `core/database/src/main/java/.../di/DatabaseModule.kt`
- `core/database/src/test/java/...` — 4 suites de prueba (Robolectric)

### `:core:network`
- `core/network/src/main/java/.../api/BigDecimalSerializer.kt`
- `core/network/src/main/java/.../api/SyncModels.kt`
- `core/network/src/main/java/.../api/SincronizacionApi.kt`
- `core/network/src/main/java/.../client/NetworkModule.kt` — modificado (añadido `provideSincronizacionApi`)

### `:feature:asignacion`
- `feature/asignacion/build.gradle.kts`
- `feature/asignacion/consumer-rules.pro`
- `feature/asignacion/src/main/java/.../data/AsignacionRepository.kt`
- `feature/asignacion/src/main/java/.../data/AsignacionMapper.kt`
- `feature/asignacion/src/main/java/.../domain/ResultadoDescarga.kt`
- `feature/asignacion/src/main/java/.../domain/PersonaResumen.kt`
- `feature/asignacion/src/main/java/.../domain/SyncMetadataUi.kt`
- `feature/asignacion/src/main/java/.../domain/RutUtils.kt`
- `feature/asignacion/src/main/java/.../worker/DescargaAsignacionWorker.kt`
- `feature/asignacion/src/main/java/.../worker/AsignacionSyncScheduler.kt`
- `feature/asignacion/src/main/java/.../ui/AsignacionViewModel.kt`
- `feature/asignacion/src/main/java/.../ui/AsignacionListScreen.kt`
- `feature/asignacion/src/main/java/.../ui/PersonaDetalleScreen.kt`
- `feature/asignacion/src/main/java/.../navigation/AsignacionNavGraph.kt`
- `feature/asignacion/src/main/AndroidManifest.xml`
- `feature/asignacion/src/test/java/...` — 5 suites de prueba

### `:core:security` — correcciones preexistentes de Fase 4A
- `build.gradle.kts` — añadido `androidTestImplementation(kotlinx-coroutines-test)` y `androidTestImplementation(androidx.test.ext.junit)` que faltaban
- `SecureTokenStoreInstrumentedTest.kt` — corregida sintaxis `first()` (era `kotlinx.coroutines.flow.first(flow) { true }`, debe ser `flow.first()`) y agregado import `kotlinx.coroutines.flow.first`

### `:feature:auth` — modificaciones
- `SessionRepository.kt` — `@Singleton` (era `@ActivityRetainedScoped`)
- `AuthModule.kt` — nuevo; `@Binds TokenProvider`
- `CobranzaNavGraph.kt` — refactorizado a `authNavGraph` (extensión de NavGraphBuilder)

### `:app` — modificaciones
- `CobranzaApp.kt` — añadido `Configuration.Provider` + `HiltWorkerFactory`
- `CobranzaNavGraph.kt` — nuevo; NavHost completo
- `HomeScreen.kt` — nuevo
- `LogoutUseCase.kt` — nuevo
- `AndroidManifest.xml` — WorkManager auto-init deshabilitado
- `build.gradle.kts` — añadidas dependencias de Fase 4B

### Documentación
- `docs/adr/0033-room-base-datos-local-android.md`
- `docs/adr/0034-workmanager-sincronizacion-background.md`
- `docs/adr/0035-reemplazo-atomico-bundle-descargado.md`
- `docs/adr/0036-arquitectura-modulo-feature-asignacion.md`
- `docs/gestion/STATUS.md` — actualizado
- `docs/gestion/CHANGELOG.md` — actualizado
- `docs/gestion/ROADMAP.md` — actualizado
- `docs/arquitectura/MODULOS.md` — actualizado

---

## Stack definitivo (Fase 4B añade)

| Componente | Versión |
|---|---|
| AGP | 9.3.0 |
| Gradle | 9.6.1 |
| Kotlin | 2.4.10 |
| KSP | 2.3.10 |
| Compose BOM | 2026.06.01 |
| Hilt | 2.60.1 / AndroidX 1.4.0 |
| Room | 2.7.2 |
| WorkManager | 2.10.1 |
| Robolectric | 4.14.1 |
| compileSdk | 37 |
| targetSdk | 36 |
| minSdk | 29 |

---

## Correcciones aplicadas en verificación final (2026-08-02)

1. **`BundleReplacementTransaction.reemplazar()`** — añadido `fechaConsultada = bundle.asignacion.fecha` al upsert de `SyncMetadataEntity`
2. **`BundleReplacementTransactionTest`** — añadido test `reemplazo atomico actualiza metadata con fechaConsultada`
3. **`AsignacionDescargaRestTest.java`** — añadido Test 13: contrato JSON Android (nombres de campos, BigDecimal como número, UUID lowercase, fechas ISO-8601)
4. **`LogoutUseCaseTest`** (nuevo en `:app`) — 4 tests: orden correcto, llamadas exactas a cada colaborador
5. **`SyncMetadataEntity`** — documentados `versionContrato` y `generadoEn` como reservados (API no los devuelve actualmente)
6. **`CobranzaDatabase`** — `exportSchema = true`; schema v1 generado en `core/database/schemas/`
7. **`LogoutUseCase`** — `sessionRepository.logout()` envuelto en `runCatching` (best-effort: fallo de red no impide limpieza local)
8. **`LogoutIntegrationTest`** — 4 tests Room/Robolectric: verifica limpieza completa de Room, cancelación de workers, cierre de sesión y comportamiento best-effort ante fallo de red
9. **`app/build.gradle.kts`** — añadido `robolectric`, `room-testing`, `androidx-test-core` para tests de integración en `:app`

## Siguiente acción exacta

Cierre formal de Fase 4B: commit + PR/merge a main + tag. Apertura de Fase 4C (gestiones offline: outbox, GPS, fotografías diferidas).

Antes de iniciar Fase 4C, revisar:
- `docs/sincronizacion/PROTOCOLO_SINCRONIZACION.md` — cola outbox
- `docs/adr/0026-dos-origenes-de-gestion.md` — ASIGNACION_DIARIA vs BUSQUEDA_DIRECTA
- `docs/adr/0028-gestion-inmutable.md`
- `docs/adr/0029-separacion-temporal-dispositivo-servidor.md`
- `docs/adr/0030-fotografias-diferidas.md`

---

## No repetir

- Agregar `fechaConsultada` a `BundleReplacementTransaction.reemplazar()` (ya corregido)
- Agregar Test 13 a `AsignacionDescargaRestTest` (ya corregido)
- Crear `LogoutUseCaseTest` (ya creado)
- Crear `LogoutIntegrationTest` Room/Robolectric (ya creado)
- Corregir best-effort en `LogoutUseCase` (ya corregido con `runCatching`)
- Documentar `versionContrato`/`generadoEn` en `SyncMetadataEntity` (ya documentado)
- Agregar `kotlinx-coroutines-test` a `core:security` androidTest (ya corregido)
- Agregar `androidx.test.ext.junit` a `core:security` androidTest (ya corregido)
- Corregir `first()` en `SecureTokenStoreInstrumentedTest` (ya corregido)
- Mover `Locale.getDefault()` fuera de Composables (ya corregido en AsignacionListScreen, PersonaDetalleScreen, HomeScreen)
- Commit de Fase 4A (ya en `3d00ccf`)
- Merge a main (fast-forward completado en 4A)
- Tag `v0.9.0-android-base` (publicado en 4A)
- Modificar contrato de login (corregido en `3d00ccf`)
- Instalar Android SDK 37.1 (instalado en `$HOME/Android/Sdk`)
- Corregir `buscarORegistrar` TOCTOU (en producción desde `3d00ccf`)
