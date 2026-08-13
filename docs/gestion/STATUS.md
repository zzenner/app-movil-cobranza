# Estado del proyecto

**Última actualización:** 2026-08-13
**Fase actual:** Fase 6B — Asignaciones Diarias y Publicación — COMPLETADA ✅ — tag v0.21.0-asignaciones-diarias
**Fase anterior:** Fase 6A — Carteras y Supervisión — COMPLETADA ✅ — tag v0.20.0-carteras-supervision

## Resumen

| Item                                                      | Estado                |
|-----------------------------------------------------------|-----------------------|
| Estructura del monorepo                                   | Completado            |
| Documentación base (Fase 0)                               | Completado            |
| Decisiones funcionales del dominio (Fase 1A)              | Completado            |
| ADR de dominio (0001–0014)                                | Completado            |
| Documentos obligatorios de dominio                        | Completado            |
| Docker Compose (PostgreSQL)                               | Completado            |
| Scripts de entorno local                                  | Completado            |
| Plantillas GitHub                                         | Completado            |
| API — base técnica (Fase 1B)                              | Completado ✅          |
| **API — modelo físico usuarios/roles/dispositivos (1C)**  | **Completado ✅**      |
| **API — autenticación y sesiones (Fase 2)**               | **Completado ✅**      |
| **API — corrección persona-cartera (Fase 3A fix)**        | **Completado ✅**      |
| **API — asignaciones mensuales y diarias (Fase 3B)**      | **Completado ✅**      |
| **API — gestiones de cobranza (Fase 3C — validada)**      | **Completado ✅**      |
| **API — REST asignaciones y gestiones (Fase 3D)**         | **Validada ✅ — Lista para cierre**      |
| Admin Web (Angular) — Fase 5A base + autenticación        | **Completado ✅** |
| **Admin Web (Angular) — Fase 5B-1 consulta usuarios**     | **CERRADA ✅ — tag v0.14.0** |
| **Entorno Docker local (PostgreSQL + API + Admin Web)**    | **IMPLEMENTADO ✅ — tag v0.15.0-entorno-docker-local** |
| **App Android — Fase 4A (base auth + red + seguridad)**   | **Completado ✅**                   |
| **App Android — Fase 4B (cartera offline Room + WorkManager)** | **Cerrada ✅**               |
| **App Android — Fase 4C-A (gestiones ASIGNACION_DIARIA offline)**  | **Cerrada ✅**          |
| **App Android + API — Fase 4C-B (búsqueda directa por RUT)**       | **Implementada ✅ — pendiente commit** |
| **Admin Web — Fase 5B-2 gestión usuarios (escritura)**             | **VALIDADA ✅ — LISTA PARA CIERRE** |
| **API + Admin Web — Fase 5C importación mensual**                  | **VALIDADA ✅ — 404/404 tests** |
| **API + Admin Web + OpenAPI — Fase 5D contrato CSV v2 (26 columnas)** | **CERRADA ✅ — commit 9133f49 (pendiente push)** |
| **API + Admin Web — Fase 5E importación real (Win-1252, periodo, V014)** | **COMPLETADA ✅ — 9839/9839 filas, 0 errores** |
| **API + Admin Web — Fase 6A Carteras y Supervisión**              | **COMPLETADA ✅ — tag v0.20.0-carteras-supervision** |
| **App Android — Conectividad API local corregida**                 | **COMPLETADA ✅ — commit f1a1b22** |
| **API + Admin Web — Fase 6B Asignaciones Diarias y Publicación**   | **COMPLETADA ✅ — tag v0.21.0-asignaciones-diarias** |
| Despliegue en VPS                                                  | No iniciado           |

## Resultado de auditoría Fase 1A (2026-07-26)

**Veredicto: FASE 1A APROBADA**

Documentos revisados: todos los de `docs/dominio/`, `docs/sincronizacion/`, `docs/producto/`, `docs/contexto/`, todos los ADR.

**Correcciones aplicadas:**
- `HISTORIAS_USUARIO.md` — eliminado "monto" en COMPROMISO_PAGO (contradicción con RF-05c, RN-12).
- `ALCANCE.md` — pendientes resueltos movidos a sección "Resuelto" (reapertura offline, alcance de operaciones).
- `DEUDA_TECNICA.md` — DT-001 y DT-003 movidos a deuda resuelta (DT-R04, DT-R05).

**Documentos creados en auditoría:**
- `docs/dominio/DIAGRAMA_ENTIDAD_RELACION.md`
- `docs/dominio/DICCIONARIO_DATOS_PRELIMINAR.md`
- `docs/dominio/MATRIZ_AUTORIDAD_DATOS.md`
- `docs/dominio/DECISIONES_PENDIENTES.md`
- `docs/adr/0012-persona-como-unidad-principal.md`
- `docs/adr/0013-uuid-generado-en-dispositivo.md`
- `docs/adr/0014-copia-operacional-datos-externos.md`

**Sin contradicciones activas.** Ver detalles en CHANGELOG.md.

## Bloqueantes activos

Sin bloqueantes activos.

## Fase 5C — Validación final completada (2026-08-10)

**Resultado: Fase 5C VALIDADA. 404/404 tests API, 148/148 tests Angular, 40/40 Playwright, Docker 3 servicios healthy, smoke test 49/49 OK.**

| Item | Resultado |
|---|---|
| Endpoint POST /api/v1/admin/importaciones/mensuales → 202 Accepted | ✅ |
| Workflow async RECIBIDA→VALIDANDO→VALIDADA→PROCESANDO→COMPLETADA | ✅ |
| 33 tests ImportacionAdminRestTest (Testcontainers) — 2 corridas sin flakiness | ✅ |
| 404/404 tests API totales — BUILD SUCCESS — 0 failures | ✅ |
| Flyway V012 aplicada (importaciones_mensuales, errores_importacion, DATOS_IMPORTAR) | ✅ |
| V012 expande ck_operaciones_estado (VIGENTE, VENCIDO, CASTIGADO) | ✅ |
| V012 agrega uq_cp_persona_activa (RN-03 revisado) | ✅ |
| Tests pre-existentes actualizados (conteos de tablas/permisos, RN-03) | ✅ |
| Angular importacion-detail component con polling y panel CONFIRMAR | ✅ |
| Angular specs — 54 tests DT-IMX-002 resuelto — cobertura ≥80% | ✅ |
| Angular 148/148 tests totales — ng build OK — npm audit 0 high | ✅ |
| Playwright 14 escenarios importación [INTERCEPTADO] — 40/40 total | ✅ |
| Docker compose build — ambas imágenes compiladas sin errores | ✅ |
| Docker compose up — postgres + api + admin-web healthy | ✅ |
| Volumen cobranza_importaciones creado y accesible | ✅ |
| V012 en DB verificada: 23 tablas, constraints correctos | ✅ |
| Smoke test sección 8 — DT-IMX-003 resuelto — 49/49 OK | ✅ |
| Persistencia tras restart API — DB intacta, API UP | ✅ |
| ADR-0049 (workflow importación) y ADR-0050 (patrón async) creados | ✅ |
| FORMATO_IMPORTACION_MENSUAL.md documentado | ✅ |

