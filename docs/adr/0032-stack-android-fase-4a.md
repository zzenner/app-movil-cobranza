# ADR-0032 — Stack técnico Android (Fase 4A)

**Estado:** Aceptado — 2026-08-02
**Contexto:** Fase 4A — Base Android

---

## Contexto

La Fase 4A crea el proyecto Android desde cero. Se deben elegir las versiones exactas del toolchain, las bibliotecas de red, almacenamiento seguro, inyección de dependencias y UI, de forma que sean estables y producción-ready al momento de implementación (agosto 2026).

## Decisiones

### Toolchain

| Componente | Versión | Justificación |
|---|---|---|
| AGP (Android Gradle Plugin) | 9.3.0 | Estable agosto 2026; compatible con Gradle 9.6.x |
| Gradle | 9.6.1 | Versión mínima requerida por AGP 9.3 |
| Kotlin | 2.4.10 | Compatible con KSP 2.3.10 y Compose Compiler embebido |
| KSP | 2.3.10 | Generación de código para Hilt; reemplaza KAPT |
| `compileSdk` | 37 (Android 17) | Requerido por lifecycle 2.11.0 y hilt-navigation-compose 1.4.0 |
| `targetSdk` | 36 (Android 16) | Pruebas instrumentadas no ejecutadas en Android 17; evita activar cambios de comportamiento no verificados |
| `minSdk` | 29 (Android 10) | Decisión provisional ADR-0011; cubre >90% de dispositivos corporativos estimados |

### UI

| Componente | Versión | Justificación |
|---|---|---|
| Jetpack Compose BOM | 2026.06.01 | BOM estable de junio 2026; gestiona versiones de Material3, Foundation, Runtime |
| Material3 | (via BOM) | Componentes de diseño actualizados |
| Navigation Compose | 2.9.8 | Navegación tipo-safe para Compose; compatible con Hilt |

### Inyección de dependencias

| Componente | Versión | Justificación |
|---|---|---|
| Hilt Android | 2.60.1 | Estándar de Google para Android DI |
| Hilt AndroidX (ViewModel, Navigation) | 1.4.0 | Integración Navigation Compose con `hiltViewModel()` |

`SessionRepository` usa `@ActivityRetainedScoped` para sobrevivir cambios de configuración sin necesitar `ViewModel` directo en el repositorio.

### Red

| Componente | Versión | Justificación |
|---|---|---|
| Retrofit | 3.0.0 | Cliente REST idiomático con soporte Kotlin Coroutines |
| OkHttp BOM | 5.4.0 | Versión estable; reemplaza OkHttp 4.x |
| kotlinx.serialization JSON | 1.11.0 | Serializador nativo Kotlin; evita Gson/Moshi |
| kotlinx.coroutines Android | 1.11.0 | Dispatcher Main para coroutines en Android |

**Dos clientes HTTP:**
- `@Named("public")`: sin autenticación; para login, refresh y logout.
- `@Named("authenticated")`: con interceptor Bearer + `SingleFlightAuthenticator`.

**Single-flight refresh:** `SingleFlightAuthenticator` implementa `OkHttp Authenticator` con `kotlinx.coroutines.sync.Mutex`. Si dos solicitudes paralelas reciben 401, solo una llama al endpoint `/refresh`; la otra espera y reutiliza el token renovado.

### Almacenamiento seguro

| Componente | Versión | Justificación |
|---|---|---|
| Preferences DataStore | 1.2.1 | Almacenamiento no sensible: `identificadorInstalacion`, `sessionExpiresAt` |
| Android Keystore (API pública) | — | AES-256-GCM/NoPadding; IV único por operación; sin EncryptedSharedPreferences |

El refresh token se cifra con Android Keystore (AES-256-GCM) antes de persitirse en DataStore. La clave permanece en el Keystore del sistema operativo y nunca sale en texto plano.

`identificadorInstalacion` no se cifra — no es dato sensible y debe ser legible para incluirlo en el body del login.

### Backup

`android:allowBackup="false"` en `AndroidManifest.xml` (cubre API 22+). `data_extraction_rules.xml` excluye explícitamente todos los dominios de cloud-backup y device-transfer (API 31+). Esto garantiza que ni el refresh token cifrado ni el `identificadorInstalacion` se copian a la nube de Google.

## Consecuencias

- El proyecto es multi-módulo desde el inicio: `:app`, `:core:network`, `:core:security`, `:feature:auth`.
- Fases futuras añadirán módulos (`:core:database` con Room, `:feature:cartera`, `:feature:gestiones`) sin afectar la base establecida.
- `TokenProvider` es una interfaz en `:core:network` que `SessionRepository` (en `:feature:auth`) implementa. Esto invierte la dependencia y evita un ciclo: `core:network` no depende de `feature:auth`.

## Alternativas descartadas

- **KAPT en lugar de KSP:** KAPT es más lento y no compatible con el modelo de compilación de Kotlin 2.x a largo plazo.
- **EncryptedSharedPreferences:** marcado como deprecated en API 33+; la API pública del Keystore con AES-GCM es más portable y explícita.
- **Gson en lugar de kotlinx.serialization:** requiere reflexión en tiempo de ejecución; kotlinx.serialization opera en tiempo de compilación, compatible con R8/ProGuard sin configuración adicional.
- **Mono-módulo:** la separación desde el inicio reduce el acoplamiento y facilita las pruebas unitarias sin framework Android.
