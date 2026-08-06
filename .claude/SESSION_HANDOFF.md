# Handoff de sesión — Fase 5B-1 VALIDADA ✅ / Pendiente commit y Fase 5B-2

**Fecha:** 2026-08-05
**Rama activa:** `feature/fase-5b-usuarios-admin`

---

## Estado de Git

| Referencia | Hash | Descripción |
|---|---|---|
| `main` | `71d47b2` | feat: implementar base admin web y autenticacion fase 5a |
| `feature/fase-5b-usuarios-admin` | `6009e11` | docs: preparar contexto para fase 5b (último commit en rama) |
| Cambios actuales | sin commit | Toda la implementación Fase 5B-1 — sin commit aún |

---

## Auditoría final Fase 5B-1 — 2026-08-05

| Suite | Ejecuciones | Resultado |
|---|---|---|
| API — `mvn clean verify` | 3 consecutivas | ✅ **323 pruebas — 0 failures — BUILD SUCCESS** (3/3) |
| Angular — `npm run build` | 1 | ✅ **BUILD SUCCESSFUL** — chunks usuarios-list y usuario-detail |
| Angular — `npm run test:ci` | 1 | ✅ **50 tests — 0 failures** |
| Angular — `npm run test:coverage` | 1 | ✅ core/auth: 96.6%/100%, guards: 100%/86.4%, core/http: 88.9%/100%, usuarios-list: 94.2%/80%, usuario-detail: 85.7%/82.4% |
| Angular — `npm run e2e` | 1 | ✅ **14 tests Playwright — 14 passed** |
| `npm audit --audit-level=high` | 1 | ✅ 0 high/critical (3 moderate, no acción requerida) |

---

## Archivos creados o modificados en Fase 5B-1

### API (`apps/api/`)

**Dominio / web (nuevos)**
- `usuarios/dominio/EstadoUsuario.java` — enum: ACTIVO, BLOQUEADO_TEMPORAL, BLOQUEADO, INACTIVO
- `usuarios/web/RolVigenteAdmin.java` — record(codigo, fechaAsignacion)
- `usuarios/web/ItemListadoUsuarioAdmin.java` — record listado (sin contrasenaHash, sin version)
- `usuarios/web/RespuestaListadoUsuarios.java` — record paginado
- `usuarios/web/DetalleUsuarioAdmin.java` — record detalle completo
- `usuarios/aplicacion/UsuarioAdminService.java` — servicio con batch queries, Clock
- `usuarios/web/UsuarioAdminController.java` — GET /api/v1/admin/usuarios + /{id}

**Repositorios (extendidos)**
- `usuarios/infraestructura/UsuarioRepository.java` — añadido `findAllByIdIn`
- `usuarios/infraestructura/UsuarioRolRepository.java` — añadido `findAllByUsuarioIdInAndActivoTrue`
- `usuarios/infraestructura/SupervisionRepository.java` — añadido `findAllByEjecutivoIdInAndActivoTrue`
- `usuarios/infraestructura/RolPermisoRepository.java` — añadido `findAllByIdRolIdIn`

**Tests nuevos**
- `usuarios/EstadoUsuarioTest.java` — 7 tests unitarios para calcularEstado
- `UsuarioAdminRestTest.java` — 28 tests de integración (auth, filtros, paginación, detalle, seguridad)

### Angular (`apps/admin-web/`)

**Nuevos**
- `core/guards/permission.guard.ts` + `.spec.ts` — guard basado en permisos (no roles)
- `features/usuarios/models/usuario.models.ts` — tipos TypeScript
- `features/usuarios/services/usuarios.service.ts` + `.spec.ts` — HttpClient, HttpParams condicionales
- `features/usuarios/components/usuarios-list/usuarios-list.component.ts` + `.spec.ts`
- `features/usuarios/components/usuario-detail/usuario-detail.component.ts` + `.spec.ts`
- `features/usuarios/usuarios.routes.ts` — rutas lazy, permissionGuard, data: { permission: 'USUARIOS_VER' }
- `e2e/usuarios.spec.ts` — 8 tests Playwright interceptados (usuarios)

**Modificados**
- `app/app.routes.ts` — añadido `/usuarios` loadChildren
- `app/core/layout/layout.component.ts` — enlace Usuarios condicional por permiso

### Contratos y documentación
- `contracts/openapi/cobranza-api.yaml` — GET /api/v1/admin/usuarios + /{id} con schemas completos
- `docs/adr/0046-politica-acceso-consultas-admin-usuarios.md` — nuevo ADR
- `.claude/TASK_CURRENT.md` — actualizado a EN IMPLEMENTACIÓN

---

## Bugs corregidos durante implementación

1. **NPE en `UsuarioAdminService.listar()` L80**: `Map.of().get(null)` al iterar usuarios sin supervisor.
   Fix: extraer `supId` local antes de acceder a `nombresPorSupervisorId`.

2. **ConstraintViolationException → 422 en lugar de 400**: eliminada `@Validated` y anotaciones `@Min/@Max/@Size` del controlador, reemplazadas por validación manual con `IllegalArgumentException` → 400.

3. **Playwright route pattern sin query params**: `**/api/v1/admin/usuarios` no interceptaba URLs con `?pagina=...`. Fix: cambiado a `**/api/v1/admin/usuarios**`.

4. **`jest.fn()` / `jest.Mocked` en tests Vitest**: reemplazados con `vi.fn()` e importación explícita de `Mocked from 'vitest'`.

5. **`fakeAsync` sin zone.js en Vitest**: reemplazado por `vi.useFakeTimers()` con `vi.advanceTimersByTime()`.

---

## Siguiente acción exacta

**Fase 5B-1 VALIDADA — Lista para cierre.**

**Antes de empezar 5B-2:**
1. Commit de Fase 5B-1 (autorización explícita requerida del usuario)
2. Verificar que `main` está al día

**Fase 5B-2 — Gestión de usuarios (escritura):**
- Crear usuario, editar usuario, activar/desactivar, cambiar contraseña
- Permiso: `USUARIOS_ADMINISTRAR`
- Requiere nuevos endpoints POST/PUT en la API y nuevos formularios en Angular
- Ver `.claude/TASK_CURRENT.md` para detalles del alcance de 5B-2

---

## No repetir

- Fase 5A completa (commit 71d47b2, tag v0.13.0-admin-base, main actualizado)
- Fase 5B-1 implementada — sin commit aún (no commitear sin autorización)
- V011 ya en producción — no tocar migración
- WebOriginValidationFilter ya implementado
- Bootstrap de sesión y single-flight refresh ya implementados
- Fases Android 4A–4C-B completas — no tocar
- `GlobalExceptionHandler` mapea `ConstraintViolationException` → 422; usar validación manual para devolver 400