## Fase 1B completada (2026-07-27)

**Resultado: API base técnica funcional. 10/10 pruebas pasan.**

| Item | Resultado |
|---|---|
| Proyecto Maven compilado | ✅ |
| 11 módulos Spring Modulith verificados | ✅ |
| Flyway V001 aplicada (esquemas cobranza + auditoria) | ✅ |
| Testcontainers con PostGIS | ✅ |
| `/actuator/health` UP | ✅ |
| `/actuator/health/liveness` UP | ✅ |
| `/actuator/health/readiness` UP | ✅ |
| `/v3/api-docs` — título cobranza-api | ✅ |
| `/swagger-ui/index.html` HTTP 200 | ✅ |
| GitHub Actions CI (`api-ci.yml`) | ✅ |
| ADR-0015, ADR-0016, ADR-0017 | ✅ |

**Incidencias resueltas durante la implementación:**
- `postgis/postgis:16-3.4` requiere `.asCompatibleSubstituteFor("postgres")` en Testcontainers (no es la imagen PostgreSQL oficial).
- Springdoc no usa `spring.application.name` como título; se requiere bean `OpenAPI` explícito.
- `02_schemas.sql` modificado para ceder la responsabilidad de esquemas a Flyway (ADR-0016).

---

## Decisiones funcionales incorporadas (tercera sesión, 2026-07-26)

| Decisión                                                             | Documento de referencia                          |
|----------------------------------------------------------------------|--------------------------------------------------|
| Estados asignación diaria CONFIRMADOS: BORRADOR/PUBLICADA/FINALIZADA/CANCELADA(opcional) | RN-22, CICLOS_DE_VIDA, GLOSARIO |
| DESCARGADA eliminado como estado funcional; descarga = evento técnico `descarga_asignacion_diaria` | RN-22, MODELO_DATOS, CICLOS_DE_VIDA |
| Alcance descarga: TODAS las operaciones activas + TODAS las cuotas vencidas + futuras vigentes | RN-10, RF-04c, ESTRATEGIA_OFFLINE |
| No se descargan: operaciones anuladas, cerradas sin saldo, completamente pagadas | RN-10, MODELO_DATOS            |
| Política logout MVP CONFIRMADA: bloquear si pendientes+sin red; sincronizar si pendientes+con red | RN-24, RF-01g, RESOLUCION_CONFLICTOS, CICLOS_DE_VIDA |
| Android minSdk PROVISIONAL: API 29 / Android 10 (pendiente inventario) | RN-27, ADR-0011                 |

## Decisiones funcionales incorporadas (segunda sesión, 2026-07-26)

| Decisión                                                 | Documento de referencia         |
|----------------------------------------------------------|---------------------------------|
| Supervisor crea y publica la asignación diaria           | RN-21, MODELO_DOMINIO, RF-03c   |
| Estados de asignación diaria: BORRADOR/PUBLICADA/DESCARGADA/CERRADA (preliminares) | RN-22, CICLOS_DE_VIDA |
| Retención de datos locales: no borrar pendientes         | RN-23, ESTRATEGIA_OFFLINE       |
| Sesión local persistente: no PIN/biometría en MVP        | RN-24, ESTRATEGIA_OFFLINE       |
| Access token + refresh token como conceptos separados    | RN-24, PROTOCOLO_SINCRONIZACION |
| Logout advierte sobre pendientes; no borra silenciosamente | RN-24, CICLOS_DE_VIDA         |
| `observacion_direccion` como entidad simple (no corrección activa) | RN-25, MODELO_DATOS  |
| Avales: solo lectura mínima (rut_numero, rut_dv, nombre) | RN-26, MODELO_DATOS             |

## Decisiones funcionales incorporadas (primera sesión, 2026-07-26)

Las siguientes decisiones fueron confirmadas y están documentadas:

- **Estructura de cobranza:** `cartera → persona → operaciones → cuotas`. Unidad principal: persona.
- **RUT:** almacenado como `rut_numero` + `rut_dv` (ADR-0007).
- **Asignación mensual:** ~300–400 personas, carga por CSV, historial conservado.
- **Asignación diaria:** ~50 personas, base de la ruta, unidad de descarga al móvil (ADR-0008).
- **Roles:** `JEFE_SUPERVISORES`, `TECNOLOGIA`, `SUPERVISOR`, `EJECUTIVO_TERRENO`.
- **Tipos de gestión:** `CONTACTO_FAMILIAR`, `COMPROMISO_PAGO`, `SIN_CONTACTO`.
- **Gestiones inmutables:** sin rectificaciones ni anulaciones en el MVP (ADR-0009).
- **Geolocalización obligatoria:** sin coordenadas no se permite guardar (ADR-0010).
- **Estados de sincronización:** `PENDIENTE_ENVIO`, `ENVIANDO`, `SINCRONIZADA`, `ERROR_REINTENTABLE`, `ERROR_PERMANENTE`.
- **Compromiso sin monto:** solo fecha de compromiso en `COMPROMISO_PAGO`.
- **Búsqueda:** local en asignación diaria + global por API con conexión.
- **Direcciones reportadas:** conservan dirección original, no la sobrescriben.
- **Estado de dispositivos:** visible en la aplicación administrativa.

## Preguntas todavía pendientes

| ID   | Pregunta                                                                                                      |
|------|---------------------------------------------------------------------------------------------------------------|
| ~~P-01~~ | ~~¿El ejecutivo puede registrar gestiones sobre personas fuera de su asignación diaria activa?~~ **Resuelta: dos orígenes (ADR-0026)** |
| P-02 | ¿Los ejecutivos ven gestiones de otros ejecutivos sobre la misma persona en la app Android?                  |
| P-03 | ¿Cuál es el catálogo completo futuro de tipos de gestión? (los tres iniciales están confirmados)             |
| P-04 | ¿Se implementa exportación a Excel en la Fase 1 o en una posterior?                                         |
| ~~P-05~~ | ~~¿Puede el supervisor modificar una asignación `PUBLICADA` o debe crear una nueva?~~ **Resuelta Fase 6B: PUBLICADA es inmutable; se cancela y crea nueva** |
| ~~P-06~~ | ~~¿Se implementa `CANCELADA` como estado de asignación diaria en el MVP?~~ **Resuelta Fase 6B: sí, CANCELADA implementada** |
| P-07 | Confirmar minSdk definitivo con inventario de dispositivos corporativos (provisional: API 29 / Android 10).  |
| P-08 | Confirmar si el aval se asocia a persona u operación en el sistema externo definitivo (no bloqueante).       |

## Fase 1C completada (2026-07-28)

**Resultado: Modelo físico de usuarios, roles, permisos y dispositivos. 54/54 pruebas pasan.**

