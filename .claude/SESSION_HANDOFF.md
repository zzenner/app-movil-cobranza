# Handoff de sesión — Fase 5B-2 VALIDADA ✅ LISTA PARA CIERRE

**Fecha:** 2026-08-06
**Rama activa:** `feature/fase-5b-2-usuarios-escritura`

---

## Estado de Git

| Referencia | Hash | Descripción |
|---|---|---|
| `main` (local + origin) | `1a22c8a` | feat(infra): implementar entorno docker local integrado |
| `v0.15.0-entorno-docker-local` | `1a22c8a` | Tag Docker — publicado ✅ |
| `feature/fase-5b-2-usuarios-escritura` | HEAD | Fase 5B-2 implementada y validada — sin commit aún |
| `v0.14.0-usuarios-admin-readonly` | `d82d95d` | Tag Fase 5B-1 — publicado ✅ |

---

## Resultados de auditoría — PASADA ✅

### API — Spring Boot

| Suite | Resultado |
|---|---|
| `./mvnw --batch-mode clean verify` (run 1) | ✅ **371 tests — 0 failures — BUILD SUCCESS** |
| `./mvnw --batch-mode clean verify` (run 2) | ✅ **371 tests — 0 failures — BUILD SUCCESS** |

**Distribución de tests (371 total):**
- `UsuarioAdminEscrituraRestTest`: 36 (era 24 → +12 en auditoría)
- `AutenticacionIntegracionTest`: 32 (era 29 → +3 en auditoría)
- `AutenticacionWebIntegracionTest`: 22 (era 19 → +3 en auditoría)
- Resto de suites: 281 (sin cambios)

**Tests añadidos en auditoría (18 nuevos):**
- Actor desactivado con JWT vigente → 403
- Actor bloqueado con JWT vigente → 403
- Actor con bloqueo temporal → 403
- Contraseña >72 bytes UTF-8 → 400
- Crear usuario sin correo → 201
- Nombre duplicado case-insensitive → 409
- activar no cambia flag bloqueado
- Editar propia cuenta → 204
- Restablecer contraseña propia → 204
- Desactivar único admin activo → 409
- Locking optimista concurrente real (CountDownLatch) → un 204 y un 409
- Desactivar revoca refresh tokens del target
- renovar con usuario desactivado → 401
- renovar con usuario bloqueado → 401
- renovar con bloqueo temporal → 401
- renovarWeb con usuario desactivado → 401
- renovarWeb con usuario bloqueado → 401
- renovarWeb con bloqueo temporal → 401

### Angular — Vitest

| Suite | Resultado |
|---|---|
| `npm run test:ci` | ✅ **94 tests — 0 failures** (16 archivos spec) |
| `npm run test:coverage` | ✅ All files 84.1% Stmts, 81.35% Branch, 86.52% Lines |
| `npm run build` | ✅ éxito |
| `npm audit --audit-level=high` | ✅ 0 high/critical (3 moderate — línea base sin cambios) |

**Cobertura por componente nuevo:**
- `confirm-action-dialog`: 95.65% Stmts, 100% Lines
- `reset-password-dialog`: 88.88% Stmts, 96.15% Lines
- `usuario-create`: 84.48% Stmts, 88.33% Lines
- `usuario-detail`: 82.86% Stmts, 84.26% Lines
- `usuario-edit`: 86.71% Stmts, 90.1% Lines

**Specs creados en auditoría:**
- `confirm-action-dialog.component.spec.ts` (4 tests)
- `reset-password-dialog.component.spec.ts` (8 tests)
- `usuario-create.component.spec.ts` (7 tests)
- `usuario-edit.component.spec.ts` (7 tests)
- `usuario-detail.component.spec.ts` +10 tests acciones admin (15 total)

### Playwright e2e

| Suite | Resultado |
|---|---|
| `npm run e2e` | ✅ **26 tests — 0 failures** (7.9s) |

**Tests añadidos en auditoría (+12 [INTERCEPTADO]):**
- botón "Nuevo usuario" visible con permiso USUARIOS_ADMINISTRAR
- acceso a /usuarios/nuevo sin permiso → /forbidden
- formulario carga roles del catálogo
- nombre duplicado muestra error 409
- formulario editar carga datos del usuario
- conflicto de versión muestra mensaje
- botones de acciones administrativas visibles
- desactivar botón oculto para propia cuenta
- bloquear botón oculto para propia cuenta
- activar usuario llama API y recarga
- desactivar usuario muestra confirmación
- restablecer contraseña muestra diálogo

### Docker

| Paso | Resultado |
|---|---|
| `docker compose ps` | ✅ 3 servicios healthy (postgres, api, admin-web) |
| Imágenes reconstruidas con código 5B-2 | ✅ `docker compose build api admin-web && docker compose up -d` |
| `bash scripts/smoke-test.sh` | ✅ **47 OK, 0 FALLIDO** |

