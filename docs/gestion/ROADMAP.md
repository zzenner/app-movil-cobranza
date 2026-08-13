# Roadmap

## Fase actual: Fase 6B — Asignaciones Diarias y Publicación — COMPLETADA ✅

## Fase 0 — Inicialización del repositorio ✅

**Objetivo:** Establecer la estructura del monorepo, convenciones, documentación base y entorno local mínimo funcional.

**Incluye:**
- Estructura de directorios del monorepo.
- Archivos raíz: README, CLAUDE.md, CONTRIBUTING, .gitignore, .editorconfig, etc.
- Documentación inicial de contexto, producto, arquitectura, dominio, sincronización, seguridad, pruebas y operación.
- ADR iniciales (6 decisiones registradas).
- Docker Compose con PostgreSQL + PostGIS funcional.
- Scripts de gestión del entorno local.
- Plantillas de GitHub (PR, issues).

**No incluye:** código de negocio, proyectos Spring Boot, Angular ni Android.

---

## Fase 1A — Decisiones funcionales y documentación de dominio ✅

**Objetivo:** Confirmar y documentar todas las decisiones funcionales del dominio antes de iniciar la implementación técnica.

**Incluye:**
- Modelo de dominio confirmado: jerarquía `cartera → persona → operaciones → cuotas`.
- Decisiones de negocio confirmadas: roles, tipos de gestión, inmutabilidad, geolocalización, avales, observaciones de dirección, asignaciones diarias.
- ADR de dominio: ADR-0007 a ADR-0014 (11 ADR en total).
- Documentación de sincronización: estrategia offline, protocolo, resolución de conflictos.
- Ciclos de vida: asignación diaria, gestión (estados técnicos), sesión Android, retención de datos.
- Requisitos funcionales y no funcionales documentados.
- Documentos obligatorios: DIAGRAMA_ENTIDAD_RELACION.md, DICCIONARIO_DATOS_PRELIMINAR.md, MATRIZ_AUTORIDAD_DATOS.md, DECISIONES_PENDIENTES.md.
- Decisiones pendientes identificadas (DP-01 a DP-08).

**No incluye:** código de aplicación, migraciones SQL, proyectos Spring Boot, Angular ni Android.

---

## Fase 1B — Base técnica modular de la API ✅

**Objetivo:** Crear el proyecto Spring Boot con Spring Modulith y la infraestructura mínima verificable.

**Completado (2026-07-27):**
- Proyecto Spring Boot 3.5.16 + Spring Modulith 1.4.12 en `apps/api/`.
- Maven Wrapper, pom.xml con todas las dependencias.
- 11 módulos Spring Modulith como stubs con `package-info.java` y `@ApplicationModule`.
- Configuración (`application.yml`, `application-local.yml`).
- Migración Flyway V001: crea esquemas `cobranza` y `auditoria`.
- `GlobalExceptionHandler` con `ProblemDetail` para validación.
- 3 pruebas: `ModularidadTest`, `InfraestructuraTest` (Testcontainers + PostGIS), `ActuatorTest`.
- Contrato OpenAPI 3.1 vacío en `contracts/openapi/cobranza-api.yaml`.
- Scripts: `api-run.sh`, `api-test.sh`, `api-check.sh`.
- GitHub Actions CI (`api-ci.yml`).
- ADR-0015, ADR-0016, ADR-0017.

**No incluye:** endpoints de negocio, JWT, Spring Security, tablas de dominio.

---

## Fase 1C — Modelo físico de usuarios, roles, permisos y dispositivos ✅

**Objetivo:** Implementar el modelo de datos físico de autenticación y dispositivos con restricciones completas, servicios de dominio y pruebas de integración.

**Completado (2026-07-28):**
- Migración V002: 7 tablas (`roles`, `permisos`, `usuarios`, `usuario_roles`, `rol_permisos`, `dispositivos`, `supervision_usuarios`) con CHECKs, índices parciales e invariantes de negocio.
- Migración V003: 4 roles y 7 permisos con UUIDs estables, matriz de permisos por rol.
- Módulo `usuarios`: entidades JPA, repositorios, `UsuarioService`, `SupervisionService`, BCrypt port/adapter.
- Módulo `dispositivos`: entidad `Dispositivo`, `DispositivoService`, límite Spring Modulith (`allowedDependencies = "usuarios::api"`).
- `UsuarioConsultaApi` como `@NamedInterface("api")` para cruce de módulo sin acoplamiento interno.
- 54 pruebas pasan (unitarias + integración Testcontainers + Spring Modulith verify).
- ADR-0018, ADR-0019, ADR-0020, ADR-0021.
- `SEGURIDAD_USUARIOS_BASE.md`.