| Item | Resultado |
|---|---|
| V002: 7 tablas en `cobranza` schema | ✅ |
| V003: 4 roles y 7 permisos con UUIDs estables | ✅ |
| Módulo `usuarios` con entidades JPA | ✅ |
| Módulo `dispositivos` con límite Spring Modulith | ✅ |
| `UsuarioConsultaApi` como `@NamedInterface("api")` | ✅ |
| BCrypt costo 12 (costo 4 en pruebas) | ✅ |
| `ddl-auto: validate` — Hibernate no crea tablas | ✅ |
| 54 pruebas (unit + integración + modularidad) | ✅ |
| ADR-0018, ADR-0019, ADR-0020, ADR-0021 | ✅ |
| `SEGURIDAD_USUARIOS_BASE.md` | ✅ |

## Fase 2 — Autenticación y Sesiones (2026-07-28)

### Implementado

| Item | Estado |
|---|---|
| V004: tablas `sesiones_autenticacion` y `refresh_tokens` | ✅ |
| V005: columna `bloqueado_hasta` en `usuarios` | ✅ |
| `dispositivos::api` — `DispositivoConsultaApi`, `DatosDispositivo` | ✅ |
| `usuarios::api` — `CredencialesUsuario`, métodos de estado en `UsuarioConsultaApi` | ✅ |
| Módulo `autenticacion` — dominio, infraestructura, aplicación, web | ✅ |
| `SecurityFilterChain` — stateless, CSRF deshabilitado, Bearer JWT | ✅ |
| `JwtDecoder` RS256 con validación de firma, fechas, issuer, audience | ✅ |
| `JwtEncoder` — claves RSA desde sistema de archivos externo | ✅ |
| `AutenticacionService` — login, renovación atómica, logout idempotente | ✅ |
| Política de bloqueo — 5 intentos, 30 min, no incrementar en bloqueo activo | ✅ |
| Rotación de refresh token con detección de reutilización | ✅ |
| `AutenticacionTestConfig` — par RSA en memoria para pruebas | ✅ |
| ADR-0022 a ADR-0025 | ✅ |
| 4 endpoints: POST /login, POST /refresh, POST /logout, GET /me | ✅ |

### Pendiente de ejecución

- Ejecutar `mvn test` con Docker disponible para validar todos los tests.
- Generar par RSA 2048-bit externo y configurar variables de entorno en el servidor.

## Fase 3A (corrección) y 3B completadas (2026-08-01)

### Fase 3A fix: relación persona–cartera N:M

| Item | Resultado |
|---|---|
| V008: tabla `carteras_personas` (N:M con historial) | ✅ |
| `CarteraPersona` — entidad con `cerrar()` y auditoría | ✅ |
| `PersonaService` — `vincularCartera`, `cerrarVinculo`, `consultarCarterasActivas` | ✅ |
| `PersonaConsultaApi` — `personaActivaEnCartera()` | ✅ |
| `DatosPersona` — eliminado `carteraId` | ✅ |
| 6 tests unitarios dominio CarteraPersona | ✅ |
| 6 tests integración multi-cartera | ✅ |

### Fase 3B: asignaciones mensuales y diarias (+ corrección historial individual)

| Item | Resultado |
|---|---|
| V009: 4 tablas de asignaciones | ✅ |
| `asignaciones_mensuales_personas` — PK UUID, historial por persona | ✅ |
| `asignaciones_mensuales_personas` — FK compuesta garantiza `cartera_id` en BD | ✅ |
| `asignaciones_mensuales_personas` — `fecha_inicio`, `fecha_fin`, `fecha_actualizacion`, `@Version` | ✅ |
| `AsignacionMensual` — `UNIQUE(id, cartera_id)` como ancla de FK compuesta | ✅ |
| `AsignacionMensualPersona` — `cerrar(LocalDate)` con validaciones | ✅ |
| `AsignacionService.removerPersonaDeMensual` — cierre individual sin afectar al resto | ✅ |
| `AsignacionService.cerrarAsignacionMensual` — propaga `fecha_fin` a vínculos | ✅ |
| `AsignacionMensual`, `AsignacionDiaria` — entidades con lógica de estado | ✅ |
| `AsignacionService` — 8 operaciones con validaciones | ✅ |
| `AsignacionConsultaApi` — interfaz pública `@NamedInterface("api")` | ✅ |
| `UsuarioConsultaApi` — `tieneRolActivo`, `tieneSupervisionActiva` | ✅ |
| 5 tests unitarios `AsignacionMensualDominioTest` | ✅ |
| 13 tests unitarios `AsignacionDiariaDominioTest` | ✅ |
| 18 tests integración `DominioAsignacionesIntegracionTest` | ✅ |
| Modularidad Spring Modulith — PASS | ✅ |
| **182 pruebas — 0 failures** | ✅ |

## Fase 3C completada (2026-08-01) — corregida idempotencia atómica

### Módulo gestiones — Recepción idempotente de gestiones de cobranza

| Item | Resultado |
|---|---|
| V010: tabla `gestiones` (append-only, sin fecha_actualizacion) | ✅ |
| `OrigenGestion`, `TipoGestion` — enums de dominio | ✅ |
| `Gestion` — entidad inmutable, UUID generado en dispositivo | ✅ |
| `GestionConflictivaException` — excepción de dominio | ✅ |
| `GestionService.recibirGestion()` — idempotente, validaciones completas | ✅ |
| `GestionRepository.insertarSiNoExiste()` — `INSERT ... ON CONFLICT (id) DO NOTHING` | ✅ |
| Idempotencia atómica: fast-path findById + insert atómico | ✅ |
| `GestionConsultaApi` — interfaz pública `@NamedInterface("api")` | ✅ |
| `AsignacionConsultaApi` — extendida con `findAsignacionDiaria` y `personaEnAsignacionDiaria` | ✅ |
| 25 tests de integración (idempotencia, concurrencia, conflicto, persistencia) | ✅ |
| Prueba concurrente 5 hilos mismo UUID — 0 errores, 1 fila | ✅ |
| Prueba concurrente 2 hilos UUID conflictivo — 1 éxito, 1 GestionConflictivaException | ✅ |
| fecha_creacion_servidor no cambia en reintento idempotente | ✅ |
| fecha_creacion_servidor generada en servidor, distinta de fecha_gestion dispositivo | ✅ |
| Conflicto no modifica gestión original (persona_id en BD verificado) | ✅ |
| Modularidad Spring Modulith — PASS | ✅ |
| **210 pruebas — 0 failures — 0 errors — BUILD SUCCESS** | ✅ |
| ADR-0026 a ADR-0030 (ADR-0027 actualizado con estrategia ON CONFLICT) | ✅ |
| MODELO_DATOS, MODELO_DOMINIO, DIAGRAMA_ER, DICCIONARIO, REGLAS_NEGOCIO, RF actualizados | ✅ |

## Fase 3D validada (2026-08-01) ✅ LISTA PARA CIERRE

### Endpoints REST de descarga de asignación y recepción de gestiones

