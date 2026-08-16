# SESSION_HANDOFF — App Móvil Cobranza

**Última actualización:** 2026-08-16 22:35 (sesión actual — estabilización entorno Java + fix visual asignación)
**Rama:** main
**Commit HEAD antes de esta ronda:** 6653752 (fix(android): mantener sesion autenticada offline tras reapertura sin red)

## Ronda — Entorno Java (JDK 17) + fix visual "0 op." + regresión (2026-08-16, tarde)

Alcance: NO se tocó backend/API/WSL. Solo Android/Windows.

### Entorno Java del proyecto — JDK estándar: 17

**Diagnóstico inicial (antes de esta ronda):**

| Componente | Java detectado |
|---|---|
| PowerShell `java` (PATH) | Oracle JDK 8 (`1.8.0_471`) — `C:\Program Files (x86)\Common Files\Oracle\Java\java8path\java.exe` |
| Git Bash `java` (PATH) | Mismo JDK 8 del sistema (falla con "Gradle requires JVM 17 or later") |
| Android Studio JBR (bundled) | OpenJDK 25.0.2 (JetBrains) — `C:\Program Files\Android\Android Studio\jbr` |
| `JAVA_HOME` | No definido |
| `.idea/gradle.xml` (no versionado) | `gradleJvm = #GRADLE_LOCAL_JAVA_HOME` (ya delega a la variable de entorno `JAVA_HOME`, sin necesidad de tocar este archivo) |

**Causa raíz confirmada de los ~43 fallos Robolectric:** ninguno de los `build.gradle.kts` del
proyecto declara `kotlin { jvmToolchain(17) }` — solo fijan `sourceCompatibility`/
`targetCompatibility = VERSION_17` (bytecode target), sin forzar qué JDK ejecuta el *daemon* de
Gradle. Sin `JAVA_HOME` explícito en Windows, Gradle terminaba corriendo bajo el JBR de Android
Studio (JDK 25.0.2). Reproducido y confirmado con evidencia exacta:
`java.lang.IllegalArgumentException: Unsupported class file major version 69` en
`org.objectweb.asm.ClassReader.<init>(ClassReader.java:200)`, invocado desde
`org.robolectric.internal.bytecode.InstrumentingClassWriter.getCommonSuperClass` — el ASM
embebido en Robolectric 4.14.1 no soporta parsear class files de JDK 25 (major version 69) al
instrumentar clases. Detalle completo en `docs/gestion/DEUDA_TECNICA.md` (DT-R07, resuelta).

**Resolución (sin tocar ninguna dependencia):**
- Instalado **Microsoft Build of OpenJDK 17.0.20+8 LTS** vía
  `winget install --id Microsoft.OpenJDK.17` → `C:\Program Files\Microsoft\jdk-17.0.20.8-hotspot`.
  No se desinstaló ni modificó el JDK 8 existente ni el JBR de Android Studio.
- `JAVA_HOME` configurado a nivel de **Usuario de Windows** (`[Environment]::SetEnvironmentVariable`,
  scope `User`) apuntando a esa ruta, y `%JAVA_HOME%\bin` antepuesto al `PATH` de Usuario. Es
  persistente para toda terminal/sesión **nueva** (incluidas futuras sesiones de Claude Code).
- **Importante:** la sesión de shell de *esta* conversación ya estaba iniciada antes del cambio,
  por lo que no heredó la variable automáticamente (limitación del harness, no de la
  configuración — el registro de Windows quedó correcto, verificado). Durante el resto de esta
  sesión se exportó `JAVA_HOME` explícitamente por comando. Una terminal nueva fuera de esta
  sesión ya lo hereda sin pasos adicionales.
- Confirmado con JDK 17: `core:database` pasa de 39/39 fallando a **39/39 OK**;
  `feature:gestion` pasa de 4/27 fallando a **27/27 OK**. Cero cambios de código de producción o
  de dependencias para lograrlo.
