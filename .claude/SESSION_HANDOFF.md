# SESSION_HANDOFF — App Móvil Cobranza

**Última actualización:** 2026-08-20 03:00 (sesión actual — DT-014: identificar persona al registrar gestión)
**Rama:** main
**Commit HEAD antes de esta ronda:** c07979d (docs: validar funcionalmente Busqueda de persona por RUT)

## Ronda — DT-014: identificar persona al registrar gestión (2026-08-20)

Alcance: solo `feature:gestion` (`GestionFormViewModel.kt`, `GestionFormScreen.kt`) + su test.
DT-013 no se tocó. NO se tocó backend/API/WSL.

### Causa raíz

`GestionFormViewModel.init` ya leía nombre/RUT desde Room (`PersonaDao.getPersonaConDetalle()`
para origen `ASIGNACION_DIARIA`, `PersonaDirectaDao.findById()` para `BUSQUEDA_DIRECTA`), pero el
resultado se guardaba en variables privadas (`personaRutNumero`, `personaRutDv`, `personaNombre`)
usadas solo al construir el `GestionForm` en `guardar()` — nunca expuestas al `StateFlow` que
observa `GestionFormScreen`. La pantalla no tenía forma de mostrar quién era la persona porque el
dato nunca llegaba a la capa de Compose, no porque faltara consultarlo. **No se necesitó ninguna
llamada de red nueva.**

### Solución

- Nuevo `PersonaIdentidadState` (`Cargando`/`Disponible(nombre, rutNumero, rutDv)`/`NoDisponible`)
  en `GestionFormViewModel.kt`, expuesto como `GestionFormState.identidad`. Las variables privadas
  se eliminaron; `guardar()` ahora exige `Disponible` (si es `NoDisponible`, bloquea con
  `errorGeneral` en vez de guardar con datos vacíos).
- `GestionFormScreen` — nueva card "Persona" (nombre + `RUT: 12.345.678-9` formateado) como primer
  elemento del formulario, antes de "Tipo de gestión". Solo nombre y RUT — sin dirección,
  operaciones ni teléfonos. RUT formateado con una función local (mismo patrón que
  `feature:asignacion/domain/formatearRut`, reimplementada en el propio archivo para no introducir
  una dependencia de `feature:gestion` hacia `feature:asignacion` solo por 3 líneas de formato).
  Los tres estados de identidad mantienen la misma estructura de Card (evita parpadeo de layout).

### Verificación funcional (emulador, `Cobranza_API36_Stable`, API real)

- **Camino A (asignación diaria):** Home → Mi asignación → Cristian Marcelo Agusto González
  (14.503.973-8) → Registrar gestión. Card muestra "CRISTIAN MARCELO AGUSTO GONZALEZ / RUT:
  14.503.973-8" — coincide exactamente con `PersonaDetalleScreen`. Gestión `COMPROMISO_PAGO`
  guardada con éxito (validación de fecha de compromiso funcionando sin cambios), visible en
  "Gestiones en este dispositivo" de esa misma persona tras volver — asociación correcta
  confirmada.
- **Camino B (búsqueda directa):** Home → Buscar persona por RUT → 12.520.996-3 (Mauricio Antonio
  Verdugo Rebolledo, **fuera** de la asignación diaria de `ej_demo_133`) → Registrar gestión. Card
  muestra la identidad correcta. Búsqueda posterior de una persona distinta (14.503.973-8) desde
  la misma pantalla de búsqueda confirmó que la card **no arrastra** la persona anterior — muestra
  "CRISTIAN MARCELO AGUSTO GONZALEZ" de inmediato, sin residuo de Mauricio.
- Sin crash, sin ANR, sin parpadeo visible de layout en ningún tránsito.

### Privacidad

Logcat revisado durante toda la verificación manual (búsquedas, navegación, guardado real de una
gestión): **ningún RUT ni nombre aparece en ningún log**, ni antes ni después del cambio — la
identidad solo vive en el `StateFlow`/Compose, nunca se pasa a `Log.*` ni a analytics.

### Tests

