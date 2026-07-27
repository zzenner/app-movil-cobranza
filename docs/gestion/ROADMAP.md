# Roadmap

## Fase actual: Fase 0 — Inicialización del repositorio

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

## Fase 1 — API base y modelo de datos (PENDIENTE)

**Objetivo:** Crear el proyecto Spring Boot con Spring Modulith, el esquema de base de datos inicial con Flyway, y los módulos de autenticación y usuarios.

**Incluye (borrador):**
- Proyecto Spring Boot 3.x con Spring Modulith en `apps/api/`.
- Esquema PostgreSQL con Flyway: tablas `usuarios`, `carteras`, `asignaciones`, `personas`.
- Módulo de autenticación (login + JWT o sesión).
- Módulo de usuarios (CRUD de administradores y cobradores).
- Contrato OpenAPI básico en `contracts/openapi/`.
- Pruebas de integración con Testcontainers.
- Docker Compose actualizado con la API.

**PENDIENTE:** Definir exactamente el mecanismo de autenticación y el alcance del módulo de personas.

---

## Fase 2 — Módulos de cartera y gestiones (PENDIENTE)

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
