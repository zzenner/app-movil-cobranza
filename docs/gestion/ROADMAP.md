# Roadmap

## Fase actual: Fase 4C-B — Búsqueda directa por RUT — IMPLEMENTADA ✅ PENDIENTE COMMIT

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

## Fase 5 — Administración web (PENDIENTE)

**Objetivo:** Crear la aplicación web Angular para la administración del sistema.

**Incluye (borrador):**
- Proyecto Angular con componentes standalone en `apps/admin-web/`.
- Módulos: usuarios, carteras, asignaciones, visualización de gestiones.
- Integración completa con la API.

---

## Fase 6 — Despliegue en VPS (PENDIENTE)

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