`GestionFormViewModelTest` +3 casos: identidad disponible vía asignación diaria (Flow de
`PersonaDao`), identidad disponible vía búsqueda directa (ya cubierto implícitamente por los tests
existentes, ahora con aserción explícita), y persona no disponible (`personaDirectaDao.findById`
retorna `null`) bloqueando `guardar()` sin invocar `repository.guardarLocal()`. Sin infraestructura
de test de Compose nueva — el proyecto no tenía ninguna (único test instrumentado existente es
`SecureTokenStoreInstrumentedTest`, no relacionado, ver DT-011), y el cambio de UI es
suficientemente simple para no justificarla.

Suite completa: `testDebugUnitTest` + `lint` + `:app:assembleDebug` → BUILD SUCCESSFUL, **192/192
tests, 0 fallos** (189 baseline + 3 nuevos; `feature:gestion` pasó de 30/30 a 33/33). Lint: 0
issues en los 8 módulos.

### No incluir en el commit

- `apps/mobile-android/gradle.properties` — cambio preexistente ajeno, sigue sin tocar.

### DT-013

Sin cambios. Permanece diferida y documentada.

### DT-014

Resuelta — ver **DT-R09** en `docs/gestion/DEUDA_TECNICA.md`.

---

## Ronda — Búsqueda de persona por RUT: validación funcional completa (2026-08-19)

Alcance: solo `feature:busqueda` + verificación de su integración (`PersonaBusquedaApi`,
`NetworkModule`, navegación, `PersonaDirectaDao`). DT-013 no se tocó. NO se tocó backend/API/WSL
(solo lectura de código en `apps/api` para confirmar la regla de autorización real).
**No se encontraron bugs.** Toda la validación fue de lectura/observación; no hubo cambios de
código ni de tests en esta ronda.

### Flujo técnico confirmado

`HomeScreen` → botón "Buscar persona por RUT" → `RUTA_BUSQUEDA_DIRECTA` → `BusquedaDirectaScreen`
(feature:busqueda). `BusquedaDirectaViewModel.buscar()` primero valida localmente con
`RutValidator.esValido()` (módulo 11 estándar, sin llamar a la red si el RUT es inválido) y luego
llama a `BusquedaDirectaRepository.buscar()` → `PersonaBusquedaApi.buscarPersona()`
(`POST api/v1/personas/busquedas`, RUT en el body — ADR-0041). Si la API responde 200, el
resultado se persiste como snapshot en Room `persona_directa` (`PersonaDirectaDao.upsert`,
ADR-0042) y la UI navega de inmediato a `GestionFormScreen` (sin pantalla de detalle intermedia).
Confirmado en `NetworkModule.kt` que `PersonaBusquedaApi` se construye desde el Retrofit
`@Named("authenticated")` — mismo `SingleFlightAuthenticator` y mismo interceptor de token que
`GestionApi`/`SincronizacionApi`; no existe lógica de tokens propia en `feature:busqueda`.

**Este flujo es 100% online — no consulta Room antes de llamar a la API, ni para personas de la
asignación del día.** RN-18 distingue explícitamente "búsqueda local" (filtro en la lista de
asignación descargada, vive en `feature:asignacion`, no alcanzable desde este botón de Home) de
"búsqueda global" (este flujo, siempre requiere red). Confirmado leyendo
`BusquedaDirectaRepository.buscar()` línea por línea: no hay ninguna consulta a
`PersonaDao`/`AsignacionDiariaDao` antes del `api.buscarPersona()`.

### Regla de autorización encontrada (punto crítico del encargo)