**No incluye:** login, JWT, refresh tokens, endpoints HTTP expuestos.

---

## Fase 2 — Autenticación JWT y sesiones ✅

**Completado (2026-07-29):**
- Autenticación con JWT RS256 (access token 15 min).
- Refresh tokens opacos con rotación y revocación.
- Sesiones de dispositivo asociadas al access token.
- Spring Security con filtro JWT, `GlobalExceptionHandler`.
- Migraciones V004–V006: `sesiones`, `refresh_tokens`, `configuracion_sistema`.
- ADR-0022, ADR-0023, ADR-0024, ADR-0025.

---

## Fase 3A — Dominio de cobranza: carteras, personas, créditos ✅

**Completado:**
- Módulo `personas`: entidad, repositorio, `PersonaConsultaApi`.
- Módulo `carteras`: `CarterapersonsaRelation`, asignaciones mensuales.
- Módulo `operaciones`: créditos, cuotas.
- Migraciones V007–V008.

---

## Fase 3B — Asignaciones diarias ✅

**Completado (2026-07-31):**
- Módulo `asignaciones`: `AsignacionDiaria`, `AsignacionMensual`, estados (`BORRADOR`, `PUBLICADA`, `FINALIZADA`, `CANCELADA`).
- Consulta de asignaciones por ejecutivo (`AsignacionConsultaApi`).
- Migración V009.
- 155+ pruebas integración.

---

## Fase 3C — Gestiones de cobranza ✅ CERRADA (v0.7.0-gestiones)

**Completado (2026-08-01):**
- Módulo `gestiones`: entidad inmutable, dos orígenes (`ASIGNACION_DIARIA` / `BUSQUEDA_DIRECTA`).
- `GestionService` con idempotencia atómica: fast-path `findById` + `INSERT … ON CONFLICT (id) DO NOTHING`.
- `GestionConflictivaException` para mismo UUID con contenido distinto.
- Validación: origen coherente con `asignacion_diaria_id`; diaria en estado `PUBLICADA` o `FINALIZADA`; GPS obligatorio; `COMPROMISO_PAGO` requiere `fecha_compromiso`.
- `fecha_gestion` generada en dispositivo; `fecha_creacion_servidor` generada en servidor.
- Migración V010.
- 210 pruebas (0 failures, 0 errors); Spring Modulith verify PASS.
- ADR-0026 a ADR-0030.

---

## Fase 3D — API REST de asignaciones y gestiones ✅ Validada — Lista para cierre

**Objetivo:** Exponer los endpoints HTTP que consume la app Android para sincronizar asignaciones y enviar gestiones.

**Completado (2026-08-01):**
- `GET /api/v1/asignaciones/diaria/activa` — bundle completo (personas, direcciones, avales, operaciones ACTIVA, cuotas, últimas 10 gestiones). Parámetro `?fecha` opcional. 204 si no hay asignación activa.
- `POST /api/v1/gestiones` — recepción idempotente: 201 INSERTADA / 200 IDEMPOTENTE / 409 GESTION_CONFLICTIVA. Sin header Location.
- `@PreAuthorize("hasRole('EJECUTIVO_TERRENO')")` en ambos endpoints.
- Estrategia bulk: 8 queries IN en total, sin N+1.
- `GlobalExceptionHandler` con ProblemDetail uniforme (code, timestamp, path).
- `OpenApiConfig` con SecurityScheme Bearer JWT.
- 27 tests REST (12 descarga + 15 gestiones). **237 pruebas — 0 failures.**

---

## Fase 4A — Base Android: autenticación y almacenamiento seguro ✅

**Objetivo:** Crear el proyecto Android con la capa de red, seguridad y autenticación completa, sin funcionalidades de cartera ni gestiones.

**Completado (2026-08-02):**
- Toolchain: AGP 9.3.0, Gradle 9.6.1, Kotlin 2.4.10, KSP 2.3.10, `compileSdk/targetSdk=37`, `minSdk=29`.
- Corrección API: `SolicitudLogin.dispositivoId` → `identificadorInstalacion`; auto-registro de dispositivo post-login.
- `:core:network` — cliente público + autenticado, `SingleFlightAuthenticator` (Mutex), `TokenProvider`.
- `:core:security` — `InstallationIdStore` (DataStore) + `SecureTokenStore` (Keystore AES-256-GCM).
- `:feature:auth` — `SessionRepository`, `LoginViewModel`, `AuthState`, pantallas Check/Login/Home.
- Backup deshabilitado: `allowBackup=false` + `data_extraction_rules.xml`.
- Pruebas JVM: 5 suites — 38 tests, 0 failures.
- CI: `.github/workflows/android-ci.yml`.
- ADR-0031 (identificadorInstalacion), ADR-0032 (stack técnico Android).