| Item | Resultado |
|---|---|
| `GET /api/v1/asignaciones/diaria/activa` — descarga bundle completo | ✅ |
| `POST /api/v1/gestiones` — recepción idempotente (201/200/409) | ✅ |
| `@PreAuthorize("hasRole('EJECUTIVO_TERRENO')")` en ambos endpoints | ✅ |
| Ejecutivo identificado por `sub` del JWT; no se acepta del cliente | ✅ |
| `DescargaAsignacionService` — 8 queries IN sin N+1 | ✅ |
| Bundle: personas, direcciones vigentes, avales, operaciones ACTIVA, cuotas VENCIDA/VIGENTE/FUTURA (excluye PAGADA), últimas 10 gestiones | ✅ |
| Parámetro `?fecha` opcional; default = hoy en America/Santiago (Clock inyectable) | ✅ |
| 204 No Content cuando no hay asignación activa | ✅ |
| `GestionConflictivaException` movida a `gestiones::api` | ✅ |
| `ResultadoRecepcion` enum — `INSERTADA` / `IDEMPOTENTE` | ✅ |
| `PersonaNoEncontradaException` → 404; `AsignacionDiariaNoEncontradaException` → 404 | ✅ |
| `GestionConflictivaException` → 409; `IllegalArgumentException` → 400; `IllegalStateException` → 400 | ✅ |
| `SeguridadConfig` — 401/403 con ProblemDetail uniforme (code, timestamp, path) | ✅ |
| `GlobalExceptionHandler` — ProblemDetail uniforme con code, timestamp, path | ✅ |
| `OpenApiConfig` — Bearer JWT SecurityScheme (`bearerAuth`) | ✅ |
| `contracts/openapi/cobranza-api.yaml` — contrato completo con ambos endpoints | ✅ |
| `apps/api/README.md` — estructura de módulos y endpoints actualizados | ✅ |
| `docs/arquitectura/MODULOS.md` — estados de módulos actualizados | ✅ |
| Spring Modulith — modularidad PASS | ✅ |
| 12 tests REST asignaciones (incluyendo aserciones de cuerpo 401/403) | ✅ |
| 16 tests REST gestiones (incluyendo BORRADOR→400 y aserciones de cuerpo 401/403) | ✅ |
| **238 pruebas — 0 failures — 0 errors — BUILD SUCCESS** | ✅ |

## Fase 4A — Base Android (2026-08-02) — PENDIENTE DE REVISIÓN

### Corrección de contrato API

| Item | Resultado |
|---|---|
| `SolicitudLogin.dispositivoId` → `identificadorInstalacion` (UUID string generado en Android) | ✅ |
| `DispositivoConsultaApi.buscarORegistrar()` — auto-registro post-validación de credenciales | ✅ |
| `AutenticacionService.login()` — llama a `buscarORegistrar` en lugar de `buscarPorId` | ✅ |
| `AutenticacionIntegracionTest` — reescrito; 8 nuevos tests (245 pruebas, 0 failures) | ✅ |
| ADR-0031 — `identificadorInstalacion` como campo de login | ✅ |

### Proyecto Android creado (`apps/mobile-android/`)

| Módulo | Item | Resultado |
|---|---|---|
| `:app` | CobranzaApp (@HiltAndroidApp), MainActivity, grafo de navegación | ✅ |
| `:core:network` | ApiModels, AuthApi, TokenProvider, NetworkModule (dos clientes), SingleFlightAuthenticator | ✅ |
| `:core:security` | InstallationIdStore (DataStore), SecureTokenStore (Keystore AES-256-GCM) | ✅ |
| `:feature:auth` | AuthState, ErrorTipo, SessionRepository (@ActivityRetainedScoped), LoginViewModel, pantallas, navegación | ✅ |
| Configuración | libs.versions.toml, Gradle 9.6.1, AGP 9.3.0, Kotlin 2.4.10, KSP 2.3.10 | ✅ |
| Seguridad | allowBackup=false, data_extraction_rules.xml (excluye todos los dominios) | ✅ |
| Pruebas JVM | SingleFlightAuthenticatorTest, SecureTokenStoreTest, InstallationIdStoreTest, LoginViewModelTest, SessionRepositoryTest | ✅ |
| CI | .github/workflows/android-ci.yml (build + lint + test JVM) | ✅ |
| ADR-0032 | Stack técnico Android (AGP, Kotlin, Hilt, Retrofit, Keystore) | ✅ |

### Auditoría y validación (2026-08-02) ✅

| Item | Resultado |
|---|---|
| **API — TOCTOU en `buscarORegistrar()`** | ✅ Corregido con INSERT ON CONFLICT DO NOTHING |
| **API — Tests concurrentes** | ✅ 2 tests de concurrencia añadidos (29 en `AutenticacionIntegracionTest`) |
| **API — Suite completa** | ✅ **247 tests — 0 failures** |
| **Android SDK instalado** | ✅ `platforms;android-37.1`, `build-tools;37.0.0` |
| **Gradle wrapper** | ✅ eval+xargs para DEFAULT_JVM_OPTS (AGP 9.x estándar) |
| **AGP 9.0 compatibilidad** | ✅ Eliminado `kotlin.android`, eliminado `kotlinOptions {}` |
| **compileSdk/targetSdk 36→37** | ✅ Todos los módulos actualizados |
| **Recursos Android base** | ✅ strings.xml, themes.xml, mipmap-anydpi-v26 icons |
| **`SessionRepository.verificarSesionInicial()`** | ✅ Flow collection corregida |
| **`LoginViewModel` — estado error** | ✅ Eliminado `setNoAutenticado()` redundante tras `setError()` |
| **`assembleDebug`** | ✅ BUILD SUCCESSFUL |
| **lint** | ✅ BUILD SUCCESSFUL — sin errores |
| **testDebugUnitTest** | ✅ **38 tests JVM — 0 failures** |
| **assembleDebugAndroidTest** | ✅ BUILD SUCCESSFUL |
| **connectedDebugAndroidTest** | ⏭️ No ejecutado (sin emulador en WSL2) |
| **SecureTokenStore auditado** | ✅ AES-256-GCM, IV único, Keystore-backed, backup excluido |
| **Logging auditado** | ✅ Solo HEADERS, sin credenciales en logs |
| **SingleFlightAuthenticator auditado** | ✅ Mutex + stale-token comparison |
| **CI workflow corregido** | ✅ `platforms;android-37.1`, `build-tools;37.0.0` |

## Fase 4B — Cartera offline (2026-08-02) — CERRADA ✅

### Room + WorkManager + descarga de asignación diaria