**Un ejecutivo puede buscar y acceder a CUALQUIER persona del sistema, no solo a las de su
asignación diaria — comportamiento intencional y documentado, no un bug de seguridad.**
Confirmado en tres niveles:
1. **RN-18** (`docs/dominio/REGLAS_NEGOCIO.md`): "Búsqueda global: `POST
   /api/v1/personas/busquedas`... Rol requerido: `EJECUTIVO_TERRENO`" — sin mención de scoping por
   asignación.
2. **Código backend** (`BusquedaPersonaService.buscar()`, leído completo): `ejecutivoId` se usa
   **únicamente** para el log de auditoría `[BUSQUEDA_AUDITORIA]`, nunca para filtrar o restringir
   el resultado de `personaConsultaApi.findByRut()`. `BusquedaPersonaController` solo exige
   `@PreAuthorize("hasRole('EJECUTIVO_TERRENO')")`, sin chequeo adicional de pertenencia a cartera
   o asignación.
3. **Prueba real en el emulador**: se buscó el RUT 12520996-3 (Mauricio Antonio Verdugo
   Rebolledo), confirmado por consulta SQL directa que **nunca** ha estado en ninguna asignación
   diaria de `ej_demo_133` — la búsqueda lo encontró y navegó normalmente a "Registrar gestión".
No se clasifica como incidencia de seguridad: es la razón de ser de `BUSQUEDA_DIRECTA` como origen
de gestión distinto de `ASIGNACION_DIARIA` (permitir gestionar a alguien fuera de la asignación
del día, ej. un aval o referencia que aparece en terreno).

### Validación de RUT

`RutValidator.esValido()` (feature:busqueda): número y DV en campos separados (coherente con
ADR-0007/DT-R01), módulo 11 estándar, rechaza vacío, no-dígitos, número >8 dígitos, DV que no sea
0-9/K. La validación es **puramente local** — nunca llega a la API si el RUT es inválido (ahorra
tráfico). `BusquedaDirectaViewModel` filtra la entrada en tiempo real: campo Número solo acepta
dígitos (los puntos de formato chileno, ej. "14.503.973", se eliminan automáticamente al tipear,
quedando "14503973" — tolerancia silenciosa ya implementada, no se agregó ninguna nueva), campo DV
acepta 1 carácter y lo fuerza a mayúscula. DV incorrecto (probado: 14503973-**5** en vez de
14503973-**8**) se rechaza localmente con "El RUT ingresado no es válido..." sin llamar a la API —
confirmado con logcat limpio (cero requests HTTP en ese intento).

### Comportamiento online (verificado en emulador, `Cobranza_API36_Stable`, API real)

- **Formulario vacío:** botón "Buscar" deshabilitado (`enabled = !buscando && rutNumero.isNotBlank
  () && rutDv.isNotBlank()`) — imposible disparar una búsqueda vacía.
- **RUT de persona en la asignación del día** (14503973-8, Cristian Marcelo Agusto González):
  encontrado, navega a "Registrar gestión", se persiste en `persona_directa` (verificado por
  extracción binaria de Room — ver nota WAL abajo).
- **RUT de persona existente pero fuera de la asignación** (12520996-3): encontrado y accesible —
  ver sección de autorización arriba.
- **RUT inexistente** (11111111-1, formato y DV válidos, confirmado ausente en Postgres): mensaje
  "No se encontró ninguna persona con el RUT ingresado.", sin crash, sin navegación, sin datos
  inventados, permanece en la pantalla de búsqueda.
- **DV incorrecto:** ver sección de validación de RUT arriba.
- **Búsquedas consecutivas** (persona A → inexistente → persona B → DV incorrecto → persona A de
  nuevo): cada resultado reemplaza limpiamente al anterior, sin residuales, sin mezclar estados de
  error/éxito, sin duplicar la fila en Room (`persona_directa` terminó con exactamente 2 filas tras
  3 búsquedas repetidas de la misma persona — upsert por `id` del servidor funciona correctamente).
- **Doble/triple-tap sobre "Buscar"** (3 taps rápidos consecutivos): solo se disparó **1** request
  HTTP real (confirmado contando líneas `--> POST`/`<-- 200` en Logcat con el tag `OkHttp`) — el
  guard síncrono `if (s.buscando) return` en `BusquedaDirectaViewModel.buscar()` previene
  correctamente la condición de carrera antes de que la corrutina ceda el hilo.
- **Navegación a "Registrar gestión" y vuelta atrás:** confirma persona correcta implícitamente
  (mismo RUT recién buscado), back del sistema regresa limpio a la pantalla de búsqueda (con los
  últimos valores de número/DV aún visibles — el ViewModel no limpia esos campos, solo
  `personaEncontradaId`; no está mal, RN-18 no exige limpiar ni preservar, y la tarea explícitamente
  no lo exige tampoco), nueva búsqueda funciona sin arrastrar estado previo.
- **Force-stop + reapertura:** sesión sobrevive, Home visible, "Buscar persona por RUT" abre en
  estado limpio (formulario vacío, sin mensaje de error residual) — no hay corrupción de estado.
- **Recuperación de conectividad:** con la API caída, la búsqueda de una persona no descargada
  (12520996-3) devolvió correctamente "Sin conexión. Verifique su red e intente nuevamente." Tras
  reiniciar el contenedor (`docker start cobranza-api-1`, sin reinstalar la app ni reiniciar el
  emulador) y confirmar `/actuator/health` UP, **repetir el mismo tap** sobre "Buscar" completó la
  búsqueda con éxito (200 real, confirmado en Logcat) — recuperación limpia.
- **401/renovación de sesión:** no se re-probó end-to-end en esta ronda (el mecanismo es idéntico
  al ya validado para `GestionApi` en la ronda anterior — mismo cliente `@Named("authenticated")`,
  mismo `SingleFlightAuthenticator`, confirmado por lectura de código en `NetworkModule.kt`); no se
  fabricó un 401 real para no introducir lógica de tokens ni tocar backend solo para esta prueba.

### Comportamiento offline (con API caída vía `docker stop cobranza-api-1`)

**No hay resolución local para ningún caso** — ni para una persona que sí está en la asignación
del día ya descargada en Room. Esto es el comportamiento **correcto según el diseño actual**, no
un bug: `BusquedaDirectaRepository.buscar()` no consulta Room antes de llamar a la API (ver "Flujo
técnico" arriba); la búsqueda local real (offline, sobre la asignación ya descargada) es una
funcionalidad distinta que vive en `feature:asignacion`, no alcanzable desde este botón de Home.
Ambos casos probados (persona en asignación local: 14503973-8; persona no descargada: 12520996-3)
devolvieron el mismo mensaje "Sin conexión..." — sin crash, sin ANR, sin spinner infinito, sin
inventar datos.

### Errores API mapeados en código (no todos re-probados con fallas reales para no tocar backend)

- 404 → `NoEncontrada` (probado real).
- 400 con cuerpo conteniendo `RUT_INVALIDO` → `RutInvalido` (mapeo verificado por lectura de
  código; el caso real de RUT inválido nunca llega a la API porque se filtra antes en el cliente,
  así que este branch del repository cubre el caso de que la validación local y la del servidor
  llegaran a divergir — no reproducido, cubierto por diseño defensivo).
- Otro código HTTP no 200/404/400 → `Error("Error del servidor: <code>")` (no reproducido; no se
  fabricó un 500 real para no modificar backend).
- `IOException` (API caída, timeout de red) → `SinConexion` (probado real).
- Cualquier otra excepción → `Error(e.message)` (no reproducido).

### Privacidad y Logcat

Revisado con logcat real durante toda la sesión de pruebas: **ningún RUT completo, nombre
completo ni token Bearer aparece en Logcat.** `feature:busqueda` no tiene ninguna llamada a
`Log.*`. El único logging de red (`HttpLoggingInterceptor`, `NetworkModule.kt`) está configurado
en `Level.HEADERS` (no `BODY`), así que el cuerpo de la solicitud/respuesta (que contiene RUT y
nombre) nunca se imprime. Confirmado además que el header `Authorization` tampoco aparece en el
log (verificado con un request real y logcat limpio): el interceptor de logging se agrega al
`OkHttpClient` público antes de que el interceptor de token se añada en el cliente autenticado
(`publicClient.newBuilder().addInterceptor { token }`), por lo que el logging captura la solicitud
antes de que el header `Authorization` se inserte. No se requirió ningún cambio.

### Datos de Room verificados (extracción binaria, ver nota WAL)

`persona_directa` tras la sesión de pruebas: exactamente 2 filas (14503973-8 Cristian Marcelo
Agusto González; 12520996-3 Mauricio Antonio Verdugo Rebolledo), sin duplicados pese a múltiples
búsquedas repetidas de la misma persona — `upsert` por `id` (UUID del servidor) funciona
correctamente como llave de idempotencia.

**Nota operativa para futuras sesiones:** Room usa journal mode WAL. Para leer datos recién
escritos con `run-as cat` + `sqlite3` local, es indispensable copiar también `cobranza.db-wal` y
`cobranza.db-shm` junto a `cobranza.db` (mismo directorio, mismo prefijo) — copiar solo el `.db`
principal devuelve datos desactualizados o tablas vacías si hay escrituras recientes sin
checkpoint. Ruta: `/data/data/cl.zzenner.cobranza/databases/`. Usar el prefijo `//` en las rutas
remotas de `adb exec-out run-as ... cat` para evitar el mangling de MSYS/Git Bash en Windows.

