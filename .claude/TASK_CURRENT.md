# Tarea activa

**Fase:** 6A — Carteras y Supervisión
**Estado:** BACKEND + ANGULAR COMPLETADOS — Pendiente commit y cierre formal
**Rama:** feature/fase-6a-carteras-supervision

## Completado en esta sesión

### Backend (Java / Spring Boot)
- V015 migration: 3 permisos nuevos (CARTERAS_VER, SUPERVISION_VER, SUPERVISION_ADMINISTRAR), asignados a roles
- Cartera.java: campo codigoOrigen mapeado
- CarteraService: listarTodas() con Sort
- CarteraAdminController: GET /api/v1/admin/carteras con PERM_CARTERAS_VER
- CodigoEjecutivoDuplicadoException: excepción de dominio
- Usuario.java: codigoEjecutivoOrigen field + setter normalizado
- UsuarioRepository: 2 nuevos query methods
- UsuarioRolRepository: findAllByRolIdAndActivoTrue
- SupervisionService: reescritura completa (reasignar atómico, validarUsuarioOperativo, actualizarCodigo, saveAndFlush)
- SupervisionAdminQueryService: consultas con filtros
- SupervisionAdminController: 5 endpoints REST (GET ×2, POST, DELETE, PATCH)
- CarterasSupervisionRestTest: 22 escenarios integración — TODOS PASAN ✅
- SeguridadIntegracionTest: actualizado conteo permisos 8→11 — PASAN ✅
- Suite completa: 461 tests, 0 fallos ✅

### Angular (Admin Web)
- CartesrasModule: CarterasService + CarterasListComponent + carteras.routes.ts
- SupervisionModule: SupervisionService + SupervisionListComponent + diálogos + supervision.routes.ts
- app.routes.ts: rutas /carteras y /supervision registradas
- Layout: items de menú Carteras (CARTERAS_VER) y Supervisión (SUPERVISION_VER)
- 4 specs nuevos (2 service + 2 component) — 176 tests, 0 fallos ✅
- Build producción OK ✅

### Docker
- Rebuild exitoso: API + Admin Web
- Smoke test: GET /api/v1/admin/carteras → 200 + 4 carteras, GET /api/v1/admin/supervision/ejecutivos → 200

## Pendiente para cierre formal

1. Commit: feat(admin): implementar carteras y supervision fase 6a
2. Push branch feature/fase-6a-carteras-supervision
3. Merge a main con --ff-only
4. Tag v0.20.0-carteras-supervision
5. Actualizar SESSION_HANDOFF.md final
6. Entregar reporte final