- Nota para Android Studio: como `.idea/gradle.xml` ya usaba `#GRADLE_LOCAL_JAVA_HOME`, el IDE
  recogerá el nuevo `JAVA_HOME` de Usuario automáticamente en el próximo *Gradle sync* (puede
  requerir reiniciar Android Studio o invalidar caches si ya estaba abierto durante el cambio).

**Verificación rápida para cualquier sesión futura:**
```powershell
java -version        # debe reportar 17.x
.\gradlew --version  # sección "JVM" debe reportar 17.x
```

### Suite de tests completa (JDK 17) — todos los módulos declarados en `settings.gradle.kts`

| Módulo | Tests | Pasan | Fallan | Skipped |
|---|---|---|---|---|
| `app` | 8 | 8 | 0 | 0 |
| `core:network` | 13 | 13 | 0 | 0 |
| `core:security` | 3 | 3 | 0 | 0 |
| `core:database` | 39 | 39 | 0 | 0 |
| `feature:auth` | 30 | 30 | 0 | 0 |
| `feature:asignacion` | 31 | 31 | 0 | 0 |
| `feature:gestion` | 27 | 27 | 0 | 0 |
| `feature:busqueda` | 22 | 22 | 0 | 0 |
| **Total** | **173** | **173** | **0** | **0** |

`./gradlew lint` → BUILD SUCCESSFUL (todos los módulos). `./gradlew :app:assembleDebug` → BUILD
SUCCESSFUL.

**Instrumented tests:** existe un único test instrumentado en el proyecto,
`core:security:SecureTokenStoreInstrumentedTest` (cifrado/descifrado AES-GCM del refresh token
vía Android Keystore real). No pudo ejecutarse: `testInstrumentationRunner` apunta a una clase
(`androidx.test.runner.AndroidJUnitRunner`) cuyo artefacto no está en el classpath de
`androidTest` del módulo — falla con `ClassNotFoundException` al arrancar la instrumentación, no
relacionado con JDK/Robolectric. Ver DT-011 en `docs/gestion/DEUDA_TECNICA.md`. No se corrigió
(fuera del foco de esta ronda: entorno Java + fix visual).

### Corrección visual — "0 op." partido verticalmente en `AsignacionListScreen`