### Deuda técnica registrada

**DT-014** (nueva, ver `docs/gestion/DEUDA_TECNICA.md`): `GestionFormScreen` no muestra nombre ni
RUT de la persona al registrar una gestión (afecta ambos orígenes, no exclusivo de búsqueda
directa). Impacto bajo (omisión de UI, no error de datos), decisión recomendada documentada.

### Anomalía investigada y descartada (no es un bug)

Al iniciar esta ronda, una consulta SQL directa con `CURRENT_DATE` (zona horaria de la sesión de
Postgres, efectivamente UTC) devolvía 0 filas para la asignación de hoy de `ej_demo_133`. Era un
falso negativo de la consulta, no un problema real: la API resuelve "hoy" en **America/Santiago**
(`SincronizacionApi.kt`, comentario explícito), y a esa hora (03:06 UTC del 19-ago) el día vigente
en Santiago aún era el 18-ago — la asignación PUBLICADA de esa fecha existe y la API la devuelve
correctamente (HTTP 200, confirmado con login real + llamada directa al endpoint). No se modificó
nada; solo se corrigió el criterio de búsqueda de datos de prueba para esta ronda.

### Tests

Sin cambios de código → sin tests nuevos. Suite completa ejecutada al final:
`testDebugUnitTest` + `lint` + `:app:assembleDebug` → BUILD SUCCESSFUL, **189/189 tests, 0
fallos** (feature:busqueda: 22/22, igual al baseline reportado al inicio de la ronda). Ver tabla
completa en el informe final de la tarea.

