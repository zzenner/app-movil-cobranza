# ADR-0016 — Flyway como propietario exclusivo del esquema de base de datos

**Estado:** Aceptado
**Fecha:** 2026-07-27
**Fase:** 1B

---

## Contexto

Al iniciar la Fase 1B se encontró que los esquemas `cobranza` y `auditoria` eran creados por el script de inicialización Docker (`infrastructure/postgres/init/02_schemas.sql`). Existen dos mecanismos posibles para gestionar el esquema de la base de datos: los scripts de inicialización del contenedor Docker y Flyway.

Tener dos mecanismos que crean objetos de base de datos introduce ambigüedad sobre quién es el propietario del esquema y complica la evolución del mismo.

---

## Decisión

**Flyway es el propietario exclusivo de todos los objetos de base de datos de la aplicación** (esquemas, tablas, índices, funciones, tipos, etc.).

Los scripts de inicialización Docker (`infrastructure/postgres/init/`) se restringen a:
- Creación de extensiones PostgreSQL necesarias (`postgis`, `postgis_topology`, `uuid-ossp`, `pg_trgm`).
- No crean esquemas ni tablas.

La migración `V001__crear_esquemas_base.sql` es la primera migración Flyway y crea los esquemas `cobranza` y `auditoria` de forma idempotente (`CREATE SCHEMA IF NOT EXISTS`).

Se modificó `02_schemas.sql` para eliminar la creación de esquemas.

---

## Alternativas descartadas

| Alternativa | Motivo de descarte |
|---|---|
| Scripts Docker crean esquemas, Flyway crea tablas | Dos propietarios del esquema. Ambiguo y difícil de evolucionar. |
| Hibernate `ddl-auto: create-drop` o `update` | Hibernate no tiene control de versiones del esquema. Peligroso en producción. |
| Scripts SQL manuales fuera de Flyway | Sin trazabilidad ni reproducibilidad del estado del esquema. |

---

## Consecuencias

- Toda evolución del esquema se registra en `apps/api/src/main/resources/db/migration/`.
- Hibernate tiene `ddl-auto: none` — no crea ni valida tablas.
- La tabla `flyway_schema_history` reside en el esquema `public` (comportamiento por defecto de Flyway).
- Las nuevas migraciones siguen la convención `V{NNN}__{descripcion_breve}.sql`.
- Si se levanta un entorno limpio (sin volumen), Docker crea solo las extensiones y Flyway crea los esquemas en el primer arranque de la API.

---

## Referencias

- ADR-0002 — PostgreSQL como base de datos relacional
- ADR-0003 — Flyway para migraciones de base de datos
- `infrastructure/postgres/init/01_extensions.sql`
- `apps/api/src/main/resources/db/migration/V001__crear_esquemas_base.sql`
