# Tarea actual

## Identificación

- **Fase:** 5A — Base del administrador web y autenticación web
- **Estado:** AUDITADA — PENDIENTE REVISIÓN Y COMMIT
- **Rama activa:** `feature/fase-5-admin-web`
- **Base funcional:** `4cddf50 feat: implementar busqueda directa por rut fase 4c-b`
- **Tag de fase anterior:** `v0.12.0-busqueda-directa`

## Objetivo

Implementar la base técnica del panel administrativo web:
proyecto Angular operativo, autenticación web separada de Android,
access token en memoria, refresh token en cookie HttpOnly,
layout administrativo, pantalla de login y pantalla Home con perfil real.

## Alcance

### API (cambios)

- Migración V011: `tipo_cliente` en `sesiones_autenticacion`, `dispositivo_id` nullable para WEB
- Endpoints web: `POST /api/v1/auth/web/login`, `POST /api/v1/auth/web/refresh`, `POST /api/v1/auth/web/logout`
- `GET /api/v1/auth/me` compartido; contrato corregido y alineado con OpenAPI
- Sin `identificadorInstalacion` en login web
- Refresh token por cookie HttpOnly, no en JSON
- Sin registro de dispositivo para navegadores

### Angular

- Angular 22.1.0, TypeScript 6.0.x, Angular Material 22.1.0
- Vitest 4.1.10 (no Karma)
- Playwright 1.62.1 (E2E)
- Access token únicamente en memoria
- Bootstrap de sesión al inicio de la app
- Guards funcionales
- Layout (sidenav + toolbar)
- Login y Home con perfil real

## No incluye

- CRUD de usuarios
- Carteras ni asignaciones
- Tag ni commit (pendiente de aprobación)

## Estado de verificación

| Suite | Resultado |
|---|---|
| API — `./mvnw clean verify` | ✅ **288 pruebas — 0 failures** |
| Angular — `npm run build` | ✅ **BUILD SUCCESSFUL** |
| Angular — `npm run test:ci` | ✅ **30 tests — 0 failures** |
| Angular — `npm run test:coverage` | ✅ core/auth 96.66%, guards 100%, core/http 88.88% |
| Angular — `npm run e2e` | ✅ **6 tests Playwright — 6 passed** |
| `npm audit --audit-level=high` | ✅ Exit 0 |

## Fase 5B — Pendiente (próximo)

Primera funcionalidad administrativa real: **listado de usuarios de solo lectura**.
Requerirá un endpoint nuevo: `GET /api/v1/admin/usuarios`.
No se implementa en 5A.

## Fases anteriores — CERRADAS ✅

| Fase | Tag | Commit |
|---|---|---|
| 4C-B Búsqueda directa | `v0.12.0-busqueda-directa` | `4cddf50` |
| 4C-A Gestiones offline | `v0.11.0-gestiones-offline` | `dec7b18` |
| 4B Cartera offline | `v0.10.0-descarga-offline` | (ver ROADMAP) |
