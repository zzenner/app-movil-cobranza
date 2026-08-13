# Estado de sesión — Fase 6B: Asignaciones Diarias y Publicación

**Fecha:** 2026-08-13
**Rama:** main
**Estado:** IMPLEMENTADA ✅ — pendiente commit formal y push

## Resumen de lo implementado

### API (Java / Spring Boot)

| Archivo | Cambio |
|---------|--------|
| `V016__asignaciones_admin.sql` | `publicado_por_id UUID NULL` en `asignaciones_diarias`; permisos asignaciones a TECNOLOGIA |
| `AsignacionDiaria.java` | `publicar(UUID publicadoPorId)` — campo `publicadoPorId` + getter |
| `AsignacionService.java` | `publicarAsignacionDiaria(UUID, UUID)` — 2do arg propagado |
| `AsignacionDiariaPersonaRepository.java` | `deleteByAsignacionDiariaIdAndPersonaId` añadido |
| `AsignacionAdminQueryService.java` | Consultas nativas: periodos, mensuales, personas-disponibles, diarias, detalle |
| `AsignacionAdminService.java` | crearBorrador, actualizarPersonas, publicar, cancelar |
| `AsignacionAdminController.java` | 9 endpoints en /api/v1/admin/asignaciones |
| `AsignacionAdminRestTest.java` | 12 escenarios @Order(10–120) |
| 4 test files existentes | `publicarAsignacionDiaria(id, null)` en AsignacionDescargaRestTest, GestionRestTest, GestionesIntegracionTest, DominioAsignacionesIntegracionTest |
| `AsignacionDiariaDominioTest.java` | `ad.publicar(null)` y `() -> ad.publicar(null)` en assertThatThrownBy |

### Angular (Admin Web)

| Archivo | Cambio |
|---------|--------|
| `features/asignaciones/models/asignacion.models.ts` | Interfaces: ItemPeriodo, ItemAsignacionMensualAdmin, ItemPersonaDisponible, ItemAsignacionDiariaAdmin, DetalleAsignacionDiariaAdmin, ItemPersonaEnDiaria + DTOs |
| `features/asignaciones/services/asignaciones.service.ts` | 9 métodos HTTP |
| `features/asignaciones/components/asignaciones-list/` | Tabla con filtros fecha/estado |
| `features/asignaciones/components/asignacion-create/` | Stepper: mensual → personas → guardar |
| `features/asignaciones/components/asignacion-detail/` | Detalle, publicar, cancelar |
| `features/asignaciones/asignaciones.routes.ts` | Rutas con permissionGuard |
| `app/app.routes.ts` | Ruta /asignaciones añadida |
| `core/layout/layout.component.ts` | Ítem menú Asignaciones + tienePermisoAsignaciones signal |
| Specs (4 archivos) | 196/196 tests totales |

## Estado de pruebas

| Suite | Resultado |
|-------|-----------|
| API unit tests (domain) | 29/29 ✅ |
| API integration tests | No ejecutados — Docker no disponible en WSL2 |
| Angular unit tests (Vitest) | 196/196 ✅ |
| Angular build (ng build) | Limpio — 0 warnings ✅ |

## Estado del repositorio

- Rama: main
- HEAD: 0f1bd24 (docs: documentar conectividad Android con entorno Docker local)
- Todos los cambios de Fase 6B están en working tree (sin commit aún)
- origin/main: pendiente push (requiere autorización)

## Siguiente acción exacta

```bash
cd /home/msalazar/app-movil-cobranza

git add \
  apps/api/src/main/resources/db/migration/V016__asignaciones_admin.sql \
  apps/api/src/main/java/cl/zzenner/cobranza/asignaciones/dominio/AsignacionDiaria.java \
  apps/api/src/main/java/cl/zzenner/cobranza/asignaciones/aplicacion/AsignacionService.java \
  apps/api/src/main/java/cl/zzenner/cobranza/asignaciones/infraestructura/AsignacionDiariaPersonaRepository.java \
  apps/api/src/main/java/cl/zzenner/cobranza/asignaciones/aplicacion/AsignacionAdminQueryService.java \
  apps/api/src/main/java/cl/zzenner/cobranza/asignaciones/aplicacion/AsignacionAdminService.java \
  apps/api/src/main/java/cl/zzenner/cobranza/asignaciones/web/AsignacionAdminController.java \
  apps/api/src/test/java/cl/zzenner/cobranza/AsignacionAdminRestTest.java \
  apps/api/src/test/java/cl/zzenner/cobranza/AsignacionDescargaRestTest.java \
  apps/api/src/test/java/cl/zzenner/cobranza/DominioAsignacionesIntegracionTest.java \
  apps/api/src/test/java/cl/zzenner/cobranza/GestionRestTest.java \
  apps/api/src/test/java/cl/zzenner/cobranza/GestionesIntegracionTest.java \
  apps/api/src/test/java/cl/zzenner/cobranza/asignaciones/AsignacionDiariaDominioTest.java \
  apps/admin-web/src/app/app.routes.ts \
  apps/admin-web/src/app/core/layout/layout.component.ts \
  apps/admin-web/src/app/core/layout/layout.component.spec.ts \
  apps/admin-web/src/app/features/asignaciones/ \
  docs/gestion/STATUS.md \
  docs/gestion/CHANGELOG.md \
  docs/gestion/ROADMAP.md \
  .claude/TASK_CURRENT.md \
  .claude/SESSION_HANDOFF.md

git commit -m "feat(asignaciones): implementar asignaciones diarias fase 6b"

git tag v0.21.0-asignaciones-diarias
```

Luego (requiere autorización explícita):
```bash
git push origin main
git push origin v0.21.0-asignaciones-diarias
```

## Deuda técnica

- Integration tests requieren Docker Desktop WSL2 integration habilitado.
- OpenAPI YAML (`contracts/openapi/cobranza-api.yaml`) no actualizado aún con los nuevos endpoints de admin/asignaciones.
- No hay Playwright E2E para el flujo de asignaciones (pendiente si se requiere cobertura E2E).
