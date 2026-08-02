# Changelog

Todos los cambios significativos del proyecto se documentan aquí.
Formato basado en [Keep a Changelog](https://keepachangelog.com/es/1.1.0/).

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
