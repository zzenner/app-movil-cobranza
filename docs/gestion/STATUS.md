# Estado del proyecto

**Última actualización:** 2026-08-02
**Fase actual:** Fase 4A — Base Android (autenticación + almacenamiento seguro) — VALIDADA ✅
**Fase anterior:** Fase 3D — API REST de asignaciones y gestiones ✅ CERRADA

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
| Admin Web (Angular)                                       | No iniciado           |
| **App Android — Fase 4A (base auth + red + seguridad)**   | **Validada ✅ — Lista para cierre** |
| Despliegue en VPS                                         | No iniciado           |

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
| P-05 | ¿Puede el supervisor modificar una asignación `PUBLICADA` o debe crear una nueva?                           |
| P-06 | ¿Se implementa `CANCELADA` como estado de asignación diaria en el MVP?                                      |
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

## Próximo paso recomendado

Cierre formal de Fase 4A y apertura de Fase 4B (cartera offline con Room y WorkManager).

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
