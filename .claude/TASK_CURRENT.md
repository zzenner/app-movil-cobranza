# Tarea actual

## Identificación

- **Fase:** 4A — Android base: red, seguridad, autenticación, navegación
- **Estado:** VALIDADA ✅ — pendiente de cierre Git
- **Rama activa:** `feature/fase-4a-android-base`
- **Commit base:** `98854cf feat(api): implementar api rest asignaciones y gestiones fase 3d`
- **Restricción:** sin commit, sin push, sin tags hasta instrucción explícita

## Objetivo actual

Cierre formal de Fase 4A: commit en rama `feature/fase-4a-android-base` y apertura de PR hacia `main`.

## Alcance implementado

**API:**
- `identificadorInstalacion` reemplaza `dispositivoId` en el contrato de login
- TOCTOU fix en `buscarORegistrar()` con `INSERT … ON CONFLICT DO NOTHING`
- 2 tests de concurrencia añadidos → 247 tests totales, 0 failures

**Android (`apps/mobile-android/`):**
- Módulos: `:app`, `:core:network`, `:core:security`, `:feature:auth`
- AES-256-GCM via Android Keystore (SecureTokenStore)
- DataStore para `identificadorInstalacion` y `sessionExpiresAt`
- SingleFlightAuthenticator con Mutex
- SessionRepository `@ActivityRetainedScoped`
- Navegación: Check → Login/Home sobre AuthState
- Backup completamente deshabilitado (allowBackup=false + data_extraction_rules.xml)
- compileSdk=37, targetSdk=36, minSdk=29

## Fuera de alcance

- Room / SQLite
- WorkManager
- Descarga de asignaciones diarias
- Gestiones, GPS, fotografías
- Fase 4B (en ningún caso)

## Pruebas ejecutadas

| Suite | Resultado |
|---|---|
| API — `./mvnw clean verify` | ✅ 247 tests — 0 failures |
| Android — `assembleDebug` | ✅ BUILD SUCCESSFUL |
| Android — `lint` | ✅ BUILD SUCCESSFUL |
| Android — `testDebugUnitTest` | ✅ 38 tests — 0 failures |
| Android — `assembleDebugAndroidTest` | ✅ BUILD SUCCESSFUL |
| Android — `connectedDebugAndroidTest` | ⏭️ Sin emulador en WSL2 |

## Siguiente acción exacta

1. Recibir autorización del usuario.
2. `git add` de los archivos listados en `SESSION_HANDOFF.md`.
3. Commit con mensaje convencional describiendo Fase 4A completa.
4. Abrir PR hacia `main`.
