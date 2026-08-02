# Roadmap

## Fase actual: Fase 3D — API REST de asignaciones y gestiones ✅ Validada — Lista para cierre

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

## Fase 4 — App Android (PENDIENTE)

**Objetivo:** Crear la app Android con soporte offline-first, registro de gestiones y sincronización.

**Incluye (borrador):**
- Proyecto Android con Jetpack Compose en `apps/mobile-android/`.
- Arquitectura Clean con Room, WorkManager, Hilt.
- Módulos: autenticación, cartera, gestiones, sincronización.
- Soporte completo offline: outbox, estados de sincronización.
- Pruebas instrumentadas y en dispositivo real.

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