### No incluir en el commit

- `apps/mobile-android/gradle.properties` — cambio preexistente ajeno, sigue sin tocar.

### Incidencias API para WSL

Ninguna. No se encontró ningún comportamiento del backend que requiera corrección — la regla de
autorización "cualquier persona, cualquier ejecutivo" es la documentada en RN-18, no un defecto.

---

## Ronda — DT-012: logout no queda atrapado por gestiones en error permanente (2026-08-19)

Alcance: solo DT-012. DT-013 (sin sincronización proactiva por reconexión) permanece diferida,
sin cambios. NO se tocó backend/API/WSL.

### Causa raíz

`HomeViewModel.solicitarLogout()`/`sincronizarYLogout()` usaban
`gestionRepository.contarNoResueltas()` (cuenta cualquier estado != `SINCRONIZADA`) para decidir
si bloquear el logout. Esto trata igual una gestión que el worker todavía puede reenviar solo
(`PENDIENTE_ENVIO`/`ERROR_REINTENTABLE`) que una que el worker **nunca** vuelve a seleccionar
(`ERROR_PERMANENTE`/`CONFLICTO` — excluidas explícitamente de `GestionLocalDao.getElegibles()`).
Sin esa distinción, una sola gestión rechazada por validación del servidor dejaba al ejecutivo
sin ninguna vía de salida desde la UI — reproducido y documentado como DT-012 en la ronda
anterior con evidencia real (Tasty Maule Spa, HTTP 404 antes del fix de endpoint).

**Hallazgo adicional durante esta corrección:** `BundleReplacementTransaction.limpiarTodo()`
borraba `gestion_local` sin ningún filtro — un test existente (`LogoutIntegrationTest`)
documentaba literalmente ese borrado incondicional, incluyendo gestiones `PENDIENTE_ENVIO`. Esto
nunca ocurría en producción porque el logout jamás se alcanzaba con pendientes reales, pero de
haber implementado el fix de DT-012 sin corregir esto, la primera vez que un usuario saliera con
una gestión permanente sin resolver, esa **única copia local se habría perdido** — justo lo que
RN-24 prohíbe. Se corrigió como parte necesaria del mismo cambio, antes de tocar `HomeViewModel`.