---

## Fase 4B — Cartera offline ✅ CERRADA — tag v0.10.0-descarga-offline

**Objetivo:** Implementar descarga y visualización offline de la asignación diaria.

**Completado (2026-08-02):**
- `:core:database` — Room 2.7.2 con 9 entidades, 8 DAOs, `BundleReplacementTransaction` atómica, `SyncMetadataEntity` con 5 estados.
- `:core:network` — DTOs de sincronización (`SyncModels.kt`), `BigDecimalSerializer`, `SincronizacionApi`.
- `:feature:asignacion` — `AsignacionRepository` (Mutex single-flight), `DescargaAsignacionWorker` (@HiltWorker), `AsignacionSyncScheduler`, `AsignacionViewModel`, pantallas `AsignacionListScreen` y `PersonaDetalleScreen`.
- `:feature:auth` — `SessionRepository` elevado a `@Singleton`; `AuthModule` con `@Binds`.
- `:app` — NavHost completo, `LogoutUseCase`, `CobranzaApp` con `Configuration.Provider`.
- WorkManager 2.10.1 con inicialización manual (HiltWorkerFactory).
- Pruebas: `core:database` + `feature:asignacion` = 31+ tests, 0 failures.
- ADR-0033, ADR-0034, ADR-0035, ADR-0036.

**No incluye:**
- Outbox de gestiones pendientes.
- GPS.
- Fotografías diferidas.

---

## Fase 4C-A — Gestiones offline ASIGNACION_DIARIA ✅ IMPLEMENTADA

**Objetivo:** Registro y sincronización offline de gestiones desde la asignación diaria activa.

**Implementado:**
- `:core:database` Room v2 — tabla `gestion_local` con outbox pattern, lease atómico, backoff exponencial, migración 1→2 no destructiva.
- `:core:network` — `GestionApi`, `GestionDtos`.
- `:feature:gestion` — `GestionRepository` (outbox + Mutex), `AndroidLocationProvider` (LocationManager, timeout 30s), `EnvioGestionWorker` (@HiltWorker), `GestionSyncScheduler`, pantallas Compose form + historial.
- `HomeViewModel.EstadoLogout` — bloqueo de logout con gestiones pendientes, sin "salir igualmente".
- 6 estados de sincronización: `PENDIENTE_ENVIO`, `ENVIANDO`, `SINCRONIZADA`, `ERROR_REINTENTABLE`, `ERROR_PERMANENTE`, `CONFLICTO`.
- ADR-0037..0040 creados.
- 135 tests JVM — 0 failures.

**No incluye:**
- BUSQUEDA_DIRECTA (requiere endpoint API global por RUT — pendiente Fase 4C-B).
- Fotografías (diferidas — ADR-0030).

## Fase 4C-B — Búsqueda directa por RUT ✅ IMPLEMENTADA

**Objetivo:** Permitir gestionar personas fuera de la asignación diaria buscando por RUT.

**Completado (2026-08-03):**
- Endpoint API `POST /api/v1/personas/busquedas` — RUT en body por privacidad; `Cache-Control: no-store`. Rol: `EJECUTIVO_TERRENO`.
- `RutValidacionApi` en `personas.api` — validación sin exponer tipo `Rut` interno entre módulos Spring Modulith.
- `BusquedaPersonaService` en módulo `sincronizacion` — orquesta persona + operaciones + gestiones.
- Room v3: `persona_directa` (snapshot JSON offline), `asignacionDiariaId` nullable en `gestion_local`.
- `MIGRATION_2_3` no destructiva (25 columnas explícitas, recreación de tabla, sin `fallbackToDestructiveMigration`).
- `:feature:busqueda` — `RutValidator` (Módulo 11), `BusquedaDirectaRepository`, `BusquedaDirectaViewModel`, `BusquedaDirectaScreen`.
- `GestionForm.origenGestion` — campo explícito (no inferido); validator cross-valida origen ↔ asignacionDiariaId.
- `HomeScreen` — botón "Buscar persona por RUT".
- Navegación: solo `personaId` en rutas (sin PII en back stack).
- 21 tests API nuevos; 22 tests Android nuevos.
- ADR-0041, ADR-0042.

---

## Fase 5A — Base del administrador web y autenticación ✅ IMPLEMENTADA

**Objetivo:** Establecer la base técnica del panel administrativo Angular y separar la autenticación web de Android.

