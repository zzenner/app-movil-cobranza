# cobranza-android

App Android del sistema de cobranza en terreno.

## Stack

- Kotlin 2.4.10
- Jetpack Compose (BOM 2026.06.01) + Material3
- Hilt 2.60.1 para inyección de dependencias
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
├── app/                    — Actividad principal, CobranzaApp, grafo de navegación
├── core/
│   ├── network/            — Cliente HTTP (público + autenticado), SingleFlightAuthenticator
│   └── security/           — InstallationIdStore (DataStore), SecureTokenStore (Keystore AES-GCM)
└── feature/
    └── auth/               — LoginViewModel, SessionRepository, pantallas Check/Login/Home
```

## Fase 4A — Implementada

| Componente | Descripción |
|---|---|
| `identificadorInstalacion` | UUID persistido en DataStore; enviado al API en cada login |
| `SecureTokenStore` | Refresh token cifrado con AES-256-GCM via Android Keystore |
| `SingleFlightAuthenticator` | Renovación de token única en vuelo (Mutex), evita refresh concurrente |
| `SessionRepository` | Fuente de verdad de sesión: authState, TokenProvider, guardarSesion, logout |
| `LoginViewModel` | Formulario de login, mapeo de errores ProblemDetail → ErrorTipo |
| Navegación | Check → Login/Home; LaunchedEffect sobre authState |
| Backup | `allowBackup=false` + `data_extraction_rules.xml` excluye todos los dominios |

## Comandos de desarrollo

```bash
# Compilar debug (requiere SDK local)
cd apps/mobile-android
./gradlew assembleDebug --no-daemon

# Lint
./gradlew lint --no-daemon

# Pruebas JVM (sin dispositivo)
./gradlew test --no-daemon

# Pruebas instrumentadas (requiere dispositivo/emulador)
./gradlew connectedAndroidTest --no-daemon
```

## Seguridad

- El refresh token **nunca** se almacena en texto plano. Se cifra con AES-256-GCM antes de persisitir en DataStore, usando una clave que vive exclusivamente en el Android Keystore del sistema.
- Las credenciales (nombre de usuario y contraseña) **nunca** se almacenan.
- El `identificadorInstalacion` no es sensible: es un UUID público que identifica la instalación, no al usuario.
- El backup de datos está deshabilitado explícitamente (`allowBackup=false`).

## ADRs relacionados

- [ADR-0031](../../docs/adr/0031-identificador-instalacion-login.md) — Por qué el login acepta `identificadorInstalacion` en lugar de `dispositivoId`.
- [ADR-0032](../../docs/adr/0032-stack-android-fase-4a.md) — Decisiones del stack técnico Android (Fase 4A).
- [ADR-0019](../../docs/adr/0019-identificador-instalacion-android.md) — `identificador_instalacion` como UUID de instalación.
- [ADR-0022](../../docs/adr/0022-jwt-rs256-claves.md) — JWT RS256 y gestión de claves RSA.
- [ADR-0023](../../docs/adr/0023-refresh-tokens-opacos-rotacion.md) — Refresh tokens opacos con rotación.
