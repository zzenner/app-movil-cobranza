# Tarea activa

**Fase:** 6B — Asignaciones Diarias y Publicación
**Estado:** COMPLETADA ✅ — pendiente commit, tag y push
**Rama:** main

## Completado en esta sesión

### Backend (Java / Spring Boot)
- V016: columna `publicado_por_id` en `asignaciones_diarias`; permisos de asignaciones a TECNOLOGIA
- `AsignacionDiaria.publicar(UUID)` — firma actualizada con auditoría de publicador
- `AsignacionAdminQueryService` — consultas nativas vía EntityManager
- `AsignacionAdminService` — crearBorrador, actualizarPersonas, publicar, cancelar
- `AsignacionAdminController` — 9 endpoints en /api/v1/admin/asignaciones
- `AsignacionAdminRestTest` — 12 escenarios de integración
- Tests previos corregidos (4 archivos + AsignacionDiariaDominioTest)
- 29/29 unit tests — 0 failures ✅

### Angular (Admin Web)
- `features/asignaciones/` — módulo completo: models, service, 3 componentes, rutas
- Layout actualizado con ítem Asignaciones (ASIGNACIONES_VER)
- 196/196 tests Vitest — 0 failures ✅
- ng build — limpio sin warnings ✅

## Pendiente para cierre formal

1. `git add` + `git commit -m "feat(asignaciones): implementar asignaciones diarias fase 6b"`
2. `git tag v0.21.0-asignaciones-diarias`
3. `git push origin main` (requiere autorización explícita)
4. `git push origin v0.21.0-asignaciones-diarias` (requiere autorización)

## Nota sobre Docker / integration tests

Docker Desktop WSL2 integration no está habilitado en esta sesión WSL2.
Los 16 integration tests (Testcontainers) fallan por infraestructura, no por código.
Todos los unit tests de dominio (29/29) pasan.
Para ejecutar la suite completa: habilitar WSL2 integration en Docker Desktop settings.
