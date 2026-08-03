# cobranza-android

App Android del sistema de cobranza en terreno — offline-first.

## Stack

- Kotlin 2.4.10
- Jetpack Compose (BOM 2026.06.01) + Material3
- Hilt 2.60.1 para inyección de dependencias
- Room 2.7.2 (KSP) para base de datos local offline
- WorkManager 2.10.1 para sincronización en segundo plano
- Retrofit 3.0.0 + OkHttp 5.4.0 para red
- kotlinx.serialization 1.11.0 para JSON
- Android Keystore + Preferences DataStore para almacenamiento seguro

## Requisitos

- Android Studio con soporte AGP 9.3+
- JDK 17
- SDK Android 37.1 instalado (`platforms;android-37.1`, `build-tools;37.0.0`)
- `compileSdk` 37 / `targetSdk` 36 / `minSdk` 29

## Estructura de módulos

```
apps/mobile-android/
├── app/                    — Actividad principal, CobranzaApp, LogoutUseCase, grafo de navegación
├── core/
│   ├── database/           — Room v2: 10 entidades, 9 DAOs, BundleReplacementTransaction, migraciones
│   ├── network/            — Cliente HTTP (público + autenticado), SingleFlightAuthenticator, GestionApi
│   └── security/           — InstallationIdStore (DataStore), SecureTokenStore (Keystore AES-GCM)
└── feature/
    ├── auth/               — LoginViewModel, SessionRepository, pantallas Check/Login/Home
    ├── asignacion/         — AsignacionRepository, DescargaAsignacionWorker, AsignacionViewModel, pantallas lista/detalle
    └── gestion/            — GestionRepository (outbox), EnvioGestionWorker, AndroidLocationProvider, pantallas formulario/historial
```

## Fases implementadas

### Fase 4A — Base Android (autenticación y red)

| Componente | Descripción |
|---|---|
| `identificadorInstalacion` | UUID persistido en DataStore; enviado al API en cada login |
| `SecureTokenStore` | Refresh token cifrado con AES-256-GCM via Android Keystore |
| `SingleFlightAuthenticator` | Renovación de token única en vuelo (Mutex), evita refresh concurrente |
| `SessionRepository` | Fuente de verdad de sesión: authState, TokenProvider, guardarSesion, logout |
| `LoginViewModel` | Formulario de login, mapeo de errores ProblemDetail → ErrorTipo |

### Fase 4B — Cartera offline (Room + WorkManager)

| Componente | Descripción |
|---|---|
| `CobranzaDatabase` v1 | 9 entidades; `exportSchema=true`; sin `fallbackToDestructiveMigration` |
| `BundleReplacementTransaction` | Reemplazo atómico del bundle descargado; `limpiarTodo()` en logout |
| `DescargaAsignacionWorker` | `@HiltWorker`; KEEP; CONNECTED; EXPONENTIAL |
| `AsignacionListScreen` | Lista offline con búsqueda por RUT/nombre |
| `PersonaDetalleScreen` | Detalle con operaciones, cuotas y gestiones locales |

### Fase 4C-A — Gestiones offline (outbox, GPS, sincronización)

| Componente | Descripción |
|---|---|
| `CobranzaDatabase` v2 | `gestion_local` (sin FK a persona); `MIGRATION_1_2` aditiva; schema `2.json` |
| `GestionLocalDao` | CAS atómico `adquirirLease()`, `recuperarLeasesExpirados()`, FIFO, `contarNoResueltas()` |
| `GestionRepository` | Outbox + Mutex single-flight; backoff `min(30s·2ⁿ, 24h)`; sin límite de intentos |
| `AndroidLocationProvider` | `LocationManager` (sin Google Play Services); timeout 30s; detección mock |
| `EnvioGestionWorker` | `@HiltWorker`; ExistingWorkPolicy.KEEP; mapea `ResultadoProcesamiento` → WorkManager.Result |
| `GestionFormScreen` | GPS obligatorio; doble-tap guard; tipos de gestión; COMPROMISO_PAGO con fecha |
| `GestionHistorialScreen` | Historial unificado local+histórico; dedup por UUID; badge por estado |
| `HomeViewModel.EstadoLogout` | Bloqueo de logout si hay gestiones no-SINCRONIZADA; sin "salir igualmente" |

### Pruebas JVM (sin dispositivo)

**143 tests — 0 failures** (Fase 4C-A). Ejecutar con:

```bash
cd apps/mobile-android
./gradlew testDebugUnitTest --no-daemon
```

## Comandos de desarrollo

```bash
cd apps/mobile-android

# Compilar debug
./gradlew assembleDebug --no-daemon

# Lint
./gradlew lint --no-daemon

# Pruebas JVM (sin dispositivo)
./gradlew testDebugUnitTest --no-daemon

# Compilar APK de pruebas instrumentadas
./gradlew :app:assembleDebugAndroidTest --no-daemon

# Pruebas instrumentadas (requiere dispositivo/emulador — no disponible en WSL2)
./gradlew connectedDebugAndroidTest --no-daemon
```

## Base de datos Room

- **Versión:** 2
- **Schemas exportados:** `core/database/schemas/cl.zzenner.cobranza.core.database.CobranzaDatabase/`
  - `1.json` — Fase 4B (9 entidades)
  - `2.json` — Fase 4C-A (agrega `gestion_local`)
- **Sin `fallbackToDestructiveMigration`** — toda migración es explícita

## Seguridad

- El refresh token **nunca** se almacena en texto plano. Se cifra con AES-256-GCM antes de persistir en DataStore, usando una clave que vive exclusivamente en el Android Keystore del sistema.
- Las credenciales (nombre de usuario y contraseña) **nunca** se almacenan.
- El backup de datos está deshabilitado explícitamente (`allowBackup=false`).

## ADRs relacionados

- [ADR-0031](../../docs/adr/0031-identificador-instalacion-login.md) — `identificadorInstalacion` en login
- [ADR-0032](../../docs/adr/0032-stack-android-fase-4a.md) — Stack técnico Android (Fase 4A)
- [ADR-0033](../../docs/adr/0033-room-base-datos-local-android.md) — Room como BD local
- [ADR-0034](../../docs/adr/0034-workmanager-sincronizacion-background.md) — WorkManager para sincronización
- [ADR-0035](../../docs/adr/0035-reemplazo-atomico-bundle-descargado.md) — Reemplazo atómico del bundle
- [ADR-0036](../../docs/adr/0036-arquitectura-modulo-feature-asignacion.md) — Arquitectura feature:asignacion
- [ADR-0037](../../docs/adr/0037-outbox-migracion-room-v2.md) — Outbox pattern y migración Room v2
- [ADR-0038](../../docs/adr/0038-estados-lease-workmanager.md) — Estados, lease CAS y WorkManager
- [ADR-0039](../../docs/adr/0039-gps-location-manager.md) — GPS vía LocationManager
- [ADR-0040](../../docs/adr/0040-logout-gestiones-no-resueltas.md) — Política de logout
