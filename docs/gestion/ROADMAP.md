# Roadmap

## Fase actual: Fase 1B completada — preparando Fase 2

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

## Fase 2 — Módulos de cartera y gestiones (PENDIENTE)

> Anteriormente llamado "segunda mitad de Fase 1". Se inicia después de que Fase 1B esté estable.

**Objetivo:** Implementar los módulos de carteras, asignaciones, personas, créditos y gestiones en la API.

**Incluye (borrador):**
- Módulos de carteras y asignaciones en la API.
- Módulo de personas y créditos (o integración con sistema externo: PENDIENTE).
- Módulo de gestiones: recepción idempotente, persistencia, consulta.
- Endpoints de sincronización para Android.
- Ampliación del contrato OpenAPI.

---

## Fase 3 — App Android (PENDIENTE)

**Objetivo:** Crear la app Android con soporte offline-first, registro de gestiones y sincronización.

**Incluye (borrador):**
- Proyecto Android con Jetpack Compose en `apps/mobile-android/`.
- Arquitectura Clean con Room, WorkManager, Hilt.
- Módulos: autenticación, cartera, gestiones, sincronización.
- Soporte completo offline: outbox, estados de sincronización.
- Pruebas instrumentadas y en dispositivo real.

---

## Fase 4 — Administración web (PENDIENTE)

**Objetivo:** Crear la aplicación web Angular para la administración del sistema.

**Incluye (borrador):**
- Proyecto Angular con componentes standalone en `apps/admin-web/`.
- Módulos: usuarios, carteras, asignaciones, visualización de gestiones.
- Integración completa con la API.

---

## Fase 5 — Despliegue en VPS (PENDIENTE)

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