### Regla final de logout (ver RN-24 actualizada)

| Estado | Comportamiento |
|---|---|
| Sin pendientes | Logout inmediato, sin diálogo. |
| `PENDIENTE_ENVIO`/`ERROR_REINTENTABLE` | Sin cambios: bloquea, ofrece "Sincronizar y cerrar sesión". |
| `ERROR_PERMANENTE`/`CONFLICTO` (y ya no quedan reintentables) | Diálogo nuevo en lenguaje simple; requiere confirmación explícita ("Cerrar sesión de todas formas") o cancelar. |
| Combinación (reintentable + permanente) | Se resuelve primero la reintentable (comportamiento sin cambios); el diálogo de "confirmar salida" solo aparece cuando el conteo de reintentables ya es 0. |

### Retención de datos (crítico)

Tras el logout: `gestion_local` **solo pierde las filas ya `SINCRONIZADA`**
(`GestionLocalDao.deleteSincronizadas()`). Cualquier gestión pendiente, en reintento o con error
permanente sobrevive íntegra (id, contenido, `codigoErrorServidor`, `mensajeError`) — verificado
end-to-end en el emulador. El resto de las tablas (`asignacion_diaria`, `persona`,
`gestion_historica`, `operacion`, `persona_directa`, `sync_metadata`) sí se limpia por completo,
por ser caché de solo lectura que se vuelve a descargar en el próximo login.

### Cambio UX

Nuevo diálogo (`HomeScreen`, estado `EstadoLogout.ConfirmarLogoutConNoRecuperables`): "Hay
gestiones que no se pudieron enviar. Tiene N gestión(es) que el sistema no podrá enviar de forma
automática. Quedarán guardadas en este dispositivo, pero no se enviarán solas: avise a su
supervisor para resolverlas. Cerrar sesión no significa que esos datos ya fueron enviados." Sin
mencionar `ERROR_PERMANENTE` ni otros tecnicismos. Botones: "Cerrar sesión de todas formas" /
"Cancelar".

### Verificación funcional (emulador, `Cobranza_API36_Stable`)

Logout normal (0 pendientes): sin diálogo, directo a login — sin regresión. Gestión forzada a
`ERROR_PERMANENTE` mediante manipulación controlada de Room (sin tocar backend, ya que
reproducir un 422/409 real requeriría cambios de API fuera de alcance Windows): diálogo correcto
→ confirmación → logout exitoso → gestión persiste en Room tras el logout con su error intacto →
relogin sin crash ni ANR.

### Deuda derivada NO resuelta (fuera de alcance de esta tarea)

`gestion_local` no tiene columna `ejecutivoId`. Si un ejecutivo distinto llega a loguearse en el
mismo dispositivo mientras queda una gestión permanente sin resolver del ejecutivo anterior
(ahora posible gracias a este fix), esa gestión huérfana seguiría en Room sin asociación clara al
usuario que la generó. No se evaluó ni corrigió en esta ronda — el foco fue exclusivamente
desbloquear el logout sin perder datos. Registrar como deuda técnica futura si se prioriza.

### No incluir en el commit

- `apps/mobile-android/gradle.properties` — cambio preexistente ajeno, sigue sin tocar.

### DT-013

Sin cambios. Permanece diferida y documentada, según lo indicado explícitamente para esta ronda.

---

## Ronda — Gestiones: flujo funcional completo online/offline (2026-08-17/18)

Alcance: NO se tocó backend/API/WSL. Solo Android/Windows. Cubre:
persona asignada → registrar gestión → persistencia local → offline → sincronización → recuperación.

### Flujo técnico confirmado