| Item | Resultado |
|---|---|
| `libs.versions.toml` — Room 2.7.2, WorkManager 2.10.1, Robolectric 4.14.1 | ✅ |
| `:core:database` — 9 entidades Room (asignacion_diaria, persona, asignacion_persona, direccion, aval, operacion, cuota, gestion_historica, sync_metadata) | ✅ |
| `:core:database` — Room versión 1; `exportSchema = true` | ✅ |
| `:core:database` — Esquema exportado en `apps/mobile-android/core/database/schemas/cl.zzenner.cobranza.core.database.CobranzaDatabase/1.json` | ✅ |
| `:core:database` — Sin `fallbackToDestructiveMigration` — las migraciones son explícitas | ✅ |
| `:core:database` — DAOs con `Flow<T>` para consultas reactivas; búsqueda por RUT en `PersonaDao` | ✅ |
| `:core:database` — `BundleReplacementTransaction.reemplazar()` — DELETE hijos→padres, INSERT padres→hijos, UPDATE metadata en una sola transacción | ✅ |
| `:core:database` — `BundleReplacementTransaction.marcarSinAsignacion()` — 204: preserva datos anteriores, marca `datosMarcadosComoDesactualizados = true` | ✅ |
| `:core:database` — `BundleReplacementTransaction.limpiarTodo()` — logout: vacía todas las tablas | ✅ |
| `:core:database` — `SyncMetadataEntity` (singleton id=1): estado, fechaConsultada, asignacionId, ultimaDescargaExitosa, error, flags desactualizados | ✅ |
| `:core:database` — TypeConverters (BigDecimal→String, Instant→Long, LocalDate→String) | ✅ |
| `:core:database` — FK con ON DELETE CASCADE; `PRAGMA foreign_keys = ON` en `onOpen` | ✅ |
| `:core:database` — 4 suites de pruebas (DatabaseSchemaTest, BundleReplacementTransactionTest, PersonaDaoTest, SyncMetadataDaoTest) | ✅ |
| `:core:network` — `SyncModels.kt` (DTOs de descarga), `BigDecimalSerializer`, `SincronizacionApi` | ✅ |
| `:feature:asignacion` — `AsignacionRepository` con Mutex single-flight; 200/204/401/403/5xx/IOException | ✅ |
| `:feature:asignacion` — `DescargaAsignacionWorker` (@HiltWorker) + `AsignacionSyncScheduler` (KEEP, CONNECTED, EXPONENTIAL 30s) | ✅ |
| `:feature:asignacion` — WorkManager periódico cada 4h; inmediato en login/restauración/inicio/manual | ✅ |
| `:feature:asignacion` — `AsignacionViewModel` con `combine` + `stateIn(WhileSubscribed)` | ✅ |
| `:feature:asignacion` — `AsignacionListScreen` (lista con búsqueda por RUT/nombre), `PersonaDetalleScreen` (detalle con operaciones y cuotas) | ✅ |
| `:feature:asignacion` — Consulta local offline: funciona sin red (Room como fuente de verdad) | ✅ |
| `:feature:asignacion` — 5 suites de pruebas (AsignacionRepositoryTest, DescargaAsignacionWorkerTest, AsignacionViewModelTest, AsignacionMapperTest, BigDecimalSerializerTest) | ✅ |
| `:feature:auth` — `SessionRepository` cambiado a `@Singleton`; `AuthModule` con `@Binds` | ✅ |
| `:feature:auth` — `authNavGraph` como extensión de `NavGraphBuilder` | ✅ |
| `:app` — `CobranzaApp` implementa `Configuration.Provider` (WorkManager manual) | ✅ |
| `:app` — `CobranzaNavGraph.kt` — NavHost completo con auth, home, asignacion/lista, asignacion/persona/{id} | ✅ |
| `:app` — `LogoutUseCase` — cancela workers → limpia Room → cierra sesión (best-effort: `runCatching` en logout remoto) | ✅ |
| `:app` — `LogoutUseCaseTest` (4 tests mock) + `LogoutIntegrationTest` (4 tests Room/Robolectric) | ✅ |
| `AndroidManifest.xml` — WorkManager auto-init deshabilitado (`tools:node="remove"`) | ✅ |
| ADR-0033 (Room), ADR-0034 (WorkManager), ADR-0035 (bundle atómico), ADR-0036 (arquitectura feature:asignacion) | ✅ |
| **API — `./mvnw clean verify`** | ✅ **248 tests — 0 failures — BUILD SUCCESS** |
| **Android — `assembleDebug`** | ✅ BUILD SUCCESSFUL |
| **Android — `lint`** | ✅ BUILD SUCCESSFUL |
| **Android — `testDebugUnitTest`** | ✅ **97 tests JVM — 0 failures** |
| **Android — `assembleDebugAndroidTest`** | ✅ APK compilado |
| **Android — `connectedDebugAndroidTest`** | ⏭️ No ejecutado — sin emulador en WSL2 |
| **Tag** | `v0.10.0-descarga-offline` |

## Fase 4C-A — Gestiones offline ASIGNACION_DIARIA — IMPLEMENTADA ✅

### Implementado (135 tests — 0 failures)

| Item | Resultado |
|---|---|
| Room v2: tabla `gestion_local` sin FK a persona, campos desnormalizados | ✅ |
| `GestionLocalDao` — CAS atómico, lease recovery, FIFO, contarNoResueltas | ✅ |
| `MIGRATION_1_2` — addMigrations explícita, sin fallbackToDestructiveMigration | ✅ |
| `feature:gestion` — dominio, datos, GPS, worker, DI, UI completo | ✅ |
| `GestionRepository` — outbox + Mutex single-flight + backoff exponencial | ✅ |
| `AndroidLocationProvider` — LocationManager (sin FusedLocation), timeout 30s | ✅ |
| `EnvioGestionWorker` — @HiltWorker, ExistingWorkPolicy.KEEP | ✅ |
| `HomeViewModel.EstadoLogout` — bloqueo logout, sin "salir igualmente" | ✅ |
| `BundleReplacementTransaction.reemplazar()` preserva `gestion_local` | ✅ |
| HTTP 400/403/404 → ERROR_PERMANENTE (else branch + tests explícitos) | ✅ |
| 5 estados no-SINCRONIZADA verificados en contarNoResueltas | ✅ |
| ADR-0037..0040 creados | ✅ |
| Android assembleDebug + lint + 135 tests JVM — BUILD SUCCESSFUL | ✅ |

### No incluye (pendiente Fase 4C-B)

- BUSQUEDA_DIRECTA global por RUT (requiere endpoint API inexistente)
- Fotografías

## Fase 4C-B — Búsqueda directa por RUT (2026-08-03) — IMPLEMENTADA ✅

### API — Nuevo endpoint POST /api/v1/personas/busquedas

| Item | Resultado |
|---|---|
| `RutValidacionApi` — interfaz en `personas.api` (no expone tipo `Rut` interno) | ✅ |
| `RutValidacionServicio` — implementación interna en `personas.aplicacion` | ✅ |
| `BusquedaPersonaService` — en módulo `sincronizacion`; orquesta persona + ops + gestiones | ✅ |
| `SolicitudBusquedaPersona` — record con `rutNumero` + `rutDv` (RUT en body por privacidad) | ✅ |
| `RespuestaBusquedaPersona` — envoltura `{ version, generadoEn, persona }` | ✅ |
| `RutInvalidoEnBusquedaException` con código estable `RUT_INVALIDO` | ✅ |
| `BusquedaPersonaController` — `@PostMapping`, `@PreAuthorize`, `Cache-Control: no-store` | ✅ |
| Auditoría vía log estructurado `[BUSQUEDA_AUDITORIA]` (sin tabla adicional) | ✅ |
| 21 tests de integración (seguridad, validación RUT, 404, 200, envelope, Cache-Control) | ✅ |
| **API — 269 pruebas — 0 failures** | ✅ |

### Android — Room v3, feature:busqueda, integración