**Secciones smoke test:**
1. Infraestructura (5 OK)
2. API pública (2 OK)
3. SPA Angular/Nginx (4 OK)
4. Proxy Nginx→API (2 OK)
5. Flujo autenticación completo (10 OK)
6. Seguridad de proceso (2 OK)
7. Endpoints escritura 5B-2 (23 OK): GET /admin/roles, POST /admin/usuarios, GET detalle con version, PUT datos-basicos, desactivar/activar con login rechazado/aceptado, bloquear/desbloquear con login rechazado/aceptado, restablecer-contrasena (contraseña antigua rechazada, nueva aceptada), usuario en listado, estado final correcto

---

## Archivos modificados en auditoría (adicionales a los de la implementación)

### Nuevos spec files (Angular)
```
apps/admin-web/src/app/features/usuarios/components/confirm-action-dialog/confirm-action-dialog.component.spec.ts
apps/admin-web/src/app/features/usuarios/components/reset-password-dialog/reset-password-dialog.component.spec.ts
apps/admin-web/src/app/features/usuarios/components/usuario-create/usuario-create.component.spec.ts
apps/admin-web/src/app/features/usuarios/components/usuario-edit/usuario-edit.component.spec.ts
```

### Modificados en auditoría
```
apps/api/src/test/java/cl/zzenner/cobranza/UsuarioAdminEscrituraRestTest.java     (+12 tests)
apps/api/src/test/java/cl/zzenner/cobranza/autenticacion/AutenticacionIntegracionTest.java     (+3 tests renovar)
apps/api/src/test/java/cl/zzenner/cobranza/autenticacion/AutenticacionWebIntegracionTest.java  (+3 tests renovarWeb)
apps/admin-web/src/app/features/usuarios/components/usuario-detail/usuario-detail.component.spec.ts  (+10 tests)
apps/admin-web/e2e/usuarios.spec.ts                                                 (+12 tests Playwright)
scripts/smoke-test.sh                                                               (+23 escenarios escritura)
docs/gestion/STATUS.md                                                              (estado → VALIDADA)
docs/gestion/CHANGELOG.md                                                           (audit results)
docs/gestion/ROADMAP.md                                                             (estado, test count, Fase 5C corregida)
.claude/TASK_CURRENT.md                                                             (VALIDADA)
apps/api/README.md                                                                  (write endpoints docs)
apps/admin-web/README.md                                                            (5B-2 implemented)
docs/operacion/DOCKER_LOCAL.md                                                      (rebuild instructions)
docs/arquitectura/MODULOS.md                                                        (write controllers)
docs/dominio/REGLAS_NEGOCIO.md                                                      (new rules: último admin, auto-lockout, BCrypt limit, locking, ortogonales, revocación)
docs/producto/REQUISITOS_FUNCIONALES.md                                             (RF-07g)
docs/producto/HISTORIAS_USUARIO.md                                                  (HU-007 completada)
```

---

## Siguiente acción exacta

**VALIDACIÓN COMPLETA. Fase 5B-2 lista para commit y merge a main.**

Pasos para cerrar:
1. `git add -A` y `git commit -m "feat: implementar gestión administrativa de usuarios escritura fase 5b-2"` (pedir autorización primero)
2. `git checkout main && git merge feature/fase-5b-2-usuarios-escritura`
3. `git tag v0.16.0-usuarios-admin-escritura`
4. Iniciar Fase 5B-3 o Fase 5C según prioridad del negocio

---

## Decisiones clave de implementación

- **activo y bloqueado son ortogonales** (ADR-0048). Un usuario puede tener ambos flags simultáneamente.
- **BEFORE_COMMIT para revocación de sesiones** (ADR-0047). Atómico con la operación de modificación.
- **Spring Modulith — handlers de dominio en el controlador del módulo**. `GlobalExceptionHandler` (root package) no puede referenciar tipos no expuestos de `usuarios.dominio`.
- **Validación de contraseña — bytes UTF-8**: `contrasena.getBytes(StandardCharsets.UTF_8).length > 72`.
- **Locking optimista manual + @Version**: Service compara versión antes de modificar; JPA `@Version` añade segunda capa.
- **Roles iniciales en creación**: La única forma de asignar roles es al crear el usuario.
- **Fase 5C = Importación mensual de datos de cobranza** (personas, operaciones, cuotas, asignaciones) — NO importación de usuarios.

---

## No repetir

- Entorno Docker CERRADO — commit `1a22c8a`, tag `v0.15.0-entorno-docker-local`
- Fase 5B-1 CERRADA — commit `d82d95d`, tag `v0.14.0-usuarios-admin-readonly`
- Spring Modulith: exponer operaciones entre módulos solo via interfaces en `*.api` (@NamedInterface)
- `GlobalExceptionHandler` raíz NO puede importar tipos de `usuarios.dominio` (violation)
- El `SeguridadUsuarioModificadaEvent` está en `usuarios.api` (paquete expuesto) — no en `usuarios.dominio`
- En Alpine, `localhost` puede resolver a IPv6 — usar `127.0.0.1` en healthchecks
- `Map.of()` lanza NPE con claves null — usar `HashMap` cuando las claves pueden ser null
- `permissionGuard` ya implementado
- `WebOriginValidationFilter` ya implementado
- No agregar tablas de auditoría persistente — logs estructurados únicamente
- No implementar importación CSV de usuarios — Fase 5C es importación mensual de datos de cobranza