`PersonaDetalleScreen` (feature:asignacion) → botón "Registrar gestión" → `GestionFormScreen`
(feature:gestion) con `personaId`+`asignacionDiariaId`. El formulario expone: tipo de gestión
(chips `SIN_CONTACTO`/`CONTACTO_FAMILIAR`/`COMPROMISO_PAGO`, obligatorio), captura de GPS
(obligatoria, botón "Capturar", el botón "Registrar gestión" está deshabilitado hasta tenerla),
observación (opcional, ≤500), observación de dirección (opcional, ≤200), fecha de compromiso
(solo visible/obligatoria si tipo=COMPROMISO_PAGO, debe ser hoy o posterior). Sin campo de
fotografía — **no implementada** en UI, DTO ni esquema de BD (confirmado en los tres niveles).

Al guardar: `GestionRepository.guardarLocal()` inserta en Room `gestion_local` con un **UUID
generado en el cliente** como PK — este mismo UUID se envía como `id` en el POST y es el
mecanismo de idempotencia (servidor responde 200/IDEMPOTENTE si mismo id+contenido, 409/CONFLICTO
si mismo id+contenido distinto). Guardado 100% local primero (offline-first real, no espera red)
y dispara `GestionSyncScheduler.programarEnvioInmediato()` (WorkManager, red requerida).

`GestionRepository.procesarOutbox()` usa un patrón de lease (CAS) para concurrencia segura,
clasifica la respuesta HTTP: 200/201→SINCRONIZADA; 401→vuelve a PENDIENTE_ENVIO y detiene el lote
(el 401 real ya pasó por `SingleFlightAuthenticator` antes de llegar aquí, confirmado — la
sincronización de gestiones usa el mismo cliente autenticado que el resto de la app); 409→CONFLICTO;
422→ERROR_PERMANENTE; 5xx e IOException→ERROR_REINTENTABLE con backoff exponencial manual
(30s·2^intentos, máx 24h, vía `fechaProximoIntentoEpoch`). **Importante:** `getElegibles()` solo
recoge `PENDIENTE_ENVIO`/`ERROR_REINTENTABLE` — `ERROR_PERMANENTE` y `CONFLICTO` nunca se
reintentan automáticamente ni tienen UI de reintento/descarte manual (ver deuda técnica).

Logout con pendientes: `HomeViewModel.solicitarLogout()` bloquea si
`gestionRepository.contarNoResueltas() > 0` (cualquier estado != SINCRONIZADA); ofrece
"Sincronizar y cerrar sesión" → `procesarOutbox()` → si quedan pendientes, permanece bloqueado
("No se pudo sincronizar"). Coincide exactamente con RN-24.

### Bugs encontrados y corregidos

**1. Endpoint de gestiones apuntaba a la ruta incorrecta (crítico, ya estaba parcialmente
corregido sin commitear al iniciar esta ronda).** `GestionApi.crearGestion()` usaba
`@POST("gestiones")` en vez de `@POST("api/v1/gestiones")` (todos los demás endpoints usan el
prefijo `api/v1/`). Efecto: **todo intento de sincronizar una gestión fallaba con 404 real**
contra el backend, clasificándose como `ERROR_PERMANENTE` (sin reintento posible). Este fix y su
test de regresión (`GestionApiTest`, verifica la ruta HTTP real vía MockWebServer) ya existían sin
commitear al comenzar esta ronda — se conservaron, verificaron y se incluyen en el commit.

**2. Duplicación real por doble-tap en "Registrar gestión" (encontrado y corregido en esta
ronda).** Severidad: Alta — genera una **gestión duplicada real en el backend** (2 filas
Postgres con mismo contenido, IDs distintos), no solo un artefacto local.
- **Síntoma:** doble-tap sobre "Registrar gestión" crea 2 gestiones locales distintas (2 UUID),
  ambas eventualmente sincronizadas como 2 filas independientes en `cobranza.gestiones`.
- **Causa raíz:** `GestionFormViewModel.guardar()` solo comprobaba `isSubmitting`. Ese flag
  vuelve a `false` en cuanto el guardado local (rápido, síncrono) termina — **antes** de que la
  navegación de salida (`LaunchedEffect(guardadoExitoso) { onNavigateBack() }`) surta efecto. En
  esa ventana el botón vuelve a estar habilitado y el formulario sigue con los mismos datos
  cargados; un segundo tap real (no simultáneo, ~400ms después) reenvía el mismo formulario.