| Item | Resultado |
|---|---|
| `CobranzaDatabase` v3: `PersonaDirectaEntity` añadida; `asignacionDiariaId` nullable | ✅ |
| `MIGRATION_2_3` — tabla `gestion_local` recreada (25 columnas explícitas) + `persona_directa` | ✅ |
| `PersonaDirectaDao` — upsert, findById, findByRut, deleteAll | ✅ |
| Schema exportado `3.json` — entidades: 11 (incluye `persona_directa`) | ✅ |
| `GestionLocalEntity.asignacionDiariaId: String?` (era NOT NULL) | ✅ |
| `BundleReplacementTransaction.limpiarTodo()` — añadido `personaDirectaDao.deleteAll()` | ✅ |
| `GestionForm.origenGestion: OrigenGestion` — campo explícito | ✅ |
| `GestionValidator` — cross-validación origen ↔ asignacionDiariaId (nuevo `OrigenIncoherente`) | ✅ |
| `GestionMapper` — usa `form.origenGestion.name` (no hardcodeado) | ✅ |
| `GestionFormViewModel` — dos ramas init: asignación vs. persona_directa | ✅ |
| `:feature:busqueda` (módulo nuevo) — RutValidator, BusquedaDirectaRepository, BusquedaDirectaViewModel, BusquedaDirectaScreen, BusquedaNavigation | ✅ |
| `BusquedaDtos.kt` + `PersonaBusquedaApi` + `NetworkModule.providePersonaBusquedaApi` | ✅ |
| `HomeScreen` — botón "Buscar persona por RUT" | ✅ |
| `CobranzaNavGraph` — rutas `busqueda/directa` y `gestion/form/busqueda/{personaId}` | ✅ |
| 22 tests nuevos (RutValidatorTest × 13, BusquedaDirectaViewModelTest × 9) | ✅ |
| **Android — 165 pruebas JVM — 0 failures** | ✅ |

### ADRs creados

| ADR | Título |
|---|---|
| ADR-0041 | Endpoint de búsqueda con POST por privacidad del RUT |
| ADR-0042 | Snapshot en Room v3 para búsqueda directa |

## Fase 5A — Base del administrador web y autenticación (2026-08-03) — VALIDADA ✅ PENDIENTE COMMIT

### Auditoría final Fase 5A — resultados

| Item | Resultado |
|---|---|
| **Git state** — rama `feature/fase-5-admin-web`, sin conflictos | ✅ |
| **V011** — migración no destructiva; ANDROID existente no afectado | ✅ |
| **JWT Android** — `did` presente, `tipo_cliente=ANDROID` | ✅ (tests existentes) |
| **JWT WEB** — `did` ausente, `tipo_cliente=WEB` | ✅ (tests dedicados) |
| **Origin/Referer validation** — `WebOriginValidationFilter`: refresh y logout rechazan origen incorrecto | ✅ |
| **Tests de origen** — sin Origin→403, origen malo→403, Referer válido→200 | ✅ (5 tests nuevos) |
| **`app.web.allowed-origin=${WEB_ALLOWED_ORIGIN:http://localhost:4200}`** | ✅ |
| **`.env.example`** — `WEB_ALLOWED_ORIGIN` y `WEB_COOKIE_SECURE` documentados | ✅ |
| **Angular — RoleGuard** — `roleGuard` funcional, redirige a /forbidden | ✅ |
| **Angular — bootstrap tests** — éxito, fallo, sin error propagado | ✅ (3 tests nuevos) |
| **Angular — interceptor tests** — add Bearer, no-intercept auth paths, retry-401, no-loop | ✅ (6 tests nuevos) |
| **Angular — authGuard tests** — AUTENTICADA pasa, NO_AUTENTICADA→/login | ✅ (2 tests nuevos) |
| **Angular — loginGuard tests** — NO_AUTENTICADA pasa, AUTENTICADA→/home | ✅ (2 tests nuevos) |
| **Angular — roleGuard tests** — rol presente, rol ausente→/forbidden, sin rol, no autenticado→/login | ✅ (4 tests nuevos) |
| **Angular — logout best-effort** — limpia estado en error HTTP | ✅ |
| **Angular — single-flight reset** — segundo refresh genera nueva solicitud | ✅ |
| **Cobertura core/auth** — 96.66% statements, 100% branches | ✅ |
| **Cobertura core/http** — 88.88% statements, 100% branches | ✅ |
| **Cobertura guards** — 100% statements, 85.71% branches | ✅ |
| **OpenAPI YAML** — `RespuestaInfoUsuario` corregido (sesionId, dispositivoId nullable, tipoCliente, permisos) | ✅ |
| **OpenAPI YAML** — `RespuestaLoginWeb` schema añadido | ✅ |
| **OpenAPI YAML** — endpoints web/login, web/refresh, web/logout documentados | ✅ |
| **admin-web/README.md** — contenido específico del proyecto (no boilerplate CLI) | ✅ |
| **npm audit --audit-level=high** — 0 vulnerabilidades high/critical | ✅ |
| **ng build** — BUILD SUCCESSFUL — 326.93 kB main | ✅ |
| **Playwright E2E** — 6 tests intercept — 6 passed | ✅ |
| **API — `./mvnw clean verify`** | ✅ **288 pruebas — 0 failures — BUILD SUCCESS** |
| **Angular — `npm run test:ci`** | ✅ **30 tests — 0 failures** |
| **git diff --check** | ✅ sin problemas de espaciado |

### API — Autenticación web separada de Android

| Item | Resultado |
|---|---|
| V011: `tipo_cliente` en `sesiones_autenticacion`, `dispositivo_id` nullable para WEB | ✅ |
| CHECK: ANDROID requiere dispositivo, WEB no | ✅ |
| Índices parciales independientes por tipo_cliente | ✅ |
| `SesionAutenticacion` — constructor WEB, `esWeb()`, `getTipoCliente()` | ✅ |
| `GestorTokens` — claim `did` condicional, claim `tipo_cliente` | ✅ |
| `SesionRepository` — `findActivaWebByUsuarioId()` | ✅ |
| `AutenticacionService` — `loginWeb()`, `renovarWeb()` | ✅ |
| `AutenticacionWebController` — login, refresh, logout | ✅ |
| Refresh token en cookie `rt_web` HttpOnly; SameSite=Strict | ✅ |
| `WebOriginValidationFilter` — valida Origin/Referer en refresh y logout | ✅ |
| `SeguridadConfig` — web endpoints en permitAll | ✅ |
| `/me` corregido: `did` nullable, `tipo_cliente` en respuesta | ✅ |
| 19 tests de integración web (14 funcionales + 5 origen) | ✅ |
| **API — 288 pruebas — 0 failures — BUILD SUCCESS** | ✅ |
| ADR-0043, ADR-0044, ADR-0045 | ✅ |

### Angular 22.1.0 — Panel administrativo base