**Completado (2026-08-03):**
- `V011__autenticacion_web.sql` — `tipo_cliente` en `sesiones_autenticacion`; `dispositivo_id` nullable para WEB; CHECKs; índices parciales `uq_sesiones_activa_android` / `uq_sesiones_activa_web`.
- Endpoints web: `POST /api/v1/auth/web/login`, `/web/refresh` (cookie HttpOnly), `/web/logout`. Sin `identificadorInstalacion`.
- Cookie `rt_web`: HttpOnly, SameSite=Strict, Path=/api/v1/auth/web/refresh. Access token solo en body.
- `WebOriginValidationFilter` — valida `Origin`/`Referer` en `/web/refresh` y `/web/logout`; rechaza origen incorrecto con 403.
- JWT: `did` ausente en WEB, claim `tipo_cliente` en todos los tokens. `/me` corregido con `dispositivoId` nullable.
- Proyecto Angular 22.1.0: TypeScript 6.0.2 (`strict: true`), Angular Material 22.1.0, Vitest 4.1.10, Playwright 1.62.1.
- Autenticación Angular: access token en memoria, bootstrap con `APP_INITIALIZER`, guards funcionales (`authGuard`, `loginGuard`, `roleGuard`), single-flight refresh.
- Layout: sidenav Material, toolbar con logout. Login y Home con perfil real desde `/me`.
- Proxy dev: `/api/**` → `localhost:8080`. CI: `.github/workflows/admin-web-ci.yml`.
- 288 tests API (0 failures). 30 tests Vitest. 6 tests Playwright. Cobertura ≥80% en auth, http, guards.
- ADR-0043, ADR-0044, ADR-0045.

---

## Entorno Docker local completo ✅ IMPLEMENTADO (previa a 5B-2)

**Objetivo:** Levantar la plataforma completa en Docker para pruebas manuales locales.

**Entregado:**
- `compose.yaml` actualizado: servicios `api` y `admin-web`. PostgreSQL + PostGIS + API Spring Boot + Nginx/Angular en un único `docker compose up --build -d`.
- Dockerfiles multi-stage: API (eclipse-temurin:21-jre-alpine + su-exec), Admin Web (Node 24 + Nginx 1.27).
- Nginx: SPA fallback, proxy `/api → api:8080`, `Origin` conservado, headers de seguridad.
- `DevSeedRunner` + `UsuarioSeedApi` + `UsuarioSeedService` — seed idempotente respetando Spring Modulith.
- Scripts: `generar-claves.sh`, `levantar-entorno.sh`, `smoke-test.sh` (24 pruebas).
- `.env.example` actualizado, `infrastructure/dev-keys/` gitignored.
- Documentación: `docs/operacion/DOCKER_LOCAL.md`.
- **329 tests API — 0 failures. 50 tests Angular — 0 failures. 24/24 smoke tests.**

## Fase 5B-1 — Consulta administrativa de usuarios (solo lectura) ✅ IMPLEMENTADA

**Objetivo:** Primera funcionalidad administrativa real: consultar usuarios del sistema desde el panel web.

**Entregado:**
- API `GET /api/v1/admin/usuarios` — listado paginado con filtros (nombreUsuario, estado, rol). Orden: nombreUsuario ASC.
- API `GET /api/v1/admin/usuarios/{id}` — detalle completo (roles vigentes, permisos efectivos, supervisor, estado calculado).
- Estado calculado con precedencia: `INACTIVO → BLOQUEADO → BLOQUEADO_TEMPORAL → ACTIVO`. Clock inyectable.
- Batch queries (sin N+1): 4 consultas fijas por página, independiente del tamaño.
- Permiso `PERM_USUARIOS_VER`: acceso para JEFE_SUPERVISORES y TECNOLOGIA. SUPERVISOR y EJECUTIVO_TERRENO reciben 403.
- Angular: `features/usuarios` lazy-loaded, `permissionGuard` basado en `permisos[]`, listado con filtros y paginación, detalle con rol/supervisor.
- ADR-0046. OpenAPI actualizado. 323 tests API, 50 tests Vitest, 14 tests Playwright.

## Fase 5B-2 — Gestión administrativa de usuarios (escritura) ✅ IMPLEMENTADA

