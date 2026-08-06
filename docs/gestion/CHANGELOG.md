# Changelog

Todos los cambios significativos del proyecto se documentan aquí.
Formato basado en [Keep a Changelog](https://keepachangelog.com/es/1.1.0/).

---

## [Sin versión] — 2026-08-05 — Fase 5B-1: Consulta administrativa de usuarios (solo lectura) — IMPLEMENTADA ✅

### Añadido — API (`apps/api/`)

- `EstadoUsuario` — enum: ACTIVO, BLOQUEADO_TEMPORAL, BLOQUEADO, INACTIVO. Calculado desde `activo`, `bloqueado`, `bloqueadoHasta` y `Clock`.
- `UsuarioAdminService` — listado paginado (`listar`), detalle (`obtenerDetalle`), batch queries sin N+1: 1 page query + roles + supervisión + nombres supervisor.
- `especificacionEstado()` — JPA Specification con expresión switch; `especificacionRol()` — EXISTS subquery via Criteria API (previene duplicados en paginación).
- `UsuarioAdminController` — `GET /api/v1/admin/usuarios` (paginado, filtros: nombreUsuario, estado, rol) y `GET /api/v1/admin/usuarios/{id}`. Validación manual → 400.
- `@PreAuthorize("hasAuthority('PERM_USUARIOS_VER')")` a nivel de clase — JEFE_SUPERVISORES y TECNOLOGIA autorizados; SUPERVISOR y EJECUTIVO_TERRENO devuelven 403.
- `UsuarioRepository` — añadido `findAllByIdIn`.
- `UsuarioRolRepository` — añadido `findAllByUsuarioIdInAndActivoTrue`.
- `SupervisionRepository` — añadido `findAllByEjecutivoIdInAndActivoTrue`.
- `RolPermisoRepository` — añadido `findAllByIdRolIdIn`.
- `EstadoUsuarioTest` — 7 tests unitarios con `Clock.fixed()`.
- `UsuarioAdminRestTest` — 28 tests de integración (autorización, filtros, paginación, detalle, ausencia de campos sensibles).
- **API: 323 pruebas — 0 failures — BUILD SUCCESS.**
- ADR-0046 — política de acceso a consultas administrativas de usuarios.
- `contracts/openapi/cobranza-api.yaml` — schemas `EstadoUsuario`, `RolVigenteAdmin`, `ItemListadoUsuarioAdmin`, `RespuestaListadoUsuarios`, `DetalleUsuarioAdmin`; endpoints documentados.

### Añadido — Angular (`apps/admin-web/`)

- `permission.guard.ts` — guard funcional `CanActivateFn` basado en `permisos[]` del perfil (no roles). Espera INICIALIZANDO. Redirige a /forbidden o /login.
- `usuario.models.ts` — tipos: `EstadoUsuario`, `RolVigente`, `ItemListadoUsuario`, `RespuestaListadoUsuarios`, `DetalleUsuario`, `FiltrosListado`.
- `usuarios.service.ts` — `listar(pagina, tamanio, filtros)` con HttpParams condicionales; `obtenerDetalle(id)`.
- `UsuariosListComponent` — standalone, signals, tabla Material, filtros reactivos con debounce 400ms en nombreUsuario, paginación, sincronización con query params.
- `UsuarioDetailComponent` — standalone, signals, manejo de 403→/forbidden, 404 y 500 con templates específicos, queryParams preservados en botón Volver.
- `usuarios.routes.ts` — rutas lazy con `canActivate: [authGuard, permissionGuard]`, `data: { permission: 'USUARIOS_VER' }`.
- `app.routes.ts` — añadido `/usuarios` con loadChildren.
- `layout.component.ts` — enlace "Usuarios" visible solo con `USUARIOS_VER`.
- `e2e/usuarios.spec.ts` — 8 tests Playwright interceptados (menú visible/oculto, forbidden, listado, filtros, detalle, volver, paginación).
- **Angular: 50 tests Vitest — 0 failures.**
- **Playwright: 14 tests — 14 passed (6 auth + 8 usuarios).**
- **npm audit --audit-level=high — 0 vulnerabilidades high/critical.**

---

## [Sin versión] — 2026-08-03 — Fase 5A: Base del administrador web y autenticación web — VALIDADA ✅

### Añadido — API (`apps/api/`)

**Autenticación web separada de Android**

- `V011__autenticacion_web.sql` — columna `tipo_cliente VARCHAR(10) NOT NULL DEFAULT 'ANDROID'` en `sesiones_autenticacion`; `dispositivo_id` nullable; CHECKs `ck_sesiones_cliente_dispositivo` y `ck_sesiones_tipo_cliente`; índices parciales únicos `uq_sesiones_activa_android` y `uq_sesiones_activa_web`.
- `SesionAutenticacion` — constructor WEB sin dispositivo, campo `tipoCliente`, métodos `esWeb()` y `getTipoCliente()`.
- `GestorTokens.emitirAccessToken()` — claim `did` condicional (absent en WEB), claim `tipo_cliente` en todos los tokens.
- `SesionRepository.findActivaWebByUsuarioId()` — búsqueda de sesión WEB activa.
- `ResultadoAutenticacionWeb` — record de resultado interno con AT, RT crudo, expiración de sesión y expiración de RT.
- `AutenticacionService.loginWeb()` — autenticación sin registro de dispositivo; crea sesión WEB; cierra sesión WEB previa.
- `AutenticacionService.renovarWeb()` — rotación de refresh token para sesiones WEB; misma lógica de reuse-detection.
- `SolicitudLoginWeb`, `RespuestaLoginWeb` — DTOs web (sin `identificadorInstalacion`, sin RT en body).
- `AutenticacionWebController` — `POST /api/v1/auth/web/login`, `POST /api/v1/auth/web/refresh` (cookie), `POST /api/v1/auth/web/logout` (limpia cookie). Cookie `rt_web`: HttpOnly, SameSite=Strict, configurable `Secure`.
- `WebOriginValidationFilter` — `OncePerRequestFilter` (Order=1); valida header `Origin` (fallback `Referer`) en `/api/v1/auth/web/refresh` y `/api/v1/auth/web/logout`; rechaza con 403 si el origen no coincide con `app.web.allowed-origin`.
- `application.yml` — `app.web.allowed-origin=${WEB_ALLOWED_ORIGIN:http://localhost:4200}`.
- `AutenticacionController./me` — corregido: claim `did` nullable, campo `tipoCliente` en respuesta.
- `RespuestaInfoUsuario` — campo `sesionId`, `dispositivoId` nullable, `tipoCliente`, `permisos` agregados.
- `SeguridadConfig` — `/api/v1/auth/web/login` y `/api/v1/auth/web/refresh` en permitAll.
- `AutenticacionWebIntegracionTest` — 19 tests: login, cookie HttpOnly, sin RT en body, JWT sin did, JWT con tipo_cliente=WEB, refresh, reuse-detection, logout, segundo login cierra sesión anterior; + 5 tests de validación de Origin/Referer.
- `.env.example` — variables `WEB_ALLOWED_ORIGIN` y `WEB_COOKIE_SECURE` documentadas.
- **API: 288 pruebas — 0 failures — BUILD SUCCESS.**
- ADR-0043 (Angular 22), ADR-0044 (auth web: memoria+cookie), ADR-0045 (proxy mismo origen).

### Añadido — Angular (`apps/admin-web/`)

**Panel administrativo — base técnica**

- Proyecto inicializado: Angular 22.1.0, TypeScript 6.0.2, Angular Material 22.1.0, Vitest 4.1.10, Playwright 1.62.1.
- `TokenStorageService` — access token únicamente en memoria de proceso.
- `AuthService` — signals (`INICIALIZANDO/AUTENTICADA/NO_AUTENTICADA`), single-flight refresh con `shareReplay`.
- `SessionBootstrapService` — `APP_INITIALIZER`; bootstrap refresh → loadProfile → estado AUTENTICADA; fallo → NO_AUTENTICADA.
- `authInterceptor` funcional — adjunta Bearer, reintenta con refresh en 401, evita loop (no reintenta si no había token).
- `authGuard` / `loginGuard` / `roleGuard` funcionales — esperan resolución de INICIALIZANDO antes de decidir.
- `LayoutComponent` — sidenav Material con "Inicio", toolbar con usuario y logout.
- `LoginComponent` — formulario reactivo con validación, loading, Enter submit, no doble submit, error visible.
- `HomeComponent` — perfil real (roles, permisos, sesionId, tipoCliente).
- `ForbiddenComponent`, `NotFoundComponent`.
- `app.routes.ts` — lazy loading, rutas protegidas.
- `proxy.conf.json` — `/api/**` → `localhost:8080`.
- `README.md` — documentación específica del proyecto (stack, requisitos, dev local, seguridad, ADRs).
- **30 tests unitarios Vitest** — 8 archivos spec: `TokenStorageService`, `AuthService` (8 tests), `SessionBootstrapService` (3), `authInterceptor` (6), `authGuard` (2), `loginGuard` (2), `roleGuard` (4), `App` (1). 0 failures.
- **Cobertura:** `core/auth` 96.66%, `core/http` 88.88%, `guards` 100% statements.
- 6 tests E2E Playwright — 6 passed (redirect sin sesión, login, credenciales malas, no doble submit, sesión existente, logout). Todos intercept-based.
- `ng build` — BUILD SUCCESSFUL (326.93 kB main, lazy chunks).
- `.github/workflows/admin-web-ci.yml` — CI con build, Vitest, Playwright.

### Actualizado

- `contracts/openapi/cobranza-api.yaml` — `RespuestaInfoUsuario` con `sesionId`, `dispositivoId` nullable, `tipoCliente`, `permisos`; schema `RespuestaLoginWeb`; paths `/api/v1/auth/web/login`, `/api/v1/auth/web/refresh`, `/api/v1/auth/web/logout`.

---

## [Sin versión] — 2026-08-03 — Fase 4C-B: Búsqueda directa por RUT — IMPLEMENTADA ✅

### Añadido — API (`apps/api/`)

**Módulo `sincronizacion` — Endpoint POST /api/v1/personas/busquedas**

- `RutValidacionApi` — interfaz en `personas.api` (`@NamedInterface("api")`); expone `esValido(rutNumero, rutDv): boolean` sin revelar el tipo de dominio interno `Rut` entre módulos.
- `RutValidacionServicio` — implementación en `personas.aplicacion`; delega a `Rut.of()` con captura de `RutInvalidoException`.
- `RutInvalidoEnBusquedaException` — excepción de aplicación con código estable `RUT_INVALIDO`.
- `SolicitudBusquedaPersona` — record `{ @NotBlank rutNumero, @NotBlank rutDv }`. RUT en body por privacidad (ADR-0041).
- `RespuestaBusquedaPersona` — record `{ version: 1, generadoEn: Instant, persona: DatosPersonaDescarga }`.
- `BusquedaPersonaService` — valida RUT vía `RutValidacionApi`; busca persona, direcciones, avales, operaciones, últimas 10 gestiones; emite log estructurado `[BUSQUEDA_AUDITORIA]` con `ejecutivoId` y `personaId` (sin RUT).
- `BusquedaPersonaController` — `@PostMapping("/busquedas")`, `@PreAuthorize("hasRole('EJECUTIVO_TERRENO')")`, `Cache-Control: no-store` en la respuesta. `@ExceptionHandler(RutInvalidoEnBusquedaException)` local retorna ProblemDetail `code=RUT_INVALIDO`.
- `BusquedaPersonaRestTest` — 21 tests de integración: 401/403, RUT inválido (DV incorrecto, campos en blanco), 404, 200, envelope (`version`, `generadoEn`, `persona`), `Cache-Control: no-store`, PII no filtrado en errores.
- `contracts/openapi/cobranza-api.yaml` — v1.0.0; schemas `SolicitudBusquedaPersona`, `RespuestaBusquedaPersona`; path `/api/v1/personas/busquedas`.
- **API: 269 pruebas — 0 failures.**

### Añadido — Android `:core:database` (Room v3)

- `PersonaDirectaEntity` — tabla `persona_directa`; `@PrimaryKey id: String`, `rutNumero`, `rutDv`, `nombre`, `versionContrato`, `generadoEnEpoch`, `fechaConsultaEpoch`, `detalleJson: String` (JSON completo de `DatosPersonaDescarga`). Índice en `(rutNumero, rutDv)`.
- `PersonaDirectaDao` — `upsert`, `findById`, `findByRut`, `deleteAll`.
- `MIGRATION_2_3` — recreación de `gestion_local` con `asignacionDiariaId TEXT` (nullable); INSERT explícito de 25 columnas; recreación de 4 índices; creación de `persona_directa`. Sin `fallbackToDestructiveMigration`.
- `GestionLocalEntity.asignacionDiariaId: String?` (era `String` NOT NULL).
- `CobranzaDatabase` v3 — añadida `PersonaDirectaEntity`; `personaDirectaDao()`.
- `DatabaseModule` — `MIGRATION_2_3` añadida a `addMigrations`; `providePersonaDirectaDao`.
- `BundleReplacementTransaction.limpiarTodo()` — añadido `personaDirectaDao.deleteAll()`.
- Esquema exportado: `schemas/.../3.json` — 11 entidades.

### Añadido — Android `:core:network`

- `BusquedaDtos.kt` — `SolicitudBusquedaDto`, `RespuestaBusquedaDto` (`@Serializable`).
- `PersonaBusquedaApi.kt` — `@POST("api/v1/personas/busquedas") suspend fun buscarPersona(@Body): Response<RespuestaBusquedaDto>`.
- `NetworkModule` — `providePersonaBusquedaApi`.

### Añadido — Android `:feature:busqueda` (módulo nuevo)

- `RutValidator.kt` — `object RutValidator { fun esValido(numero, dv): Boolean; fun calcularDv(numero): String }` — algoritmo Módulo 11 estándar.
- `BusquedaDirectaRepository.kt` — `sealed class ResultadoBusqueda { Encontrada(personaId), NoEncontrada, RutInvalido, SinConexion, Error }`. Valida localmente antes de llamar a la API; serializa `body.persona` a JSON; hace upsert en `PersonaDirectaDao`.
- `BusquedaDirectaViewModel.kt` — `@HiltViewModel`; estado `BusquedaState`; `onRutNumeroChanged` filtra dígitos (máx. 8); `onRutDvChanged` uppercase (máx. 1).
- `BusquedaDirectaScreen.kt` — dos `OutlinedTextField`, botón buscar, indicador de carga, `LaunchedEffect` para navegar al encontrar persona.
- `BusquedaNavigation.kt` — `RUTA_BUSQUEDA_DIRECTA = "busqueda/directa"`.
- `RutValidatorTest.kt` — 13 tests (RUTs válidos, DV incorrecto, K mayúscula/minúscula, formato).
- `BusquedaDirectaViewModelTest.kt` — 9 tests (flujo exitoso, no encontrada, RUT inválido, sin conexión, filtros de campo).

### Modificado — Android `:feature:gestion`

- `GestionForm` — campo `origenGestion: OrigenGestion` añadido antes de `asignacionDiariaId: String?`.
- `GestionValidator` — nuevo `OrigenIncoherente`; cross-validación `ASIGNACION_DIARIA + null asignacionDiariaId → OrigenIncoherente`; `BUSQUEDA_DIRECTA + non-null → OrigenIncoherente`.
- `GestionMapper` — `origenGestion = form.origenGestion.name` (era hardcodeado a `"ASIGNACION_DIARIA"`).
- `GestionFormViewModel` — dos ramas en `init`: si `asignacionDiariaId != null` → carga desde `PersonaDao`; si null → carga desde `PersonaDirectaDao`. Deriva `origenGestion` de `asignacionDiariaId != null`.
- `GestionNavigation` — nueva ruta `gestion/form/busqueda/{personaId}` para gestiones BUSQUEDA_DIRECTA.
- Tests `GestionMapperTest`, `GestionValidatorTest` — añadido `origenGestion = OrigenGestion.ASIGNACION_DIARIA` en constructores de `GestionForm`.

### Modificado — Android `:app`

- `CobranzaNavGraph` — añadido `onIrABusqueda` en `HomeScreen`; registrada `busquedaNavGraph` con `onRegistrarGestion { personaId -> navigate("gestion/form/busqueda/$personaId") }`.
- `HomeScreen` — botón "Buscar persona por RUT" (segundo botón en `HomeScreen`).
- `app/build.gradle.kts` — `implementation(project(":feature:busqueda"))`.
- `settings.gradle.kts` — `include(":feature:busqueda")`.

### Añadido — ADRs

- `ADR-0041` — Endpoint de búsqueda con POST por privacidad del RUT.
- `ADR-0042` — Snapshot en Room v3 para búsqueda directa.

### Resultados de verificación

| Suite | Resultado |
|---|---|
| API — `./mvnw clean verify` | ✅ **269 pruebas — 0 failures — BUILD SUCCESS** |
| Android — `assembleDebug` | ✅ BUILD SUCCESSFUL |
| Android — `lint` | ✅ BUILD SUCCESSFUL |
| Android — `testDebugUnitTest` | ✅ **165 pruebas JVM — 0 failures** |
| Android — `assembleDebugAndroidTest` | ✅ APK compilado |
| Android — `connectedDebugAndroidTest` | ⏭️ No ejecutado — sin emulador en WSL2 |

---

## [Sin versión] — 2026-08-02 — Fase 4C-A: Gestiones offline ASIGNACION_DIARIA — IMPLEMENTADA ✅

### Añadido — `:core:database` (Room v2)

- `GestionLocalEntity` — tabla `gestion_local`; sin FK a persona (campos desnormalizados: `personaRutNumero`, `personaRutDv`, `personaNombre`); campos de lease (`leaseHastaEpoch`) y backoff (`fechaProximoIntentoEpoch`, `cantidadIntentos`).
- `GestionLocalDao` — `adquirirLease()` (CAS atómico, retorna filas afectadas), `recuperarLeasesExpirados()`, `getElegibles()` (FIFO, respeta backoff), `contarNoResueltas()`, `getGestionesPorPersona()`, `insert()`, `actualizarEstado()`, `deleteAll()`.
- `MIGRATION_1_2` — migración aditiva v1→v2; añade `gestion_local` + 4 índices; sin `fallbackToDestructiveMigration`.
- `schemas/...CobranzaDatabase/2.json` — esquema Room v2 exportado.
- `BundleReplacementTransaction.reemplazar()` — preserva `gestion_local` (outbox no se toca en descarga).
- `BundleReplacementTransaction.limpiarTodo()` — llama `gestionLocalDao.deleteAll()` como primera operación (logout borra outbox).
- Tests: `GestionLocalDaoTest` (14 tests), `MigrationTest` (4 tests), `BundleReplacementTransactionTest` (7 tests, incluye nuevo test de preservación de outbox).

### Añadido — `:core:network`

- `GestionDtos.kt` — `SolicitudGestionDto` (precisionMetros: Double), `RespuestaGestionDto`.
- `GestionApi.kt` — `POST /api/v1/gestiones`.
- `NetworkModule.kt` — `provideGestionApi`.

### Añadido — `:feature:gestion` (módulo nuevo)

**Dominio:**
- `GestionModels.kt` — `TipoGestion` (SIN_CONTACTO, CONTACTO_FAMILIAR, COMPROMISO_PAGO), `OrigenGestion` (ASIGNACION_DIARIA, BUSQUEDA_DIRECTA), `EstadoSincronizacion` (6 estados), `UbicacionCapturada`, `GestionForm`, `GestionResumen`, `ResultadoEnvio`, `ResultadoProcesamiento`.
- `GestionValidator.kt` — validaciones: COMPROMISO_PAGO requiere `fechaCompromiso ≥ hoy`, `observacion ≤ 500 chars`, `observacionDireccion ≤ 200 chars`.

**Datos:**
- `GestionMapper.kt` — `formToEntity`, `entityToDto` (Float→Double precisionMetros, epoch→ISO-8601), `localEntityToResumen`, `historicaEntityToResumen`.
- `GestionRepository.kt` — outbox procesarOutbox con `Mutex` single-flight; CAS por registro; backoff `min(30s * 2^n, 24h)`; historial unificado con dedup por UUID (local prevalece); mapeo HTTP: 201/200→SINCRONIZADA, 401→SesionExpirada, 409→CONFLICTO, 422→ERROR_PERMANENTE(VALIDACION), 400/403/404→ERROR_PERMANENTE(HTTP_N), 5xx→ERROR_REINTENTABLE, IOException→ERROR_REINTENTABLE.

**GPS:**
- `LocationProvider.kt` — interfaz con `ResultadoUbicacion` (Exito, PermisoDenegado, Timeout, ProveedorDeshabilitado).
- `AndroidLocationProvider.kt` — `LocationManager` (sin FusedLocation), timeout 30s con `suspendCancellableCoroutine`, detección mock (`isMock`/`isFromMockProvider`), una sola lectura (sin tracking continuo).

**Worker:**
- `EnvioGestionWorker.kt` — `@HiltWorker`; mapea `ResultadoProcesamiento` → `WorkManager.Result`.
- `GestionSyncScheduler.kt` — `programarEnvioInmediato()` (KEEP), `programarPeriodico()` (1h KEEP), `cancelarTodo()`.

**UI:**
- `GestionFormViewModel.kt` — `GpsState` sealed, `GestionFormState`, doble-tap guard vía `isSubmitting`.
- `GestionFormScreen.kt` — `FilterChip` TipoGestion, tarjeta GPS, campos, botón Registrar deshabilitado hasta GPS capturado.
- `GestionHistorialViewModel.kt` — combina `gestion_local` + `gestion_historica`; dedup por UUID (local prevalece).
- `GestionHistorialScreen.kt` — lista con `BadgeEstado` para los 6 estados.
- `GestionNavigation.kt` — rutas `gestion/form/{personaId}/{asignacionDiariaId}` y `gestion/historial/{personaId}`.

**Tests:** `GestionValidatorTest` (7), `GestionMapperTest` (5), `GestionRepositoryTest` (10, incluye 400/403/404), `EnvioGestionWorkerTest` (4).

### Modificado — `:app`

- `LogoutUseCase.kt` — añadido `GestionSyncScheduler.cancelarTodo()`.
- `HomeViewModel.kt` — `EstadoLogout` sealed class (Inactivo, Procesando, GestionesPendientes, SincronizandoParaCerrar, ErrorSincronizacion); `solicitarLogout()`, `sincronizarYLogout()`, `cancelarLogout()`.
- `HomeScreen.kt` — diálogos para cada estado de logout; sin "salir igualmente".
- `CobranzaNavGraph.kt` — rutas `gestionNavGraph`; callbacks `onRegistrarGestion` y `onVerHistorial` en `asignacionNavGraph`.
- Tests: `LogoutUseCaseTest` (4), `LogoutIntegrationTest` (4).

### Modificado — `:feature:asignacion`

- `AsignacionNavigation.kt` — ruta incluye `asignacionDiariaId`; nuevos callbacks.
- `AsignacionViewModel.kt` — `AsignacionUiState` añade `asignacionDiariaId`.
- `AsignacionListScreen.kt` — pasa `asignacionDiariaId` en `onNavigateToDetalle`.
- `PersonaDetalleViewModel.kt` — expone `asignacionDiariaId` y `gestionesLocales: Flow`.
- `PersonaDetalleScreen.kt` — reemplaza placeholder Fase 4C con `SeccionGestionesLocales` real.

### Añadido — ADRs

- `ADR-0037` — Outbox pattern y migración Room v2.
- `ADR-0038` — Estados de sincronización, lease atómico y WorkManager.
- `ADR-0039` — GPS vía LocationManager (sin Google Play Services).
- `ADR-0040` — Política de logout con gestiones no resueltas.

### Resultados de verificación (parcial — pendiente ejecución completa)

| Suite | Resultado |
|---|---|
| Android — `testDebugUnitTest` (total JVM) | ✅ 135 tests — 0 failures (validados en sesión anterior) |
| Android — `assembleDebug` | ✅ BUILD SUCCESSFUL |
| Android — `lint` | ✅ BUILD SUCCESSFUL |
| API — `./mvnw clean verify` | ⏭️ Pendiente ejecución (sin cambios en API) |

---

## [v0.10.0-descarga-offline] — 2026-08-02 — Fase 4B: Cartera offline — CERRADA ✅

### Añadido — Módulo `:core:database`

**Room 2.7.2 con KSP. 9 entidades, 8 DAOs, 1 transacción atómica, 4 suites de prueba.**

Entidades: `AsignacionDiariaEntity`, `PersonaEntity`, `AsignacionPersonaEntity`, `DireccionEntity`, `AvalEntity`, `OperacionEntity`, `CuotaEntity`, `GestionHistoricaEntity`, `SyncMetadataEntity`.

`BundleReplacementTransaction`:
- `reemplazar(bundle)` — transacción atómica delete-all → insert-all → update metadata.
- `marcarSinAsignacion(fecha)` — 204 API response: preserva datos anteriores, marca como desactualizados.
- `limpiarTodo()` — logout: elimina todas las tablas y resetea metadata.

Convenciones de tipos: UUID→String, BigDecimal→String, Instant→Long epoch, LocalDate→String ISO.
FK con `ON DELETE CASCADE`; `PRAGMA foreign_keys = ON` en `RoomDatabase.Callback.onOpen`.

### Añadido — DTOs y API de sincronización en `:core:network`

- `SyncModels.kt` — DTOs: `RespuestaAsignacionDiariaDto`, `PersonaDescargaDto`, `DireccionDescargaDto`, `AvalDescargaDto`, `OperacionDescargaDto`, `CuotaDescargaDto`, `GestionDescargaDto`.
- `BigDecimalSerializer.kt` — `KSerializer<BigDecimal>` que acepta primitivos JSON numéricos o cadena.
- `SincronizacionApi.kt` — `GET /api/v1/asignaciones/diaria/activa` retorna `Response<RespuestaAsignacionDiariaDto>`.
- `NetworkModule.kt` — añadido `provideSincronizacionApi`.

### Añadido — Módulo `:feature:asignacion`

**Repository, Worker, ViewModel, Screens, Scheduler, Mapper — 5 suites de prueba (31 tests).**

- `AsignacionRepository` — Mutex single-flight; maneja 200 (reemplazar), 204 (marcarSinAsignacion), 401/403 (ErrorPermanente), IOException (ErrorReintentar), 4xx/5xx, `SerializationException`.
- `DescargaAsignacionWorker` — `@HiltWorker`; mapea `ResultadoDescarga` → `WorkManager.Result`.
- `AsignacionSyncScheduler` — `programarInmediato()` (KEEP), `programarPeriodico()` cada 4h, `cancelarTodo()`.
- `AsignacionViewModel` — `combine(personas, metadata, busqueda).stateIn(WhileSubscribed(5_000))`; filtro por RUT/nombre en ViewModel.
- `AsignacionListScreen`, `PersonaDetalleScreen` — pantallas Compose con navegación.
- `asignacionNavGraph(navController, onLogout)` — extensión `NavGraphBuilder`.
- `ResultadoDescarga` — `Exito`, `SinAsignacion`, `ErrorReintentar`, `ErrorPermanente`, `VersionNoSoportada`.

### Modificado — `:feature:auth`

- `SessionRepository` cambiado de `@ActivityRetainedScoped` a `@Singleton` (requerido por `AsignacionRepository @Singleton` que necesita `TokenProvider`).
- `AuthModule.kt` añadido con `@Binds @Singleton abstract fun bindTokenProvider(impl: SessionRepository): TokenProvider`.
- `CobranzaNavGraph.kt` refactorizado a `authNavGraph(navController, onLoginExitoso)` extensión de `NavGraphBuilder`.

### Modificado — `:app`

- `CobranzaApp.kt` — implementa `Configuration.Provider`; inyecta `HiltWorkerFactory` para WorkManager manual.
- `CobranzaNavGraph.kt` (nuevo) — NavHost completo: rutas auth, home, asignacion/lista, asignacion/persona/{personaId}.
- `HomeScreen.kt` (nuevo) — pantalla principal con botón de cartera y logout.
- `LogoutUseCase.kt` (nuevo) — cancela workers, limpia BD, cierra sesión.
- `AndroidManifest.xml` — WorkManager auto-init deshabilitado con `tools:node="remove"` en `InitializationProvider`.

### Modificado — `libs.versions.toml`

- Añadidas versiones: `room = "2.7.2"`, `workmanager = "2.10.1"`, `robolectric = "4.14.1"`, `androidxTestCore = "1.6.1"`.
- Añadidas libraries: `room-runtime`, `room-ktx`, `room-compiler`, `room-testing`, `workmanager-ktx`, `workmanager-testing`, `hilt-work`, `hilt-compiler-androidx`, `robolectric`, `androidx-test-core`.

### Añadido — `settings.gradle.kts`

- `include(":core:database")` y `include(":feature:asignacion")`.

### Añadido — ADRs

- `ADR-0033` — Room como base de datos local Android.
- `ADR-0034` — WorkManager para sincronización de background.
- `ADR-0035` — Reemplazo atómico del bundle descargado.
- `ADR-0036` — Arquitectura del módulo `:feature:asignacion`.

### Correcciones aplicadas en verificación final

- `BundleReplacementTransaction.reemplazar()`: añadido `fechaConsultada = bundle.asignacion.fecha` en upsert de `SyncMetadataEntity`.
- `LogoutUseCase`: `sessionRepository.logout()` envuelto en `runCatching` (logout remoto best-effort).
- `AsignacionDescargaRestTest`: Test 13 — contrato JSON Android (nombres de campos, BigDecimal como número, UUID lowercase, fechas ISO-8601).
- `LogoutUseCaseTest` (4 tests mock) + `LogoutIntegrationTest` (4 tests Room/Robolectric).
- `CobranzaDatabase`: `exportSchema = true`; esquema v1 en `core/database/schemas/`.

### Resultados de verificación final

| Suite | Resultado |
|---|---|
| API — `./mvnw clean verify` | ✅ **248 tests — 0 failures — BUILD SUCCESS** |
| Android — `assembleDebug` | ✅ BUILD SUCCESSFUL |
| Android — `lint` | ✅ BUILD SUCCESSFUL |
| Android — `testDebugUnitTest` (total JVM) | ✅ **97 tests — 0 failures** |
| Android — `assembleDebugAndroidTest` | ✅ APK compilado |
| Android — `connectedDebugAndroidTest` | ⏭️ No ejecutado — sin emulador en WSL2 |

---

## [Sin versión] — 2026-08-02 — Fase 4A: Base Android — VALIDADA ✅

### Corrección — Contrato de login API

**Problema:** `SolicitudLogin` recibía `dispositivoId: UUID` (referencia interna DB). La app Android no puede conocer este UUID antes del primer login, porque el servidor lo genera en el momento del registro. Creaba un ciclo imposible de resolver.

**Cambio:** `dispositivoId: UUID` → `identificadorInstalacion: String (UUID canónico)`. El servidor registra el dispositivo automáticamente, **después** de validar las credenciales (ADR-0031).

| Archivo modificado | Cambio |
|---|---|
| `autenticacion/web/SolicitudLogin.java` | `dispositivoId: UUID` → `identificadorInstalacion: @NotBlank @Pattern(UUID) String` |
| `dispositivos/api/DispositivoConsultaApi.java` | Nuevo método `buscarORegistrar(String identificadorInstalacion, UUID usuarioId)` |
| `dispositivos/infraestructura/DispositivoConsultaApiImpl.java` | Implementación transaccional de `buscarORegistrar` |
| `autenticacion/aplicacion/AutenticacionService.java` | Llama a `buscarORegistrar` en lugar de `buscarPorId` |
| `autenticacion/web/AutenticacionController.java` | Pasa `identificadorInstalacion` en lugar de `dispositivoId` |
| `AutenticacionIntegracionTest.java` | Reescrito: 8 tests nuevos — auto-registro, reutilización, JWT did, 400/401/409 por identificador |

**247 pruebas — 0 failures — BUILD SUCCESS** (incluye 2 tests de concurrencia añadidos en auditoría).

### Añadido — Proyecto Android `apps/mobile-android/`

**Toolchain:** AGP 9.3.0, Gradle 9.6.1, Kotlin 2.4.10, KSP 2.3.10, `compileSdk`/`targetSdk`=37, `minSdk`=29.

**`:core:network`**
- `ApiModels.kt` — `SolicitudLogin`, `SolicitudRenovacion`, `RespuestaToken`, `ApiError` (serializables)
- `AuthApi.kt` — `login`, `renovar`, `logout` vía Retrofit
- `TokenProvider.kt` — interfaz de contrato (desacopla `core:network` de `feature:auth`)
- `NetworkModule.kt` — dos clientes Hilt: `@Named("public")` y `@Named("authenticated")`
- `SingleFlightAuthenticator.kt` — `OkHttp Authenticator` + `Mutex` para refresh único en vuelo

**`:core:security`**
- `InstallationIdStore.kt` — Preferences DataStore; genera `identificadorInstalacion` una sola vez; persiste `sessionExpiresAt`
- `SecureTokenStore.kt` — AES-256-GCM vía Android Keystore; IV único por cifrado; limpia datos en excepción de descifrado

**`:feature:auth`**
- `AuthState.kt` + `ErrorTipo.kt` — sealed class / enum de estados de sesión
- `SessionRepository.kt` — `@ActivityRetainedScoped`; implementa `TokenProvider`; access token en memoria; lógica de verificación inicial y refresh
- `LoginViewModel.kt` — `@HiltViewModel`; valida formulario; mapea códigos ProblemDetail a `ErrorTipo`
- `CobranzaNavGraph.kt` — rutas Check → Login/Home; navega según `authState`

**`:app`**
- `CobranzaApp.kt` — `@HiltAndroidApp`
- `MainActivity.kt` — `@AndroidEntryPoint`, establece `CobranzaNavGraph`
- `AndroidManifest.xml` — `allowBackup=false`, `dataExtractionRules`
- `data_extraction_rules.xml` — excluye todos los dominios de cloud-backup y device-transfer

**Pruebas JVM**
- `SingleFlightAuthenticatorTest.kt` — 5 casos (token fresco, single-flight, 401, refresco concurrente, omite URL refresh)
- `InstallationIdStoreTest.kt` — idempotencia, persistencia, sessionExpiresAt
- `SecureTokenStoreTest.kt` — lógica de cifrado/descifrado (JVM; Keystore instrumentado en CI)
- `LoginViewModelTest.kt` — validaciones de formulario, mapeo de errores HTTP → ErrorTipo
- `SessionRepositoryTest.kt` — refresh exitoso, refresh 401, error de red, sesión vencida, clearSession

**CI:** `.github/workflows/android-ci.yml` — build debug, lint, JVM tests. Pruebas instrumentadas documentadas, no activas.

**Documentación:** ADR-0031, ADR-0032, `apps/mobile-android/README.md`, `contracts/openapi/cobranza-api.yaml` v0.9.0 (auth endpoints + login contract), `docs/arquitectura/MODULOS.md` (módulos Android 4A), `docs/gestion/STATUS.md`, `docs/gestion/ROADMAP.md`.

---

## [Sin versión] — 2026-08-02 — Auditoría Fase 4A ✅ VALIDADA

### Correcciones aplicadas en auditoría

| Archivo | Cambio |
|---|---|
| `dispositivos/infraestructura/DispositivoRepository.java` | Nuevo método `insertarSiNoExiste()` con INSERT ON CONFLICT DO NOTHING |
| `dispositivos/infraestructura/DispositivoConsultaApiImpl.java` | `buscarORegistrar()` usa fast-path + insert atómico (elimina TOCTOU) |
| `AutenticacionIntegracionTest.java` | +2 tests de concurrencia (mismo usuario y dos usuarios, mismo ID) |
| `feature/auth/ui/LoginViewModel.kt` | Eliminado `setNoAutenticado()` redundante tras `setError()` |
| `feature/auth/data/SessionRepository.kt` | `verificarSesionInicial()`: `flow.first()` con import correcto |
| `feature/auth/build.gradle.kts` | +`kotlinx.serialization.json`, +`compose-material-icons-extended` |
| `core/network/build.gradle.kts` | `retrofit`: `implementation` → `api` (expone `Response<T>` a consumidores) |
| `feature/auth/ui/HomeScreen.kt` | `@OptIn(ExperimentalMaterial3Api::class)` en `HomeScreen()` |
| `app/build.gradle.kts` | `compileSdk`/`targetSdk`: 36 → 37 |
| `feature/auth/build.gradle.kts` | `compileSdk`: 36 → 37 |
| `core/network/build.gradle.kts` | `compileSdk`: 36 → 37 |
| `core/security/build.gradle.kts` | `compileSdk`: 36 → 37 |
| `app/src/main/res/values/strings.xml` | Creado (`app_name`) |
| `app/src/main/res/values/themes.xml` | Creado (`Theme.AppCompat` placeholder) |
| `app/src/main/res/drawable/ic_launcher_*.xml` | Creados (background + foreground vectores) |
| `app/src/main/res/mipmap-anydpi-v26/*.xml` | Creados (adaptive icons) |
| `gradle/libs.versions.toml` | +`compose-material-icons-extended` alias |
| `.github/workflows/android-ci.yml` | `android-36` → `android-37.1`, `build-tools;37.0.0` |

### Resultados finales

| Suite | Resultado |
|---|---|
| API — `./mvnw clean verify` | ✅ **247 tests — 0 failures** |
| Android — `assembleDebug` | ✅ BUILD SUCCESSFUL |
| Android — `lint` | ✅ BUILD SUCCESSFUL — sin errores |
| Android — `testDebugUnitTest` | ✅ **38 tests — 0 failures** |
| Android — `assembleDebugAndroidTest` | ✅ BUILD SUCCESSFUL |
| Android — `connectedDebugAndroidTest` | ⏭️ Sin emulador en WSL2 |

### Auditoría de seguridad

- **SecureTokenStore:** AES-256-GCM, IV de 12 bytes generado por cipher (sin reutilización), clave en Keystore TEE, ciphertext+IV en DataStore atómico, fallo de descifrado limpia tokens.
- **Backup:** `allowBackup="false"` + `data_extraction_rules.xml` excluye root/database/sharedpref/file/external en cloud-backup y device-transfer.
- **Logging:** nivel HEADERS únicamente; filtro defensivo para `"contrasena"` y `"refreshToken"`.
- **SingleFlightAuthenticator:** Mutex garantiza un solo refresh concurrente; stale-token comparison evita refresh innecesario si otro hilo ya renovó; ciclo de refresh prevenido con check de URL.

---

## [Sin versión] — 2026-08-01 — Auditoría Fase 3D ✅ VALIDADA

### Correcciones aplicadas

- `SeguridadConfig.escribirProblemDetail` — añadidos `code`, `timestamp`, `path` a respuestas 401/403. Los errores de autenticación ahora siguen el mismo formato ProblemDetail que el resto de la API.
- `GestionRestTest` — tests 401/403 actualizados con aserciones sobre el cuerpo (`code`, `timestamp`, `path`). Añadido test 16: asignación en estado `BORRADOR` → 400 con `code=ESTADO_INVALIDO`.
- `AsignacionDescargaRestTest` — tests 401/403 actualizados con aserciones sobre el cuerpo.
- `contracts/openapi/cobranza-api.yaml` — contrato completo: schemas, paths, security, error format.
- `apps/api/README.md` — módulos y endpoints actualizados (Fases 3A–3D).
- `docs/arquitectura/MODULOS.md` — estados de todos los módulos actualizados.

**238 pruebas — 0 failures — BUILD SUCCESS. Modularidad PASS.**

---

## [Sin versión] — 2026-08-01 — Fase 3D: API REST asignaciones y gestiones ✅

### Nuevo — Endpoints REST para dispositivos Android

**`GET /api/v1/asignaciones/diaria/activa`**
- Descarga el bundle completo de la asignación diaria PUBLICADA del ejecutivo autenticado.
- Parámetro opcional `?fecha` (ISO-8601); por defecto usa la fecha actual en `America/Santiago`.
- Responde 200 con `RespuestaAsignacionDiaria` o 204 No Content si no hay asignación activa.
- Bundle por persona: RUT, nombre, direcciones vigentes, avales informativos, operaciones ACTIVA con cuotas VENCIDA/VIGENTE/FUTURA, últimas 10 gestiones.
- 8 queries `IN` en total — sin N+1 independientemente del número de personas.

**`POST /api/v1/gestiones`**
- Recibe una gestión generada en el dispositivo. Idempotente: mismo UUID con mismo contenido → 200 IDEMPOTENTE; mismo UUID con contenido diferente → 409 GESTION_CONFLICTIVA.
- `ejecutivoId` extraído del JWT `sub` (no se acepta del cliente).
- Retorna 201 INSERTADA o 200 IDEMPOTENTE. Sin header `Location`.
- `OrigenGestion` y `TipoGestion` validados como enums en el DTO web.

### Nuevo — Infraestructura de módulos bulk

| Módulo | Adición |
|---|---|
| `asignaciones::api` | `AsignacionDiariaNoEncontradaException`, `findPersonasEnAsignacionDiaria(UUID)` |
| `personas::api` | `DatosDireccion`, `DatosAval`, `findAllByIds()`, `findDireccionesVigentesPorPersonas()`, `findAvalesPorPersonas()` |
| `operaciones::api` | `OperacionConsultaApi`, `DatosOperacion`, `DatosCuota`, `findOperacionesActivasConCuotas()` (creado desde cero) |
| `gestiones::api` | `GestionConflictivaException` (movida desde dominio), `ResultadoRecepcion`, `findUltimasGestionesPorPersonas()` |
| `sincronizacion` | `DescargaAsignacionService`, `AsignacionDescargaController`, `GestionController` |

### Modificado — Excepciones tipadas y handlers

- `GestionService.recibirGestion()`: retorna `ResultadoRecepcion` (antes `void`); lanza `PersonaNoEncontradaException` y `AsignacionDiariaNoEncontradaException` en lugar de `IllegalArgumentException`.
- `GlobalExceptionHandler`: 4 nuevos handlers con ProblemDetail uniforme (code, timestamp, path).
- `OpenApiConfig`: SecurityScheme Bearer JWT (`bearerAuth`).

### Pruebas

- `AsignacionDescargaRestTest` — 12 tests HTTP (401, 403, 204, 200, bundle, fecha, content-type).
- `GestionRestTest` — 15 tests HTTP (201, 200 idempotente, 409 conflicto, 404, 400, 403, no Location).
- `GestionesIntegracionTest` — import actualizado; todos los 25 tests siguen pasando.

**237 pruebas — 0 failures — BUILD SUCCESS. Modularidad PASS.**

---

## [Sin versión] — 2026-08-01 — Fase 3C validada: tests concurrentes + auditoría de dominio ✅

### Pruebas adicionales (Tests 22–25)

- **Test 22**: Concurrencia — mismo UUID, contenido conflictivo (2 hilos). Un ganador, un `GestionConflictivaException`, una sola fila en BD.
- **Test 23**: Reintento idempotente no modifica `fecha_creacion_servidor`.
- **Test 24**: `fecha_creacion_servidor` generada en servidor; distinta de `fecha_gestion` del dispositivo.
- **Test 25**: Conflicto no modifica la gestión original — `persona_id` en BD verificado por JDBC.

### Documentación de dominio actualizada

| Documento | Cambio |
|---|---|
| `MODELO_DATOS.md` | Tabla `gestiones` reescrita con columnas reales de V010: `origen_gestion`, `asignacion_diaria_id`, `observacion_direccion`, `fecha_creacion_servidor`; eliminadas `ubicacion GEOMETRY` y `created_at`. |
| `MODELO_DOMINIO.md` | Sección Gestión ampliada: dos orígenes, GPS completo, `fecha_gestion` vs `fecha_creacion_servidor`, fotografías diferidas. |
| `DIAGRAMA_ENTIDAD_RELACION.md` | Entidad GESTIONES corregida; relación `}o--o|` con ASIGNACIONES_DIARIAS; notas actualizadas. |
| `DICCIONARIO_DATOS_PRELIMINAR.md` | Sección `gestiones` corregida con columnas reales y referencias a ADRs. |
| `REGLAS_NEGOCIO.md` | RN-12 fraccionada: RN-12 (dos orígenes, ADR-0026) + RN-12b (reglas de registro). |
| `REQUISITOS_FUNCIONALES.md` | RF-05i (dos orígenes) + RF-05j (idempotencia atómica) agregados. |

**210 pruebas — 0 failures — BUILD SUCCESS.**

---

## [Sin versión] — 2026-08-01 — Fase 3C corrección: idempotencia atómica con ON CONFLICT ✅

### Corrección — Idempotencia concurrentemente segura en `GestionService`

**Problema:** La estrategia read-check-then-write tenía una ventana TOCTOU: dos solicitudes concurrentes con el mismo UUID podían superar la verificación `findById` simultáneamente y luego ambas intentar el `INSERT`, provocando una excepción de PK no manejada que envenenaba la transacción JPA.

**Cambios:**

- `GestionRepository` — nuevo método `insertarSiNoExiste()` con `@Modifying(clearAutomatically = true)` y native query `INSERT ... ON CONFLICT (id) DO NOTHING`. Retorna 1 si se insertó, 0 si ya existía.
- `GestionService.recibirGestion()` — reestructurado en dos niveles:
  1. Fast-path: `findById` para el caso común de reintento simple (sin pasar por validaciones).
  2. Insert atómico: `insertarSiNoExiste()` para proteger contra concurrencia; si retorna 0, relee y compara contenido.
- `GestionesIntegracionTest` — **Test 21** agregado: 5 hilos simultáneos con el mismo UUID → ninguna excepción, exactamente 1 fila en BD.
- `ADR-0027` — actualizado para documentar la estrategia en dos niveles y el fundamento de `ON CONFLICT DO NOTHING` frente al manejo de `DataIntegrityViolationException`.

**BUILD SUCCESS — compilación limpia sin errores.**

---

## [Sin versión] — 2026-08-01 — Fase 3C: Gestiones de cobranza ✅

### Nuevo — Módulo `gestiones` completo

**Alcance:** Recepción idempotente de gestiones de cobranza en la API. Las gestiones son inmutables; el UUID se genera en el dispositivo Android.

**Migración V010 (`gestiones`):**
- Tabla `cobranza.gestiones` con: `id` (UUID PK, generado en dispositivo), `origen_gestion`, `asignacion_diaria_id` (nullable), `persona_id`, `ejecutivo_id`, `tipo_gestion`, `fecha_gestion` (timestamp dispositivo), `fecha_creacion_servidor` (timestamp servidor), GPS completo (`latitud`, `longitud`, `precision_metros`, `proveedor_gps`, `ubicacion_simulada`, `fecha_captura_gps`), `observacion`, `observacion_direccion`, `fecha_compromiso`.
- Sin `fecha_actualizacion` ni `version`: tabla append-only.
- CHECK constraints: coherencia `origen_gestion` ↔ `asignacion_diaria_id`, coherencia `tipo_gestion` ↔ `fecha_compromiso`, rangos GPS, precision >= 0.

**Orígenes:**
- `ASIGNACION_DIARIA`: requiere diaria PUBLICADA o FINALIZADA (caso offline); ejecutivo debe ser el destinatario; persona debe estar en la diaria.
- `BUSQUEDA_DIRECTA`: persona existe; ejecutivo tiene rol `EJECUTIVO_TERRENO`; sin restricción de cartera.

**Módulo `gestiones`:**
- `OrigenGestion`, `TipoGestion` — enums de dominio.
- `Gestion` — entidad JPA inmutable; constructor valida todas las reglas de dominio; `tieneContenidoConflictivo()` para detección de conflictos de idempotencia.
- `GestionConflictivaException` — excepción de dominio para UUID con contenido conflictivo.
- `GestionRepository` — Spring Data JPA.
- `ComandoCrearGestion` — record de aplicación.
- `GestionService.recibirGestion()` — recepción idempotente con check de contenido antes de insertar; valida origen, rol, membresía, estado de diaria y fecha de compromiso con zona horaria de negocio (`America/Santiago`).
- `GestionConsultaApi` + `DatosGestion` — interfaz pública (`@NamedInterface("api")`).
- `GestionConsultaApiImpl` — consulta por persona.

**Extensión `asignaciones::api`:**
- `findAsignacionDiaria(UUID)` — buscar diaria por ID (para validación cross-módulo).
- `personaEnAsignacionDiaria(UUID, UUID)` — verificar pertenencia de persona a diaria.

**Tests:**
- 20 tests de integración: 2 flujos exitosos de ASIGNACION_DIARIA (PUBLICADA y FINALIZADA), 2 rechazos de estado (BORRADOR y CANCELADA), 2 rechazos de membresía/destinatario, 2 flujos BUSQUEDA_DIRECTA (con y sin restricción de cartera), 4 rechazos de dominio (origen incoherente, COMPROMISO sin fecha, tipo con fecha, coordenadas/precision), 1 idempotencia, 1 conflicto, 1 rol, 1 schema (sin fecha_actualizacion), 1 modularidad, 1 persistencia completa.
- **205 pruebas — 0 failures — Modularidad PASS — BUILD SUCCESS**.

**ADRs:** ADR-0026 (dos orígenes), ADR-0027 (UUID dispositivo e idempotencia), ADR-0028 (gestión inmutable), ADR-0029 (separación temporal), ADR-0030 (fotografías diferidas).

---

## [Sin versión] — 2026-08-01 — Fase 3B corrección: historial individual de asignaciones mensuales ✅

### Corrección — AsignacionMensualPersona: historial individual por persona

**Problema:** la clave primaria compuesta `(asignacion_mensual_id, persona_id)` impedía conservar múltiples períodos históricos para la misma persona en la misma asignación mensual. Sin `fecha_inicio`/`fecha_fin` en la fila del vínculo, era imposible auditar cuándo cada persona estuvo asignada. No había ninguna garantía de integridad referencial a nivel de BD para el campo `cartera_id` denormalizado en la tabla hija.

**Cambios en V009:**
- `asignaciones_mensuales_personas` — PK reemplazada por `id UUID`. Añadidos: `fecha_inicio DATE NOT NULL`, `fecha_fin DATE` (NULL = activo), `fecha_actualizacion TIMESTAMPTZ`, `version BIGINT`.
- CHECKs: `(activa=TRUE AND fecha_fin IS NULL) OR (activa=FALSE AND fecha_fin IS NOT NULL)` y `fecha_fin IS NULL OR fecha_fin >= fecha_inicio`.
- `asignaciones_mensuales` — añadido `UNIQUE(id, cartera_id)` como ancla para la FK compuesta.
- `asignaciones_mensuales_personas` — FK compuesta `(asignacion_mensual_id, cartera_id)` → `asignaciones_mensuales(id, cartera_id)` garantiza coherencia de `cartera_id` en BD (no solo en Java).

**Cambios en entidad/repositorio:**
- Eliminado `AsignacionMensualPersonaId.java`.
- `AsignacionMensualPersona` — PK UUID generada en constructor, campos `fechaInicio`, `fechaFin`, `fechaActualizacion`, `@Version`. Nuevo método `cerrar(LocalDate)` con validaciones.
- `AsignacionMensualPersonaRepository` — PK cambia a UUID. Nuevos métodos: `existsByAsignacionMensualIdAndPersonaIdAndActivaTrue`, `findByAsignacionMensualIdAndPersonaIdAndActivaTrue`, `existsByPersonaIdAndCarteraIdAndActivaTrue`. Renombrado: `findAllByAsignacionMensualIdAndActivaTrue`. Bulk close: `cerrarTodasParaMensual(mid, fechaFin, ahora)` propaga `fecha_fin` y `fecha_actualizacion`.

**Cambios en servicio:**
- `agregarPersonaAMensual` — verifica solo vínculo ACTIVO (permite reincorporación tras cierre individual).
- `removerPersonaDeMensual(UUID, UUID, LocalDate)` — nuevo método: cierra solo el vínculo de la persona indicada sin afectar a las demás.
- `cerrarAsignacionMensual` — propaga `fecha_fin = LocalDate.now()` al bulk close de vínculos.
- `agregarPersonaADiaria` — verifica solo vínculo ACTIVO (personas removidas individualmente no aparecen en diarias).

**Tests:**
- 5 nuevos tests de integración: reasignación individual, historial con fecha_inicio/fecha_fin, FK compuesta (BD rechaza cartera_id incorrecto), cierre masivo con fecha_fin, reincorporación con historial preservado.
- Total: **182 pruebas — 0 failures — Modularidad PASS — BUILD SUCCESS**.

---

## [Sin versión] — 2026-08-01 — Fase 3B: Asignaciones mensuales y diarias ✅

### Corrección previa: Fase 3A — relación persona–cartera N:M

- `V008__permitir_personas_multiples_carteras.sql` — tabla `carteras_personas` con historial. Elimina `personas.cartera_id`. Índice parcial `(cartera_id, persona_id) WHERE activa = TRUE`.
- `CarteraPersona` — entidad con `cerrar(LocalDate)`, `@PrePersist`, `@PreUpdate`, `@Version`.
- `PersonaService` — `vincularCartera`, `cerrarVinculo`, `consultarCarterasActivas`, `consultarPersonasActivas`.
- `PersonaConsultaApi` — agrega `personaActivaEnCartera(UUID, UUID)`.
- `DatosPersona` — eliminado campo `carteraId`.
- `UsuarioConsultaApi` — agrega `tieneRolActivo` y `tieneSupervisionActiva`.
- `UsuarioConsultaApiImpl` — implementa los dos nuevos métodos; inyecta `SupervisionRepository`.
- Documentación: `REGLAS_NEGOCIO.md`, `MODELO_DOMINIO.md`, `MODELO_DATOS.md`, `DIAGRAMA_ENTIDAD_RELACION.md`, `DICCIONARIO_DATOS_PRELIMINAR.md`.
- 12 tests nuevos (6 unitarios + 6 integración). Total: 146.
- `InfraestructuraTest` — actualizado conteo de tablas: 15 → 16.

### Añadido en Fase 3B

**Migración Flyway:**
- `V009__crear_asignaciones.sql` — 4 tablas: `asignaciones_mensuales`, `asignaciones_mensuales_personas`, `asignaciones_diarias`, `asignaciones_diarias_personas`.
  - `uq_am_ejecutivo_cartera_activa` — un ejecutivo, una asignación mensual activa por cartera.
  - `uq_amp_persona_cartera_activa` — una persona, un ejecutivo activo por cartera (RN-04, protegido en BD).
  - `uq_ad_ejecutivo_fecha_activa` — un ejecutivo, una diaria en BORRADOR o PUBLICADA por fecha.
  - CHECKs de coherencia: PUBLICADA→fecha_publicacion, CANCELADA→motivo.

**Módulo `asignaciones` (nuevo):**
- `AsignacionMensual` — entidad raíz con `cerrar()`, `@Version`, `@PrePersist`/`@PreUpdate`.
- `AsignacionMensualPersona` + `AsignacionMensualPersonaId` — relación N:M con `carteraId` denormalizado y `activa`.
- `AsignacionDiaria` — máquina de estados: `publicar()`, `finalizar()`, `cancelar(motivo)`.
- `AsignacionDiariaPersona` + `AsignacionDiariaPersonaId` — relación N:M simple.
- `EstadoAsignacionDiaria` — enum: BORRADOR, PUBLICADA, FINALIZADA, CANCELADA.
- Excepciones: `AsignacionNoEncontradaException`, `PersonaYaEnAsignacionException`, `TransicionEstadoInvalidaException`, `PersonaFueraDeAsignacionMensualException`.
- Repositorios: `AsignacionMensualRepository`, `AsignacionMensualPersonaRepository`, `AsignacionDiariaRepository`, `AsignacionDiariaPersonaRepository`.
- `AsignacionService` — 7 operaciones con validación de roles, supervisión, cartera y membresía de persona.
- `AsignacionConsultaApi` + `AsignacionConsultaApiImpl` — API pública `@NamedInterface("api")`.

**Pruebas:**
- `AsignacionMensualDominioTest` — 5 unitarias.
- `AsignacionDiariaDominioTest` — 13 unitarias (máquina de estados completa).
- `DominioAsignacionesIntegracionTest` — 13 integración incluyendo 3 pruebas de índices BD.
- `InfraestructuraTest` — actualizado conteo de tablas: 16 → 20.
- **Total: 177 pruebas — 0 failures — BUILD SUCCESS — Modularidad PASS.**

### No implementado (fuera de alcance Fase 3B)
- Endpoints REST de asignaciones.
- `descargas_asignacion_diaria` — postergado a fase de sincronización.
- Importación CSV de asignaciones.

---

## [Sin versión] — 2026-07-28 — Fase 2: Autenticación y Sesiones ✅

### Añadido

**Migraciones Flyway:**
- `V004__crear_sesiones_tokens.sql` — tablas `sesiones_autenticacion` y `refresh_tokens`. Índices únicos parciales para una sesión activa por (usuario, dispositivo) y un refresh token activo por sesión. CHECKs de coherencia de estado.
- `V005__agregar_bloqueado_hasta.sql` — columna `bloqueado_hasta TIMESTAMPTZ` en `cobranza.usuarios` para bloqueo temporal automático.

**Módulo `dispositivos` (extensión):**
- `dispositivos/api/` como `@NamedInterface("api")`: `DatosDispositivo`, `DispositivoConsultaApi`, `DispositivoNoValidoException`.
- `DispositivoConsultaApiImpl` con validación: activo, no revocado, pertenece al usuario.

**Módulo `usuarios` (extensión):**
- `Usuario.estaHabilitadoEn(Instant)` — considera bloqueo temporal además del administrativo.
- `Usuario.registrarAccesoExitoso()` — ahora limpia `bloqueadoHasta`.
- `UsuarioConsultaApi` — nuevos métodos: `registrarIntentoFallido`, `aplicarBloqueoTemporal`, `registrarAccesoExitoso`, `buscarCredencialesPorId`.
- `CredencialesUsuario` — clase final con `@JsonIgnoreType`, sin hash en `toString()`.
- `BcryptCodificadorContrasena` — delega a `PasswordEncoder` bean inyectado (antes lo creaba con `new`).

**Módulo `autenticacion` (nuevo):**
- Dominio: `SesionAutenticacion`, `RefreshToken` con estados y transiciones.
- Infraestructura: `SesionRepository`, `RefreshTokenRepository` con consultas JPQL y `@Lock(PESSIMISTIC_WRITE)`.
- Aplicación: `PropiedadesJwt` (properties), `GestorTokens` (emisión JWT y tokens opacos), `AutenticacionService` (login, renovación atómica, logout).
- Web: `AutenticacionController` con endpoints `/login`, `/refresh`, `/logout`, `/me`. DTOs: `SolicitudLogin`, `SolicitudRenovacion`, `RespuestaToken`, `RespuestaInfoUsuario`.
- Seguridad: `SeguridadConfig` (SecurityFilterChain stateless, CSRF off), `RsaKeyConfig` (carga filesystem, `@ConditionalOnMissingBean`), `CargadorClavesRsa`.
- Configuración raíz: `SeguridadBaseConfig` (beans `PasswordEncoder` y `Clock`).

**Pruebas:**
- `AutenticacionTestConfig` — par RSA 2048-bit en memoria para pruebas.
- `AutenticacionIntegracionTest` — 8 pruebas de integración: login, credenciales incorrectas, refresh, reuso de token, logout, /me.
- `GestorTokensTest` — 8 pruebas unitarias de generación y hash de tokens.
- `PoliticoBloqueoTest` — 5 pruebas unitarias de política de bloqueo temporal.

**ADRs:**
- ADR-0022: JWT RS256 y gestión de claves RSA externas.
- ADR-0023: Refresh tokens opacos con rotación atómica.
- ADR-0024: Sesiones por par usuario-dispositivo con expiración absoluta.
- ADR-0025: Frontera de credenciales y PasswordEncoder compartido.

### Cambiado
- `InfraestructuraTest` — cuenta de tablas actualizada de 7 a 9 (V004 agrega 2 tablas).
- Tests de integración existentes — añadido `@Import(AutenticacionTestConfig.class)` para proveer RSA en memoria.
- `CodificadorContrasenaTest` — constructor actualizado a `BcryptCodificadorContrasena(PasswordEncoder)`.

### Riesgos aceptados del MVP
- Access tokens permanecen válidos hasta 15 minutos después de logout o cambio de permisos (stateless por diseño).
- Roles y permisos en `/me` pueden estar desactualizados hasta que el token expire o se renueve.

---

## [Sin versión] — 2026-07-28 — Fase 1C: Modelo físico de usuarios, roles, permisos y dispositivos ✅

### Resultado
54/54 pruebas pasan. Módulos `usuarios` y `dispositivos` implementados con límites Spring Modulith, esquema validado por Hibernate, BCrypt costo 12.

### Añadido

**Migraciones Flyway:**
- `V002__crear_seguridad_usuarios.sql` — 7 tablas: `roles`, `permisos`, `usuarios`, `usuario_roles`, `rol_permisos`, `dispositivos`, `supervision_usuarios`. Índices parciales, CHECKs de normalización y coherencia.
- `V003__cargar_roles_permisos_iniciales.sql` — 4 roles y 7 permisos con UUIDs estables. Matriz rol→permisos.

**Módulo `usuarios`:**
- Entidades JPA: `Usuario`, `Rol`, `Permiso`, `UsuarioRol`, `RolPermiso`, `RolPermisoId`, `SupervisionUsuario`.
- Repositorios: `UsuarioRepository`, `RolRepository`, `PermisoRepository`, `UsuarioRolRepository`, `RolPermisoRepository`, `SupervisionRepository`.
- Servicios: `UsuarioService`, `SupervisionService`.
- Puerto `CodificadorContrasena` + adaptador `BcryptCodificadorContrasena`.
- Interfaz pública `UsuarioConsultaApi` en `@NamedInterface("api")`.
- Excepciones de dominio: `UsuarioNoEncontradoException`, `NombreUsuarioDuplicadoException`, `CorreoDuplicadoException`, `RolNoEncontradoException`, `RelacionSupervisionInvalidaException`.

**Módulo `dispositivos`:**
- Entidad `Dispositivo` con `@Version` y `revocar()` coherente.
- `DispositivoService` que inyecta `UsuarioConsultaApi` (no cruza al dominio de usuarios).
- `DispositivoRepository`.

**Pruebas:**
- `UsuarioTest` — 7 tests unitarios.
- `SupervisionTest` — 5 tests unitarios.
- `DispositivoTest` — 4 tests unitarios.
- `CodificadorContrasenaTest` — 5 tests unitarios (BCrypt strength 4).
- `SeguridadIntegracionTest` — 23 tests de integración (Testcontainers): esquema, UUIDs estables, normalización, historial de roles, versioning, dispositivos.
- `InfraestructuraTest` — actualizado: `flyway_crea_tablas_en_esquema_cobranza()` verifica 7 tablas.

**ADRs:**
- `ADR-0018` — Roles y permisos en tablas (no enums Java).
- `ADR-0019` — `identificador_instalacion` como UUID de instalación Android.
- `ADR-0020` — BCrypt para hashing de contraseñas, costo configurable.
- `ADR-0021` — Separación entre módulo `usuarios` y módulo `autenticacion`.

**Documentación:**
- `docs/arquitectura/SEGURIDAD_USUARIOS_BASE.md` — modelo de seguridad base, invariantes, límites de módulo.

### Modificado

- `apps/api/pom.xml` — dependencia `spring-security-crypto` añadida.
- `apps/api/src/main/resources/application.yml` — `ddl-auto: validate`, `security.bcrypt.strength: 12`.
- `apps/api/src/main/java/cl/zzenner/cobranza/usuarios/package-info.java` — `@ApplicationModule(displayName = "Usuarios")`.
- `apps/api/src/test/java/cl/zzenner/cobranza/SeguridadIntegracionTest.java` — test `hibernate_no_creo_tablas_adicionales` actualizado para excluir tablas PostGIS (`spatial_ref_sys`).

### Bugs corregidos

- `SupervisionService.validarRolActivo()`: cambiado `.ifPresent()` por `.orElseThrow()` — antes la validación se saltaba silenciosamente si el rol no existía en BD.

---

## [Sin versión] — 2026-07-27 — Fase 1B: Base técnica modular de la API ✅

### Resultado
Proyecto `apps/api/` creado y verificado. 10/10 pruebas pasan. Todos los endpoints de infraestructura responden correctamente.

### Añadido

**Proyecto Maven:**
- `apps/api/pom.xml` — Spring Boot 3.5.16, Spring Modulith 1.4.12, Flyway, JPA, Actuator, Springdoc 2.8.17.
- `apps/api/mvnw` + `apps/api/.mvn/` — Maven Wrapper.

**Código fuente:**
- `CobranzaApplication.java` — clase principal.
- 11 módulos Spring Modulith con `package-info.java` + `@ApplicationModule`: autenticacion, usuarios, dispositivos, carteras, asignaciones, personas, operaciones, gestiones, sincronizacion, auditoria, compartido.
- `GlobalExceptionHandler.java` — `ProblemDetail` para validación.
- `OpenApiConfig.java` — bean OpenAPI con título y versión.

**Configuración:**
- `application.yml` — configuración base (sin datasource, ddl-auto=none, probes, springdoc).
- `application-local.yml` — datasource local desde variables de entorno.

**Base de datos:**
- `V001__crear_esquemas_base.sql` — primera migración Flyway, crea `cobranza` y `auditoria`.

**Pruebas:**
- `ModularidadTest` — verifica estructura Spring Modulith.
- `InfraestructuraTest` — Testcontainers PostGIS, esquemas Flyway, PostGIS disponible, Hibernate sin tablas.
- `ActuatorTest` — health, liveness, readiness, OpenAPI accesibles.

**OpenAPI:**
- `contracts/openapi/cobranza-api.yaml` — contrato v0.1.0 con `paths: {}`.

**Scripts:**
- `scripts/api-run.sh`, `scripts/api-test.sh`, `scripts/api-check.sh`.

**CI:**
- `.github/workflows/api-ci.yml` — Java 21, Maven Wrapper, cache, activado en `apps/api/**`.

**ADRs:**
- `docs/adr/0015-stack-tecnico-api.md` — Java 21, Spring Boot 3.5.16, Maven.
- `docs/adr/0016-flyway-propietario-esquema.md` — Flyway como propietario exclusivo del esquema.
- `docs/adr/0017-testcontainers-pruebas-integracion.md` — Testcontainers con PostGIS.

**Documentación:**
- `docs/arquitectura/API_BASE_TECNICA.md` — documentación completa de la base técnica.
- `apps/api/README.md` — guía de comandos y endpoints.

### Modificado

- `infrastructure/postgres/init/02_schemas.sql` — retirada la creación de esquemas (responsabilidad migrada a Flyway V001).
- `docs/arquitectura/MODULOS.md` — módulos actualizados con nombres correctos (operaciones en lugar de creditos, dispositivos añadido, estados actualizados).
- `docs/gestion/DEUDA_TECNICA.md` — DT-007 marcado como resuelto (DT-R06).
- `docs/gestion/STATUS.md`, `ROADMAP.md` — Fase 1B marcada como completada.

### Validación realizada

- `./mvnw verify`: 10/10 pruebas ✅
- `docker compose up -d` + `./scripts/api-run.sh`: API inicia correctamente ✅
- Flyway V001 aplicada: esquemas `cobranza` y `auditoria` creados ✅
- Endpoints verificados: `/actuator/health`, `/actuator/health/liveness`, `/actuator/health/readiness`, `/actuator/info`, `/v3/api-docs`, `/swagger-ui/index.html` ✅

---

## [Sin versión] — 2026-07-26 — Auditoría final Fase 1A ✅

### Resultado
**FASE 1A APROBADA.** Toda la documentación de dominio es consistente con las decisiones funcionales confirmadas. No se encontraron contradicciones activas. Se crearon documentos faltantes y se corrigieron tres inconsistencias menores.

### Contradicciones corregidas

| Archivo | Detalle |
|---|---|
| `docs/producto/HISTORIAS_USUARIO.md` | HU-005: eliminado "y monto" en COMPROMISO_PAGO (contradecía RF-05c y RN-12). |
| `docs/contexto/ALCANCE.md` | Sección PENDIENTE: items resueltos (reapertura offline → RN-24; operaciones descargadas → RN-10) movidos a sección "Resuelto". |
| `docs/gestion/DEUDA_TECNICA.md` | DT-001 (PIN/biometría) → DT-R04 (resuelto). DT-003 (minSdk) → DT-R05 (resuelto provisionalmente). |

### Añadido

**Documentos de dominio obligatorios:**
- `docs/dominio/DIAGRAMA_ENTIDAD_RELACION.md` — Diagrama Mermaid con 20 entidades y sus relaciones.
- `docs/dominio/DICCIONARIO_DATOS_PRELIMINAR.md` — Descripción detallada de columnas de todas las entidades candidatas.
- `docs/dominio/MATRIZ_AUTORIDAD_DATOS.md` — Tabla de autoridad por dato: sistema externo / API / Android / Admin Web.
- `docs/dominio/DECISIONES_PENDIENTES.md` — Consolidación de DP-01 a DP-08 con impacto y bloqueantes.

**ADR faltantes:**
- `docs/adr/0012-persona-como-unidad-principal.md`
- `docs/adr/0013-uuid-generado-en-dispositivo.md`
- `docs/adr/0014-copia-operacional-datos-externos.md`

### Actualizado

- `docs/gestion/ROADMAP.md` — Fase 1A registrada como completada; Fase 1B descrita con prerequisitos.
- `docs/gestion/STATUS.md` — Veredicto de auditoría, resumen de correcciones, historial de fases actualizado.

---

## [Sin versión] — 2026-07-26 — Decisiones finales Fase 1A (tercera sesión)

### Actualizado
- `docs/dominio/CICLOS_DE_VIDA.md` — Estados de asignación diaria reescritos: BORRADOR/PUBLICADA/FINALIZADA/CANCELADA(opcional); DESCARGADA eliminado como estado funcional; concepto `descarga_asignacion_diaria` introducido; diagrama de logout con operaciones pendientes actualizado con política MVP confirmada.
- `docs/dominio/MODELO_DOMINIO.md` — Estados de asignación diaria y descarga como evento técnico.
- `docs/dominio/MODELO_DATOS.md` — `asignaciones_diarias.estado` actualizado; tabla `descargas_asignacion_diaria` añadida; nota de alcance de cuotas actualizada.
- `docs/dominio/REGLAS_NEGOCIO.md` — RN-10 (alcance operaciones/cuotas), RN-22 (estados confirmados), RN-24 (política logout completa), RN-27 (minSdk provisional). PENDIENTE reducido a 8 ítems.
- `docs/sincronizacion/ESTRATEGIA_OFFLINE.md` — Sección de datos locales con alcance completo de operaciones; política logout MVP completa.
- `docs/sincronizacion/RESOLUCION_CONFLICTOS.md` — Sección de logout con operaciones pendientes actualizada con política MVP confirmada (ya no PENDIENTE).
- `docs/producto/REQUISITOS_FUNCIONALES.md` — RF-01g con política logout completa; RF-04c con alcance de operaciones activas y cuotas vigentes. PENDIENTE actualizado.
- `docs/contexto/GLOSARIO.md` — Estados asignación diaria actualizados (DESCARGADA eliminado, FINALIZADA/CANCELADA añadidos); concepto `descarga_asignacion_diaria` añadido. PENDIENTE actualizado.
- `docs/gestion/STATUS.md` — Tercera sesión de decisiones; preguntas pendientes actualizadas.

### Añadido
- `docs/adr/0011-android-minsdk-provisional.md` — ADR para decisión provisional de minSdk API 29 / Android 10.

---

## [Sin versión] — 2026-07-26 — Complemento de decisiones funcionales Fase 1A

### Actualizado
- `docs/dominio/MODELO_DOMINIO.md` — Aval simplificado a solo lectura (rut, nombre); asignación diaria con supervisor y estados preliminares; dirección reportada → observación de dirección; relaciones actualizadas.
- `docs/dominio/MODELO_DATOS.md` — Tabla `avales` simplificada; `asignaciones_diarias` con supervisor_id, estado, fecha_publicacion; `direcciones_reportadas` reemplazada por `observaciones_direccion`.
- `docs/dominio/REGLAS_NEGOCIO.md` — RN-05, RN-09, RN-17 actualizadas; nuevas RN-21 a RN-26; pendientes reducidos y re-numerados.
- `docs/sincronizacion/ESTRATEGIA_OFFLINE.md` — Sección de retención de datos locales; sección de sesión local y tokens.
- `docs/sincronizacion/PROTOCOLO_SINCRONIZACION.md` — Flujo de sincronización con paso de validación de autenticación al recuperar conectividad.
- `docs/sincronizacion/RESOLUCION_CONFLICTOS.md` — Reescritura completa: conflictos de asignación con pendientes, sesión revocada, logout con pendientes.
- `docs/producto/REQUISITOS_FUNCIONALES.md` — RF-01 completo con sesión persistente y tokens; RF-03 con supervisor; RF-05 con tipos confirmados y observación de dirección; RF-06 con retención.
- `docs/contexto/GLOSARIO.md` — Estados de asignación diaria; sesión/tokens; observación_direccion.
- `docs/gestion/STATUS.md` — Nuevas decisiones incorporadas; pendientes actualizados.

### Añadido
- `docs/dominio/CICLOS_DE_VIDA.md` — Ciclos de asignación diaria (con estados y justificación), gestión (estados técnicos de sincronización), sesión Android, y política de retención de datos locales.

---

## [Sin versión] — 2026-07-26 — Decisiones funcionales del dominio

### Actualizado
- `docs/contexto/GLOSARIO.md` — Nuevos roles (`JEFE_SUPERVISORES`, `TECNOLOGIA`, `SUPERVISOR`, `EJECUTIVO_TERRENO`), nuevos términos (asignación mensual, asignación diaria, tipos de gestión, estados de sincronización técnicos).
- `docs/dominio/MODELO_DOMINIO.md` — Modelo completo con jerarquía confirmada: `cartera → persona → operaciones → cuotas`; avales, asignaciones mensuales y diarias, gestiones, fotografías, direcciones importadas y reportadas, supervisión, dispositivos.
- `docs/dominio/MODELO_DATOS.md` — Tablas candidatas detalladas para todos los módulos confirmados; columnas `rut_numero` y `rut_dv` en `personas`.
- `docs/dominio/REGLAS_NEGOCIO.md` — 20 reglas confirmadas (RN-01 a RN-20); 7 preguntas pendientes reducidas de 13 anteriores.
- `docs/sincronizacion/ESTRATEGIA_OFFLINE.md` — Estados de sincronización actualizados a los confirmados; asignación diaria como unidad de descarga; indicadores de estado en la interfaz; sincronización manual.
- `docs/gestion/STATUS.md` — Estado actualizado con decisiones incorporadas y preguntas pendientes reducidas.

### Añadido
- `docs/adr/0007-rut-separado-numero-dv.md` — RUT almacenado como `rut_numero` + `rut_dv`.
- `docs/adr/0008-asignacion-diaria-unidad-sincronizacion.md` — Asignación diaria (~50 personas) como unidad de descarga al móvil.
- `docs/adr/0009-gestiones-inmutables.md` — Gestiones inmutables: sin rectificaciones ni anulaciones en el MVP.
- `docs/adr/0010-geolocalizacion-obligatoria.md` — Geolocalización puntual obligatoria para registrar gestiones.

---

## [Sin versión] — 2026-07-26 — Fase 0: Inicialización

### Añadido
- Estructura de directorios del monorepo (`apps/`, `contracts/`, `infrastructure/`, `docs/`, `scripts/`, `.github/`).
- Archivos raíz de configuración: `README.md`, `CLAUDE.md`, `CONTRIBUTING.md`, `.editorconfig`, `.gitattributes`, `.gitignore`, `.env.example`.
- `compose.yaml` con PostgreSQL 16 + PostGIS 3.4 y perfil `tools` para Adminer.
- Scripts de gestión del entorno local: `start.sh`, `stop.sh`, `status.sh`, `logs.sh`, `clean.sh`, `check.sh`.
- Scripts SQL de inicialización: extensiones (postgis, uuid-ossp, pg_trgm) y esquemas lógicos (`cobranza`, `auditoria`).
- Documentación de contexto: `VISION.md`, `ALCANCE.md`, `GLOSARIO.md`.
- Documentación de producto: `REQUISITOS_FUNCIONALES.md`, `REQUISITOS_NO_FUNCIONALES.md`, `HISTORIAS_USUARIO.md`.
- Documentación de arquitectura: `ARQUITECTURA_GENERAL.md`, `MODULOS.md`, `DIAGRAMA_CONTENEDORES.md`.
- Documentación de dominio: `MODELO_DOMINIO.md`, `MODELO_DATOS.md`, `REGLAS_NEGOCIO.md`.
- Documentación de sincronización: `ESTRATEGIA_OFFLINE.md`, `RESOLUCION_CONFLICTOS.md`, `PROTOCOLO_SINCRONIZACION.md`.
- Documentación de seguridad: `SEGURIDAD.md`.
- Documentación de pruebas: `ESTRATEGIA_PRUEBAS.md`.
- Documentación de operación: `DESARROLLO_LOCAL.md`, `DESPLIEGUE_VPS.md`, `BACKUP_RESTAURACION.md`.
- Documentación de gestión: `ROADMAP.md`, `STATUS.md`, `CHANGELOG.md`, `DEUDA_TECNICA.md`.
- ADR iniciales: `0001` a `0006`.
- Plantillas de GitHub: PR template, issue template de historia, issue template de error.
