# Tarea activa

**Fase:** 6B — Asignaciones Diarias y Publicación
**Estado:** CERRADA Y PUBLICADA ✅ — tag v0.21.0-asignaciones-diarias en HEAD
**Rama:** main

## Completado y validado

### Backend (Java / Spring Boot)
- V016: columna `publicado_por_id`; permisos ASIGNACIONES_VER + ASIGNACIONES_ADMINISTRAR a TECNOLOGIA
- `AsignacionDiaria.publicar(UUID)` con auditoría de publicador
- `AsignacionAdminQueryService` — consultas nativas (fix toInstant + rut_dv.toString)
- `AsignacionAdminService` — crearBorrador, actualizarPersonas, publicar, cancelar
- `AsignacionAdminController` — 9 endpoints en /api/v1/admin/asignaciones
- `AsignacionAdminRestTest` — 25 escenarios de integración
- API corrida 1: 486/486 BUILD SUCCESS
- API corrida 2: 486/486 BUILD SUCCESS (dos consecutivas verdes ✅)

### Angular (Admin Web)
- Módulo asignaciones completo: models, service, list, create, detail, routes, layout
- 196/196 tests Vitest — 0 failures ✅
- ng build limpio; npm audit 0 high (nanoid actualizado) ✅
- Android JVM: 31/31 tests feature:asignacion — BUILD SUCCESS

### OpenAPI
- 9 schemas + 9 paths admin/asignaciones documentados en contracts/openapi/cobranza-api.yaml

### Playwright
- 12 nuevos escenarios asignaciones [INTERCEPTADO]: menú, list, permisos, crear, detalle BORRADOR/PUBLICADA, publicar
- 52/52 total (12 nuevos + 40 previos) — 0 failures ✅

### Docker + Smoke test
- `up --force-recreate` — 3 contenedores healthy ✅
- `build` falló por TLS corporativo WSL2/nginx:alpine (no es defecto de código)
- Smoke test 79/79 OK — sección 9 con scenarios Fase 6B ✅

### Git
- Push main: origin/main actualizado
- Tag v0.21.0-asignaciones-diarias recreado en HEAD y pusheado

## Próximo paso

Iniciar Fase 7 — Despliegue en VPS (pendiente definición de alcance).