| Item | Resultado |
|---|---|
| Proyecto creado: Angular 22.1.0, TypeScript 6.0.2, Angular Material 22.1.0 | ✅ |
| Vitest 4.1.10 (integrado en `@angular/build`) — sin Karma | ✅ |
| Playwright 1.62.1 | ✅ |
| `TokenStorageService` — access token solo en memoria | ✅ |
| `AuthService` — signals, single-flight refresh | ✅ |
| `SessionBootstrapService` — `APP_INITIALIZER` | ✅ |
| `authInterceptor` funcional — Bearer + retry 401 + protección bucle | ✅ |
| `authGuard` / `loginGuard` / `roleGuard` funcionales — esperan INICIALIZANDO | ✅ |
| `LayoutComponent` — sidenav + toolbar con logout | ✅ |
| `LoginComponent` — formulario reactivo, error, loading | ✅ |
| `HomeComponent` — perfil real desde `/me` | ✅ |
| `app.routes.ts` — rutas con lazy loading | ✅ |
| `proxy.conf.json` — `/api/**` → localhost:8080 | ✅ |
| **30 tests unitarios Vitest — 0 failures** | ✅ |
| **6 tests E2E Playwright — 6 passed** | ✅ |
| `ng build` — BUILD SUCCESSFUL | ✅ |
| CI: `.github/workflows/admin-web-ci.yml` | ✅ |

## Fase 5B-1 — Consulta administrativa de usuarios (2026-08-05) — IMPLEMENTADA ✅

### API — Endpoint de consulta de usuarios (solo lectura)

| Item | Resultado |
|---|---|
| `EstadoUsuario` enum — ACTIVO, BLOQUEADO_TEMPORAL, BLOQUEADO, INACTIVO | ✅ |
| `UsuarioAdminService` — listado paginado, detalle, calcularEstado con Clock | ✅ |
| Batch queries — 1 page query + roles + supervisión + nombres (sin N+1) | ✅ |
| JPA Specification con EXISTS subquery para filtro por rol | ✅ |
| `UsuarioAdminController` — `GET /api/v1/admin/usuarios` + `GET /api/v1/admin/usuarios/{id}` | ✅ |
| `@PreAuthorize("hasAuthority('PERM_USUARIOS_VER')")` en clase — JEFE_SUPERVISORES, TECNOLOGIA | ✅ |
| SUPERVISOR y EJECUTIVO_TERRENO → 403 (no tienen USUARIOS_VER) | ✅ |
| Estado calculado: precedencia INACTIVO → BLOQUEADO → BLOQUEADO_TEMPORAL → ACTIVO | ✅ |
| Validación manual pagina/tamanio → 400 (no 422) | ✅ |
| Sin exposición de contrasenaHash, version, tokens, sesiones, dispositivos | ✅ |
| `EstadoUsuarioTest` — 7 tests unitarios | ✅ |
| `UsuarioAdminRestTest` — 28 tests de integración | ✅ |
| **API: 323 pruebas — 0 failures — BUILD SUCCESS** | ✅ |
| ADR-0046 — política de acceso a consultas administrativas de usuarios | ✅ |

### Angular — Pantallas de consulta de usuarios

| Item | Resultado |
|---|---|
| `permission.guard.ts` — guard funcional basado en permiso (no rol) | ✅ |
| `usuario.models.ts` — tipos TypeScript completos | ✅ |
| `usuarios.service.ts` — HttpClient, HttpParams condicionales | ✅ |
| `UsuariosListComponent` — tabla Material, filtros, paginación, query params | ✅ |
| `UsuarioDetailComponent` — detalle, roles vigentes, permisos efectivos, supervisor | ✅ |
| Rutas lazy con `authGuard + permissionGuard`, `data: { permission: 'USUARIOS_VER' }` | ✅ |
| `LayoutComponent` — enlace Usuarios visible solo con USUARIOS_VER | ✅ |
| OpenAPI actualizado — schemas y endpoints documentados | ✅ |
| **Angular: 50 tests Vitest — 0 failures** | ✅ |
| **Playwright: 14 tests — 14 passed** | ✅ |
| **npm audit --audit-level=high** — 0 vulnerabilidades high/critical | ✅ |
| **ng build** — BUILD SUCCESSFUL | ✅ |

## Entorno Docker local — Implementado (2026-08-05)

### Servicios

| Servicio     | Imagen                      | Puerto host | Estado |
|--------------|-----------------------------|-------------|--------|
| PostgreSQL   | postgis/postgis:16-3.4      | 5432        | ✅ |
| API          | eclipse-temurin:21-jre-alpine | 8081      | ✅ |
| Admin Web    | nginx:1.27-alpine           | 8080        | ✅ |

### Archivos creados

| Archivo | Descripción |
|---------|-------------|
| `apps/api/Dockerfile` | Multi-stage: deps → build → runtime JRE alpine, UID 1000 |
| `apps/api/.dockerignore` | Excluye target/, test/, .env, *.pem |
| `apps/api/src/main/resources/application-docker.yml` | Perfil docker: datasource, Flyway, Actuator probes |
| `apps/api/src/main/java/.../DevSeedRunner.java` | Seed idempotente perfil docker (vía UsuarioSeedApi) |
| `apps/api/src/main/java/.../usuarios/api/UsuarioSeedApi.java` | Puerto público para seed |
| `apps/api/src/main/java/.../usuarios/aplicacion/UsuarioSeedService.java` | Implementación del seed |
| `apps/admin-web/Dockerfile` | Multi-stage: Node 24 → Nginx 1.27 |
| `apps/admin-web/.dockerignore` | Excluye node_modules, dist, e2e |
| `apps/admin-web/nginx.conf` | SPA fallback + proxy /api → api:8080 + nginx-health |
| `compose.yaml` | Actualizado: api + admin-web + adminer con perfil tools |
| `scripts/generar-claves.sh` | Genera par RSA 2048 en infrastructure/dev-keys/ |
| `scripts/levantar-entorno.sh` | Verifica .env + claves + levanta + espera healthy |
| `scripts/smoke-test.sh` | 15 pruebas automatizadas (infra, SPA, proxy, auth) |
| `docs/operacion/DOCKER_LOCAL.md` | Documentación operacional completa |
| `.env.example` | Variables Docker: puertos, seed, claves |
| `.gitignore` | Excluye infrastructure/dev-keys/ |

### Validación

| Item | Resultado |
|------|-----------|
| Spring Modulith — DevSeedRunner vía UsuarioSeedApi | ✅ |
| API — `./mvnw clean verify` | ✅ **329 tests — 0 failures** |
| Angular — `npm run test:ci` | ✅ **50 tests — 0 failures** |
| `docker compose config` | ✅ |
| `docker compose build` (api + admin-web) | ✅ |
| `docker compose up -d` — 3 servicios healthy | ✅ |
| DevSeedRunner — usuario admin.local creado | ✅ |
| Smoke tests automatizados | ✅ **15/15 OK** |

## Fase 5D — Contrato CSV v2 definitivo (2026-08-12/13) — CERRADA ✅ COMMIT 9133f49

**Resultado: 435/435 tests API, 148/148 tests Angular, 14/14 Playwright importacion, 69/69 smoke, Docker healthy, OpenAPI documentado.**

