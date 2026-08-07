# Handoff de sesión — Fase 5C EN PREPARACIÓN

**Fecha:** 2026-08-06
**Rama activa:** `feature/fase-5c-importacion-mensual`

---

## Estado de Git — Fase 5B-2 CERRADA

| Referencia | Hash | Descripción |
|---|---|---|
| `main` (local + origin) | `b3c4c8a` | feat: implementar gestion administrativa de usuarios fase 5b-2 |
| `v0.16.0-usuarios-admin-write` | `b3c4c8a` | Tag Fase 5B-2 — publicado ✅ |
| `feature/fase-5c-importacion-mensual` | `b3c4c8a` | Rama 5C — solo añade contexto |
| `v0.15.0-entorno-docker-local` | `1a22c8a` | Tag Docker — publicado ✅ |
| `v0.14.0-usuarios-admin-readonly` | `d82d95d` | Tag Fase 5B-1 — publicado ✅ |

---

## Cierre de Fase 5B-2 — COMPLETADO

| Acción | Resultado |
|---|---|
| Commit en `feature/fase-5b-2-usuarios-escritura` | ✅ `b3c4c8a` — 56 archivos |
| Push branch → origin | ✅ |
| Merge fast-forward a `main` | ✅ `1a22c8a..b3c4c8a` |
| Push `main` → origin | ✅ |
| Tag `v0.16.0-usuarios-admin-write` | ✅ publicado |
| Rama `feature/fase-5c-importacion-mensual` | ✅ en origin |

---

## Pruebas al cierre de 5B-2

| Suite | Resultado |
|---|---|
| API `./mvnw clean verify` | ✅ **371 tests — 0 failures** |
| Angular `npm run test:ci` | ✅ **94 tests — 0 failures** |
| Angular coverage | ✅ 84.1% Stmts, 81.35% Branch |
| Playwright `npm run e2e` | ✅ **26 tests — 0 failures** |
| Docker + smoke tests | ✅ **47 OK — 0 FALLIDO** |

---

## Fase 5C — IMPORTACIÓN MENSUAL ADMINISTRATIVA DE DATOS

### Objetivo

Cargar mensualmente la información de cobranza desde el sistema corporativo:
personas deudoras, operaciones, cuotas y asignaciones de carteras.

### Alcance preliminar

- CSV con personas, operaciones, cuotas, asignaciones
- Identificadores externos: `persona_ext_id`, `operacion_ext_id`, `cuota_ext_id`
- Validación previa con errores por fila antes de persistir
- Procesamiento transaccional y controlado
- Resumen post-carga (creadas, actualizadas, rechazadas)
- Prevención de duplicados
- Trazabilidad (quién, cuándo, cuántos registros)
- Interfaz en admin-web para subir el CSV y ver resultados

### Fuera de alcance inicial

- XLSX (salvo decisión posterior)
- Integración automática con API corporativa externa
- Jobs programados
- Importación Android
- Importación de usuarios (cubierta en 5B-2)

### Preguntas abiertas

- Formato CSV definitivo (columnas, separador, encoding, cabecera)
- Comportamiento ante existentes: reemplazar vs. ignorar vs. rechazar
- Atomicidad: lote completo vs. fila a fila
- Tamaño esperado de archivos CSV corporativos
- Carga mensual vs. carga inicial histórica — ¿mismo flujo?
- Significado exacto de `persona_ext_id`, `operacion_ext_id`, `cuota_ext_id`
- Relación RUT ↔ operación ↔ cuota en el sistema corporativo

---

## Siguiente acción exacta

"Analizar el modelo de datos y los documentos existentes para diseñar el contrato CSV
y el flujo de importación mensual de Fase 5C".

Leer en este orden:
1. `docs/dominio/REGLAS_NEGOCIO.md` — RN-01 a RN-15
2. `docs/dominio/DIAGRAMA_ENTIDAD_RELACION.md`
3. `docs/dominio/DICCIONARIO_DATOS_PRELIMINAR.md`
4. Migraciones Flyway V006–V009 en `apps/api/src/main/resources/db/migration/`
5. Entidades JPA: `Persona`, `Operacion`, `Cuota`, `Cartera`, `AsignacionMensual`
6. `docs/producto/REQUISITOS_FUNCIONALES.md` — RF-03 a RF-04
7. `docs/producto/HISTORIAS_USUARIO.md` — HU-008
8. ADR relacionados con carga inicial si existen

---

## No repetir (herencia de fases anteriores)

- `GlobalExceptionHandler` raíz NO puede importar tipos de módulos internos
- Exponer operaciones entre módulos solo via interfaces en `*.api` (@NamedInterface)
- No agregar tablas de auditoría persistente — logs estructurados únicamente
- En Alpine, `localhost` puede resolver a IPv6 — usar `127.0.0.1`
- `Map.of()` lanza NPE con claves null — usar `HashMap`
- No hacer push sin autorización explícita

---

## Historial de pruebas — serie anterior

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
