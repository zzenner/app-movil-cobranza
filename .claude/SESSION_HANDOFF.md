# Handoff de sesión — Fase 5B-1 CERRADA ✅ / Fase 5B-2 PENDIENTE

**Fecha:** 2026-08-05
**Rama activa:** `feature/fase-5b-2-usuarios-escritura`

---

## Estado de Git

| Referencia | Hash | Descripción |
|---|---|---|
| `main` | `d82d95d` | feat: implementar consulta administrativa de usuarios fase 5b-1 |
| `origin/main` | `d82d95d` | Igual — publicado ✅ |
| `v0.14.0-usuarios-admin-readonly` | `d82d95d` | Tag de Fase 5B-1 — publicado ✅ |
| `feature/fase-5b-2-usuarios-escritura` | `d82d95d` | Rama nueva — nace desde main |

---

## Cierre Fase 5B-1

| Paso | Resultado |
|---|---|
| Commit | `d82d95d feat: implementar consulta administrativa de usuarios fase 5b-1` |
| Push feature branch | ✅ `feature/fase-5b-usuarios-admin` publicada |
| Merge main (fast-forward) | ✅ `71d47b2 → d82d95d` |
| Push main | ✅ publicado |
| Tag | ✅ `v0.14.0-usuarios-admin-readonly → d82d95d` publicado |
| Rama 5B-2 | ✅ `feature/fase-5b-2-usuarios-escritura` creada y publicada |

---

## Resultados finales verificados — Fase 5B-1

| Suite | Resultado |
|---|---|
| API — `mvn clean verify` | ✅ **323 pruebas — 0 failures — BUILD SUCCESS** |
| Angular — `npm run build` | ✅ BUILD SUCCESSFUL |
| Angular — `npm run test:ci` | ✅ **50 tests — 0 failures** |
| Angular — `npm run test:coverage` | ✅ core/auth 96.7%/100%, guards 100%/86.4%, core/http 88.9%/100%, usuarios-list 94.2%/80%, usuario-detail 85.7%/82.4% |
| Angular — `npm run e2e` | ✅ **14 tests Playwright — 14 passed** |
| `npm audit --audit-level=high` | ✅ 0 high/critical (3 moderate, no acción requerida) |

---

## Siguiente acción exacta

"Revisar las reglas de creación, actualización, estado y contraseña de usuarios para planificar Fase 5B-2".

Ver antes de implementar:
1. `docs/dominio/REGLAS_NEGOCIO.md` — RN-06, RN-28
2. `apps/api/src/main/java/cl/zzenner/cobranza/usuarios/aplicacion/UsuarioService.java` — operaciones existentes
3. `apps/api/src/main/java/cl/zzenner/cobranza/usuarios/dominio/Usuario.java` — invariantes de dominio
4. `.claude/TASK_CURRENT.md` — alcance completo de 5B-2

---

## No repetir

- Fase 5B-1 CERRADA — commit `d82d95d`, tag `v0.14.0-usuarios-admin-readonly`, main actualizado
- Fase 5A CERRADA — commit `71d47b2`, tag `v0.13.0-admin-base`
- `GlobalExceptionHandler` mapea `ConstraintViolationException` → 422; usar validación manual en controladores para devolver 400
- `Map.of()` lanza NPE con claves null — usar `HashMap` cuando las claves pueden ser null
- `permissionGuard` ya implementado — no reimplementar
- WebOriginValidationFilter ya implementado — no reimplementar
- V011 en producción — no modificar migración
- Fases Android 4A–4C-B completas — no tocar
