# API — Base técnica modular (Fase 1B)

Documentación de la base técnica de `cobranza-api` establecida en la Fase 1B.

**Estado:** Completado (2026-07-27)

---

## Stack técnico

| Capa | Tecnología | Versión |
|---|---|---|
| Plataforma | Java | 21 (LTS) |
| Framework | Spring Boot | 3.5.16 |
| Modularidad | Spring Modulith | 1.4.12 |
| Build | Maven Wrapper | 3.8.7 |
| Base de datos | PostgreSQL + PostGIS | 16 + 3.4 |
| Migraciones | Flyway | 11.x (gestionado por Boot) |
| ORM | Hibernate / Spring Data JPA | 6.6.x (gestionado por Boot) |
| Documentación API | Springdoc OpenAPI | 2.8.17 |
| Pruebas integración | Testcontainers | (gestionado por Boot) |
| CI | GitHub Actions | — |

---

## Estructura de módulos Spring Modulith

El paquete raíz es `cl.zzenner.cobranza`. Spring Modulith detecta automáticamente los sub-paquetes directos como módulos.

| Módulo | Paquete | Responsabilidad |
|---|---|---|
| `autenticacion` | `cl.zzenner.cobranza.autenticacion` | Login, JWT, logout |
| `usuarios` | `cl.zzenner.cobranza.usuarios` | Usuarios y roles |
| `dispositivos` | `cl.zzenner.cobranza.dispositivos` | Registro y revocación de dispositivos Android |
| `carteras` | `cl.zzenner.cobranza.carteras` | Carteras de cobranza |
| `asignaciones` | `cl.zzenner.cobranza.asignaciones` | Asignaciones mensuales y diarias |
| `personas` | `cl.zzenner.cobranza.personas` | Copia operacional de personas y datos asociados |
| `operaciones` | `cl.zzenner.cobranza.operaciones` | Créditos y cuotas por persona |
| `gestiones` | `cl.zzenner.cobranza.gestiones` | Registro idempotente de gestiones de terreno |
| `sincronizacion` | `cl.zzenner.cobranza.sincronizacion` | Coordinación de sincronización con Android |
| `auditoria` | `cl.zzenner.cobranza.auditoria` | Trazabilidad de operaciones críticas |
| `compartido` | `cl.zzenner.cobranza.compartido` | Utilidades transversales sin lógica de dominio |

Los módulos son **stubs** en la Fase 1B. Solo contienen `package-info.java` con `@ApplicationModule` y documentación de responsabilidades.

---

## Gestión de esquema con Flyway

Flyway es el propietario exclusivo de todos los objetos de base de datos (ver ADR-0016).

**Convención de nomenclatura de migraciones:**

```
V{NNN}__{descripcion_breve}.sql
```

| Migración | Descripción |
|---|---|
| `V001__crear_esquemas_base.sql` | Crea los esquemas `cobranza` y `auditoria` |

La tabla de historia de Flyway (`flyway_schema_history`) reside en el esquema `public`.

Los scripts de inicialización Docker (`infrastructure/postgres/init/`) solo gestionan extensiones PostgreSQL (PostGIS, uuid-ossp, pg_trgm). La creación de esquemas fue migrada de `02_schemas.sql` a Flyway en esta fase (ver ADR-0016).

---

## Configuración de Hibernate

`spring.jpa.hibernate.ddl-auto: none` — Hibernate no crea ni modifica tablas. Flyway es el único responsable del esquema.

`spring.jpa.open-in-view: false` — Session in View deshabilitado para evitar lazy loading no controlado fuera de las transacciones.

---

## Endpoints de infraestructura

| Endpoint | Descripción |
|---|---|
| `GET /actuator/health` | Estado de la aplicación |
| `GET /actuator/health/liveness` | Liveness probe |
| `GET /actuator/health/readiness` | Readiness probe |
| `GET /actuator/info` | Info de la aplicación |
| `GET /v3/api-docs` | OpenAPI 3.1 en JSON |
| `GET /swagger-ui/index.html` | Swagger UI |

Los endpoints `heapdump`, `env` y `beans` del Actuator **no están expuestos**.

---

## Manejo de errores

`GlobalExceptionHandler` en `cl.zzenner.cobranza` (paquete raíz, fuera de módulos) maneja:
- `MethodArgumentNotValidException` — errores de validación en cuerpos de solicitud
- `ConstraintViolationException` — errores de validación en parámetros de método

El formato de respuesta es `ProblemDetail` (RFC 7807), habilitado globalmente con `spring.mvc.problemdetails.enabled: true`.

---

## Pruebas de la Fase 1B

| Test | Tipo | Qué verifica |
|---|---|---|
| `ModularidadTest` | Unitario (sin Spring) | Estructura modular con Spring Modulith `verify()` |
| `InfraestructuraTest` | Integración (Testcontainers) | Esquemas Flyway, PostGIS, Hibernate sin tablas |
| `ActuatorTest` | Integración web (Testcontainers) | Health, liveness, readiness, OpenAPI |

Todos los tests usan `postgis/postgis:16-3.4` (misma imagen que `compose.yaml`).

---

## Integración continua

El workflow `.github/workflows/api-ci.yml` se activa en cambios en `apps/api/**` o `contracts/openapi/**`. Ejecuta `./mvnw -B verify` con Java 21 (Temurin) y cache de dependencias Maven.

---

## Archivos clave

| Archivo | Descripción |
|---|---|
| `apps/api/pom.xml` | Proyecto Maven con dependencias y versiones |
| `apps/api/src/main/resources/application.yml` | Configuración base |
| `apps/api/src/main/resources/application-local.yml` | Datasource local (lee de env vars) |
| `apps/api/src/main/resources/db/migration/V001__crear_esquemas_base.sql` | Primera migración Flyway |
| `contracts/openapi/cobranza-api.yaml` | Contrato OpenAPI 3.1 (vacío en Fase 1B) |
| `scripts/api-run.sh` | Inicia la API con perfil local |
| `scripts/api-test.sh` | Ejecuta todas las pruebas |
| `scripts/api-check.sh` | Verificación rápida sin Testcontainers |
| `.github/workflows/api-ci.yml` | Pipeline de CI |