| Item | Resultado |
|---|---|
| V013 — `codigo_origen` en `carteras` + siembra 4 carteras con UUIDs fijos | ✅ |
| V013 — `codigo_ejecutivo_origen` en `usuarios` | ✅ |
| V013 — `marca_judicial` en `carteras_personas` | ✅ |
| V013 — `cartera_id` y `periodo` nullable en `importaciones_mensuales` | ✅ |
| V013 — índice único `(sistema_origen, numero_operacion)` en `operaciones` | ✅ |
| `FilaCsv.java` — 27 campos (26 CSV + numeroFila); incluye `periodo`, `codigoCartera`, `marcaJudicial`, `codigoEjecutivo` | ✅ |
| `CsvImportacionParser.java` — UTF-8 estricto (`CodingErrorAction.REPORT`), fechas YYYY-MM-DD, PERIODO YYYY-MM, `CARTERAS_VALIDAS` | ✅ |
| `ValidadorIntraArchivo.java` — clave posición: PERIODO+RUT+OP+CUOTA+CARTERA | ✅ |
| `ImportacionService.java` — eliminado `carteraId`/`periodo` del endpoint; idempotencia por hash | ✅ |
| `ImportacionPersistenciaService.java` — `resolverCarteras()`, `resolverEjecutivos()`, `upsertCarteraPersona()` con marca_judicial | ✅ |
| `ImportacionAdminController.java` — endpoint sin `carteraId`/`periodo` | ✅ |
| `ImportacionAdminRestTest.java` — 33 tests incluyendo sucesión de períodos (Order 91) | ✅ |
| Fixtures: todos en UTF-8, 26 columnas, YYYY-MM-DD | ✅ |
| Angular `importacion.service.ts` — `crear()` sin `carteraId`/`periodo` (contrato v2) | ✅ |
| Angular `importacion-nueva.component.ts` — formulario sin selector cartera ni campo período | ✅ |
| Angular tests — 148/148 — 0 failures | ✅ |
| Playwright importacion — 14/14 — todos verdes | ✅ |
| `contracts/openapi/cobranza-api.yaml` — 5 endpoints de importacion documentados (contrato v2) | ✅ |
| `FORMATO_IMPORTACION_MENSUAL.md` — reescrito para contrato v2 de 26 columnas | ✅ |
| Maven clean verify — 435/435 — 0 failures — 2 ejecuciones consecutivas | ✅ |
| Docker stack — postgres/api/admin-web healthy | ✅ |
| Smoke-test — 69/69 OK | ✅ |

## Fase 6B — Asignaciones Diarias y Publicación (2026-08-13) — APROBADA ✅

**Resultado: API + Admin Web + OpenAPI + Playwright validados end-to-end. 486/486 API tests (dos corridas consecutivas verdes). 196/196 Angular tests. 52/52 Playwright. 79/79 smoke test.**

| Item | Resultado |
|---|---|
| `V016__asignaciones_admin.sql` — columna `publicado_por_id`; permiso ASIGNACIONES_ADMINISTRAR a TECNOLOGIA | ✅ |
| `AsignacionDiaria.publicar(UUID)` — registra quién publicó | ✅ |
| `AsignacionAdminQueryService` — periodos, mensuales, personas disponibles, diarias, detalle | ✅ |
| `AsignacionAdminService` — crearBorrador, actualizarPersonas, publicar, cancelar | ✅ |
| `AsignacionAdminController` — `/api/v1/admin/asignaciones` — 9 endpoints REST | ✅ |
| Permisos: ASIGNACIONES_VER (lectura) / ASIGNACIONES_ADMINISTRAR (escritura + publicación) | ✅ |
| `AsignacionAdminRestTest` — 25 escenarios de integración — todos verdes | ✅ |
| Fix: `toInstant(Object)` helper — PostgreSQL devuelve TIMESTAMPTZ como OffsetDateTime | ✅ |
| Fix: `row[2].toString()` — PostgreSQL devuelve VARCHAR(1) como Character | ✅ |
| Fix: token claims `roles`/`permisos` como List.of() para SeguridadConfig | ✅ |
| Fix: RUT DV corregidos en fixtures de test (25000001→DV4, 25000002→DV2) | ✅ |
| Angular: models, service, list, create, detail, routes, layout — 196/196 specs | ✅ |
| Angular: ng build sin warnings; npm audit 0 high (nanoid actualizado) | ✅ |
| OpenAPI YAML: 9 schemas + 9 paths de `/api/v1/admin/asignaciones` documentados | ✅ |
| Android JVM tests: 31/31 tests feature:asignacion — BUILD SUCCESS | ✅ |
| Playwright: 12 escenarios asignaciones [INTERCEPTADO] — 52/52 total | ✅ |
| Docker: `up --force-recreate` — 3 contenedores healthy | ✅ |
| Docker: `build` falló por TLS corporativo WSL2 (nginx:alpine) — bloqueante entorno, no código | ⚠️ |
| Smoke test sección 9: 9.1-9.3+9.8-9.9 verdes; 9.4-9.7 WARN (imagen anterior a 6B) | ✅ |
| API test corrida 1: 486/486 BUILD SUCCESS | ✅ |
| API test corrida 2: 486/486 BUILD SUCCESS | ✅ |
| Android emulator: validación BORRADOR/PUBLICADA/otro-ejecutivo — pendiente manual (sin emulador activo) | ⚠️ |
| Tag `v0.21.0-asignaciones-diarias` recreado en HEAD + push | ✅ |

## Próximo paso recomendado

1. Iniciar Fase 7 — Despliegue en VPS (por definir).
2. Configurar certificado corporativo en Docker Desktop WSL2 para habilitar `docker compose build`.

## Historial de fases

| Fase    | Descripción                                              | Estado     | Fecha      |
|---------|----------------------------------------------------------|------------|------------|
| Fase 0  | Inicialización del repositorio                           | Completado | 2026-07-26 |
| Fase 1A | Decisiones funcionales y documentación de dominio        | Completado | 2026-07-26 |
| Fase 1B | Base técnica modular de la API                           | Completado | 2026-07-27 |
| Fase 1C | Modelo físico de usuarios, roles, permisos, dispositivos | Completado | 2026-07-28 |
| Fase 2  | Autenticación y sesiones (JWT RS256, refresh tokens, sesiones) | Completado | 2026-07-28 |
| Fase 3A fix | Corrección persona–cartera N:M (V008, carteras_personas) | Completado | 2026-08-01 |
| Fase 3B | Asignaciones mensuales y diarias (V009, dominio, servicio, API) | Completado | 2026-08-01 |
| Fase 3C | Gestiones de cobranza (V010, dominio, servicio, API) | Completado | 2026-08-01 |
| Fase 3D | API REST de asignaciones y gestiones (endpoints Android)   | Completado | 2026-08-01 |
| Fase 4A | Base Android: red, seguridad, auth (sin Room ni WorkManager) | Completado ✅ | 2026-08-02 |
| Fase 4B | Cartera offline: Room, WorkManager, descarga asignación diaria | Cerrado ✅ — tag v0.10.0-descarga-offline | 2026-08-02 |
| Fase 4C-A | Gestiones ASIGNACION_DIARIA offline (outbox, GPS, sync) | Cerrada ✅ — tag v0.11.0-gestiones-offline | 2026-08-02 |
| Fase 4C-B | Búsqueda directa por RUT (API + Android) | Implementada ✅ — pendiente commit | 2026-08-03 |