**Completado (2026-08-06):**
- API: `POST /api/v1/admin/usuarios`, `PUT /api/v1/admin/usuarios/{id}/datos-basicos`, y 5 endpoints de estado (activar, desactivar, bloquear, desbloquear, restablecer-contrasena). Permiso: `USUARIOS_ADMINISTRAR`.
- `GET /api/v1/admin/roles` — catálogo de roles para formulario de creación.
- Creación con roles iniciales (sin asignación posterior — reservado a 5B-3/5C si aplica).
- Protección "último administrador activo" (409 si la operación dejaría el sistema sin ningún admin).
- Restricción de auto-bloqueo/desactivación (409 si el actor opera sobre sí mismo).
- Revocación atómica de sesiones y refresh tokens al desactivar, bloquear o restablecer contraseña (`SeguridadUsuarioModificadaEvent` cross-módulo).
- Locking optimista: campo `version` en detalle; PUT verifica versión, 409 si conflicto.
- Angular: formularios crear/editar, diálogos confirmar/resetPassword, acciones en detalle, botón "Nuevo usuario" en listado.
- Bug fix: `Usuario.desbloquear()` ahora limpia `bloqueadoHasta`.
- Security fix: `AutenticacionService.renovar()` y `renovarWeb()` verifican estado del usuario antes de emitir tokens.
- ADR-0047, ADR-0048. 371 tests API (0 failures). Specs Angular nuevos: confirm-action-dialog, reset-password-dialog, usuario-create, usuario-edit, usuario-detail (acciones admin).

**No incluye:**
- Importación mensual de datos de cobranza (personas, operaciones, cuotas, asignaciones) — corresponde a Fase 5C.
- Asignación/remoción de roles post-creación.
- Asignación de supervisor.
- Eliminación física de usuarios.
- Tablas de auditoría persistente (auditoría via logs estructurados).

---

## Fase 5C — Importación mensual administrativa de datos (PENDIENTE)

**Objetivo:** Cargar mensualmente los datos de cobranza desde el sistema corporativo al sistema de cobranza móvil.

**Incluye (borrador):**
- Importación de personas (deudores y avales).
- Importación de operaciones y cuotas.
- Importación de asignaciones (supervisor → ejecutivo → cartera).
- Carga mediante CSV generado por el sistema corporativo.
- Validación previa de la carga con reporte de errores antes de confirmar.
- Trazabilidad de la importación: quién importó, cuándo, cuántos registros afectados.
- Reversión por lote si la validación detecta inconsistencias críticas.

**No corresponde a:** importación de usuarios (los usuarios se crean manualmente via Fase 5B-2).

---

## Fase 5D–5N — Módulos administrativos adicionales (PENDIENTE)

**Incluye (borrador):**
- Gestión de carteras y asignaciones.
- Visualización de gestiones registradas.
- Gestión de dispositivos Android.
- Dashboard de estado de sincronización.

---

## Fase 6B — Asignaciones Diarias y Publicación ✅ COMPLETADA — tag v0.21.0-asignaciones-diarias

**Objetivo:** Completar el flujo administrativo de asignaciones: Supervisor crea borrador diario, selecciona personas, publica.

**Completado (2026-08-13):**
- `V016` — `publicado_por_id` en `asignaciones_diarias`; permisos de asignaciones asignados a TECNOLOGIA.
- `AsignacionAdminQueryService` — consultas nativas de períodos, mensuales, personas disponibles, listado y detalle de diarias.
- `AsignacionAdminService` — operaciones: crearBorrador, actualizarPersonas, publicar (auditoría), cancelar.
- `AsignacionAdminController` — 9 endpoints en `/api/v1/admin/asignaciones` con permisos `ASIGNACIONES_VER` / `ASIGNACIONES_ADMINISTRAR`.
- Angular `features/asignaciones` — lista, creación (stepper), detalle con publicación y cancelación.
- Android: sin cambios — endpoint `GET /api/v1/asignaciones/diaria/activa` ya existía; solo devuelve asignaciones PUBLICADAS.
- 29/29 unit tests API, 196/196 Angular tests, ng build limpio.
- ADR pendiente: ninguno nuevo (decisiones ya documentadas en ADR-0008, RN-21, RN-22).

**No incluye:**
- Finalización de asignaciones (FINALIZADA se activa cuando el ejecutivo sincroniza al final del día — flujo Android existente).
- Fotografías.
- Despliegue en VPS.

---

## Fase 7 — Despliegue en VPS (PENDIENTE)

**Objetivo:** Desplegar el sistema completo en un VPS Ubuntu con Docker Compose y Nginx.

**Incluye (borrador):**
- `compose.prod.yaml` para producción.
- Nginx como proxy inverso con TLS.
- Scripts de despliegue y actualización.
- Backup automatizado de PostgreSQL.
- Monitoreo básico.

---

## Fases futuras (no comprometidas)

- Integración con sistema corporativo mayor de cobranza.
- Almacenamiento de fotografías compatible con S3.
- Generación de rutas de cobranza.
- Reportería avanzada.
- Revocación de dispositivos Android.
- Notificaciones push.