**Causa raíz:** en `PersonaItem` (composable privado de `AsignacionListScreen.kt`), el `Column`
con el nombre y el RUT no tenía `Modifier.weight()`. En un `Row`, un hijo sin peso mide su ancho
"natural" según su línea más larga ya envuelta; con un nombre de dos líneas (ej. "SARA ANTONIETA
DE LOURDES HERNANDEZ SILVA"), esa línea más larga ocupa casi todo el ancho de la fila, dejando
casi nada de espacio para el `Text` "0 op." siguiente, que termina envolviendo carácter por
carácter en vertical.

**Fix:** `Modifier.weight(1f)` en el `Column` del nombre/RUT — esto hace que Compose mida
primero el `Text` "0 op." (sin peso) con su ancho natural de una sola línea, y solo después le
da el espacio restante al nombre, que puede envolver a tantas líneas como necesite sin invadir
el espacio del contador. Se cambió `Arrangement.SpaceBetween` → `Arrangement.spacedBy(8.dp)` (ya
no hace falta SpaceBetween porque el peso ya empuja el contador al final) y se agregó
`maxLines = 1, softWrap = false` al contador como defensa adicional. No se truncó el nombre
(sin `overflow = Ellipsis`) para no cortar información importante, según lo pedido.

**Archivo:** `apps/mobile-android/feature/asignacion/src/main/java/.../ui/AsignacionListScreen.kt`

**Validación:** 2 nuevas `@Preview` (nombre corto / nombre largo) — no existían previews en el
módulo. No se agregó test Compose automatizado (no existía infraestructura de UI testing en el
módulo; crear una para un fix cosmético habría sido desproporcionado). Verificado visualmente en
`Cobranza_API36_Stable`: las 5 personas de la asignación demo se ven correctamente, incluida
"SARA ANTONIETA DE LOURDES HERNANDEZ SILVA" (2 líneas) con "0 op." en una sola línea alineado a
la derecha. Navegación probada explícitamente hacia una persona de nombre corto y una de nombre
largo, con retorno correcto a la lista en ambos casos.

### Decisión de sesión offline — formalizada

La regla ya implementada en el commit `6653752` (fallo transitorio de red en refresh ⇒ mantener
`Autenticado` con datos offline; 401/403 del servidor ⇒ cerrar sesión) se agregó explícitamente
a **RN-24** en `docs/dominio/REGLAS_NEGOCIO.md` (antes solo estaba implícita en "la pérdida de
red no cierra la sesión"). No se creó un ADR nuevo — ADR-0032 (stack Android) no es el lugar
correcto para una regla de negocio, y RN-24 ya era la sección apropiada.

### Regresión funcional mínima — resultado

App inicia ✅ · Restauración de sesión ✅ (sin volver a pedir login tras el APK actualizado) ·
Home visible ✅ · Mi asignación diaria abre ✅ · 5 personas visibles con el layout corregido ✅ ·
Navegación persona (nombre corto y largo) → volver ✅ (sin corromper la lista) · Sin crashes en
logcat ✅ · Sin ANR ✅.

### Archivos modificados en esta ronda

- `apps/mobile-android/feature/asignacion/src/main/java/cl/zzenner/cobranza/feature/asignacion/ui/AsignacionListScreen.kt`
- `CLAUDE.md` — nota durable sobre JDK 17.
- `docs/dominio/REGLAS_NEGOCIO.md` — precisión en RN-24.
- `docs/gestion/DEUDA_TECNICA.md` — DT-011 (nueva, activa) y DT-R07 (resuelta).
- `docs/gestion/CHANGELOG.md` — entrada de esta ronda.
- `.claude/SESSION_HANDOFF.md` — este archivo.

### No incluir en el commit

- `apps/mobile-android/gradle.properties` — cambio preexistente ajeno a esta tarea, sigue sin
  tocar.

### Siguiente acción exacta

`feature:gestion` queda técnicamente listo (173/173 tests OK a nivel de proyecto, entorno Java
estabilizado) para iniciar su ronda de pruebas funcionales — pendiente de que el usuario la
autorice explícitamente. Pendientes menores no bloqueantes: DT-011 (instrumented test runner) y
evaluar detección proactiva de conectividad en `feature:asignacion` (ya registrado en la ronda
anterior).

---

## Ronda de validación — Asignación diaria del ejecutivo (Android)

Alcance: login → Home → Mi asignación diaria → descarga/consulta → persistencia local →
reapertura → comportamiento offline → recuperación. Sin tocar registro de gestiones,
fotografías, compromisos de pago, ni sincronización de gestiones (fuera de alcance).

### Flujo técnico confirmado

`HomeScreen` dispara `LaunchedEffect(Unit) { viewModel.iniciarSincronizacion() }` en cada
composición (incluida la reapertura) → `AsignacionSyncScheduler.programarInmediato()`
(WorkManager, `ExistingWorkPolicy.KEEP`) → `DescargaAsignacionWorker` → `GET
/api/v1/asignaciones/diaria/activa` → `AsignacionRepository.descargarAsignacion()` (Mutex
single-flight) → `BundleReplacementTransaction.reemplazar()` (Room: DELETE total + INSERT
completo, transacción atómica) → `AsignacionViewModel` combina flujos Room → Compose
(`AsignacionListScreen`). La pantalla de asignación solo lee Room; no dispara red por sí
misma.

### Bug encontrado y corregido — sesión no persistía offline tras reapertura

**Severidad:** Alta (rompe el caso de uso principal de MVP: trabajo offline con datos ya
descargados).

**Síntoma:** Con la API inaccesible, al hacer force-stop y reabrir la app, el usuario
quedaba deslogueado (pantalla de login) en vez de ver Home con la asignación ya
descargada localmente. La sesión local (`sessionExpiresAt`) seguía vigente y el refresh
token seguía almacenado.

**Causa raíz:** `SessionRepository.verificarSesionInicial()`
(`feature/auth/src/main/.../data/SessionRepository.kt`) evaluaba
`else if (accessTokenInMemory == null)` para decidir si mostrar `NoAutenticado` tras un
`refreshTokens()` fallido. Como el access token **nunca se persiste** (vive solo en
memoria, por diseño), esa condición era `true` en *todo* arranque en frío, sin importar si
el fallo de `refreshTokens()` fue por red (transitorio, sesión aún válida) o por 401/403
(sesión realmente inválida). El propio comentario del código ya advertía "Error de red —
sesión potencialmente válida aún; no limpiar", pero el flujo posterior no respetaba esa
intención.

**Evidencia:** logcat muestra `POST /api/v1/auth/refresh` → `ConnectException` → la app
navega a `NoAutenticado` a pesar de que `SecureTokenStore` conservaba el refresh token
(`clearRefreshToken()` nunca se invocó).

**Corrección:** `verificarSesionInicial()` ahora distingue si el refresh token sigue
presente tras el intento de renovación (`secureTokenStore.getRefreshToken() == null`
⇒ la sesión fue invalidada por el servidor dentro de `refreshTokens()`, p. ej. 401/403).
Si el refresh token sigue presente, el fallo fue transitorio (red/servidor caído) y se
mantiene `AuthState.Autenticado(nombreUsuario)` con los datos ya cacheados localmente.

**Archivo:** `apps/mobile-android/feature/auth/src/main/java/cl/zzenner/cobranza/feature/auth/data/SessionRepository.kt`

**Pruebas agregadas** en `SessionRepositoryTest.kt`:
- `sesion vigente con error de red en refresh mantiene Autenticado con datos cacheados`
- `sesion vigente con refresh 401 durante verificacion inicial cae a NoAutenticado` (evita
  regresión: un 401/403 real sigue deslogueando correctamente)

Verificado end-to-end tras el fix: reinstalado el APK, con la API caída la app reabre
directo en Home y "Mi Asignación" muestra las 5 personas cacheadas sin conexión, sin
crash, sin ANR (WorkManager reintenta la descarga en segundo plano con `Result.retry()`).

### Otros hallazgos (no bloqueantes)

- **Bug visual menor (no corregido, fuera de alcance de esta corrección puntual):** en la
  lista de "Mi Asignación", cuando el nombre de una persona ocupa dos líneas (ej. "SARA
  ANTONIETA DE LOURDES HERNANDEZ SILVA"), la etiqueta "0 op." a la derecha se parte
  carácter por carácter en vertical ("0" / "o" / "p" / ".") en vez de mantenerse en una
  línea. Cosmético, no afecta datos ni funcionalidad. Reproducible en
  `AsignacionListScreen` (feature:asignacion). Pendiente de layout fix (probablemente
  `Row` sin `Modifier.weight`/`maxLines` adecuado en el ítem de lista).
- El mapeo `ConnectException → ErrorTipo.ERROR_SERVIDOR` ("Error en el servidor. Intente
  más tarde.") en `LoginViewModel` es intencional y correcto para el escenario de API
  caída con red del dispositivo disponible (distinto de `SIN_CONEXION`, reservado para
  falta de red física). No es un bug.
- La API `/api/v1/asignaciones/diaria/activa` no expone campo `cartera`; la UI
  correctamente no lo muestra. Confirmado contra OpenAPI.
- No existe detección activa de conectividad en `feature:asignacion` (`AuthState`/
  `AsignacionUiState.isOnline` nunca se asigna); el modo offline se infiere solo de forma
  reactiva ante fallos de red. No es un defecto para el alcance de esta ronda, pero es una
  limitación a tener presente para UX futura (ej. banner "sin conexión" persistente).

### Datos de prueba — nota operativa (no código)

El usuario semilla `ej_demo_133` tenía una contraseña desincronizada respecto a
`DEV_ADMIN_PASSWORD` (`.env`) porque el seed usa "crear si no existe" y no resetea
contraseñas de usuarios ya existentes en el volumen Postgres local. Se actualizó
`contrasena_hash` de `ej_demo_133` directamente en la BD local de desarrollo (vía
`pgcrypto`, instalado como extensión en `cobranza_db`) para que coincida con
`DEV_ADMIN_PASSWORD=123456` del `.env` actual. **Cambio de datos, no de código ni
esquema versionado; no requiere migración Flyway.** La asignación diaria de
`ej_demo_133` se regenera automáticamente cada día (confirmado: existían PUBLICADAS para
2026-08-13 a 2026-08-16, 5 personas, cartera "Temprana" en las cuatro).

### Consistencia de datos verificada

Comparación BD Postgres (fuente de verdad) vs Room local vs UI, para la asignación
`8b9fd562-ac5f-4773-a109-d1e79d1735ac` (2026-08-16, PUBLICADA, ej_demo_133): 5/5 personas
coinciden exactamente (RUT y nombre) en las tres capas, en cuatro ciclos de
descarga/reapertura distintos, sin duplicados (`asignacion_diaria`: 1 fila,
`asignacion_persona`: 5 filas, `persona`: 5 filas, siempre).

## Estado del entorno DEV

- Docker: API + PostgreSQL + Admin Web corriendo (`cobranza-api-1`, `cobranza_postgres`,
  `cobranza-admin-web-1`), todos healthy al cierre de la sesión.
- AVD usado: `Cobranza_API36_Stable` (correcto; NO usar `Medium_Phone`, ver DT-010).

## Cambios en esta sesión (pendientes de commit)

### Archivos modificados
- `apps/mobile-android/feature/auth/src/main/java/cl/zzenner/cobranza/feature/auth/data/SessionRepository.kt`
  — fix del bug de sesión offline descrito arriba.
- `apps/mobile-android/feature/auth/src/test/java/cl/zzenner/cobranza/feature/auth/SessionRepositoryTest.kt`
  — 2 pruebas nuevas de regresión.
- `.claude/SESSION_HANDOFF.md` — este archivo.

### No incluir en el commit
- `apps/mobile-android/gradle.properties` — cambio preexistente ajeno a esta tarea
  (`org.gradle.tooling.parallel=true`), ya señalado en la sesión anterior.

## Resultados de validación

- `feature:auth:testDebugUnitTest`: 18/18 OK (incluye las 2 pruebas nuevas).
- `feature:asignacion:testDebugUnitTest`: 31/31 OK (AsignacionMapperTest,
  AsignacionRepositoryTest, AsignacionViewModelTest, BigDecimalSerializerTest,
  DescargaAsignacionWorkerTest).
- `app:assembleDebug`: BUILD SUCCESSFUL.
- `core:database:testDebugUnitTest` y `feature:gestion:testDebugUnitTest`: fallan
  íntegramente por el problema conocido JDK25/Robolectric/ASM
  (`java.lang.IllegalArgumentException` en `ClassReader.java:200`), no relacionado con
  los cambios de esta sesión. No se intentó corregir (fuera de alcance, según
  instrucciones vigentes).
- Matriz completa de 17 escenarios manuales ejecutada y documentada en el informe de la
  ronda (login, apertura de asignación, consistencia API/UI, listado, duplicados,
  navegación, force-stop, persistencia, offline con datos, offline sin datos,
  recuperación, ausencia de crashes/ANR).

## Incidencias API para WSL

Ninguna. Todo el trabajo de esta ronda fue Android-only.

## Siguiente acción exacta

1. Revisar y, si se aprueba, corregir el bug visual menor de "0 op." partido
   verticalmente en `AsignacionListScreen` (feature:asignacion) — bajo impacto, no
   bloqueante.
2. Evaluar si conviene agregar detección activa de conectividad
   (`ConnectivityManager`/`NetworkCallback`) para mostrar un indicador proactivo de modo
   offline en `feature:asignacion`, en vez de inferirlo solo reactivamente.
3. Retomar Fase 7 (Despliegue en VPS) según lo indicado en `TASK_CURRENT.md`, o continuar
   con el siguiente bloque funcional de Android (registro de gestiones) — pendiente de
   decisión del usuario.