- **Evidencia:** reproducido con `adb shell input tap` encadenado dos veces sobre el botón;
  logcat mostró un único POST en el instante del doble-tap pero un segundo registro quedó
  `PENDIENTE_ENVIO` en Room y se confirmó como segunda fila en Postgres tras forzar la
  sincronización. Test `GestionFormViewModelTest` reproduce el fallo determinísticamente (sin el
  fix, `doble tap tras guardado exitoso no crea una segunda gestion local` falla; con el fix,
  pasa) — confirmado revirtiendo temporalmente el guard y re-ejecutando.
- **Solución:** `guardar()` ahora también se bloquea si `guardadoExitoso == true`
  (`if (s.isSubmitting || s.guardadoExitoso) return`), y el botón en `GestionFormScreen` agrega
  `!state.guardadoExitoso` a su condición `enabled` como defensa adicional en la UI.
- **Archivos:** `GestionFormViewModel.kt`, `GestionFormScreen.kt`.
- **Test de regresión:** `GestionFormViewModelTest.kt` (nuevo, 3 casos).

### Escenarios validados con evidencia

- Validaciones de formulario: sin tipo, fecha compromiso requerida, fecha pasada inválida,
  observación >500 caracteres — todas correctas, mensajes controlados, sin crash.
- Creación ONLINE: UI = Room = API idénticos (mismo UUID, persona, tipo, observación, GPS,
  `asignacion_diaria_id`) — verificado consultando las tres capas directamente.
- Creación OFFLINE (API detenida vía `docker stop`): gestión con `COMPROMISO_PAGO` + fecha se
  guarda localmente sin bloquear la UI, clasificada `ERROR_REINTENTABLE` (IOException/
  ConnectException correctamente distinguido de error permanente). GPS se captura 100% offline
  (no depende de red).
- Force-stop con gestión pendiente + API caída: sesión, asignación y la gestión pendiente
  siguen disponibles tras reabrir — sin pérdida, sin duplicación.
- Recuperación de conectividad: no hay disparador proactivo por reconexión (ver deuda técnica);
  al disparar cualquier sincronización real (crear otra gestión), el lote completo —incluida la
  pendiente offline— se envía y ambas quedan `SINCRONIZADA`, confirmado en Postgres con los
  mismos IDs y timestamps de creación local preservados.
- Se detectó una ventana transitoria real (no bug) de "Sin personas en la asignación" al cruzar
  la medianoche del entorno: el job de generación de asignación diaria del backend aún no había
  corrido para el nuevo día; se resolvió sola minutos después sin intervención. El logout SÍ
  limpia todo el caché local (`BundleReplacementTransaction.limpiarTodo()`), comportamiento
  esperado y ya documentado en RN-24.

### Deuda técnica nueva (no corregida, documentada)

- Sin mecanismo de reintento proactivo de gestiones al recuperar conectividad — depende del
  trabajo periódico (1h) o de una acción del usuario que dispare sync (crear otra gestión,
  intentar logout). Asignación diaria sí tiene un botón manual de "Sincronizar"; Gestiones no.
- Sin UI para reintentar o descartar manualmente una gestión en `ERROR_PERMANENTE`/`CONFLICTO`
  — al no ser elegibles para `getElegibles()`, quedan bloqueando `contarNoResueltas() > 0`
  **indefinidamente**, impidiendo el logout normal en ese dispositivo sin intervención técnica
  directa en la base de datos.

### No incluir en el commit

- `apps/mobile-android/gradle.properties` — cambio preexistente ajeno, sigue sin tocar.

### Siguiente acción exacta

Suite completa 180/180 OK, lint y assembleDebug exitosos. Gestiones queda técnicamente
**aprobado con observaciones** (2 bugs corregidos, 2 deudas de UX documentadas, sin bloqueantes).
Pendiente: decidir si priorizar un mecanismo de reintento/descarte manual para gestiones en
error permanente antes de producción, dado que bloquea el logout sin vía de escape para el
usuario final.

---

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
