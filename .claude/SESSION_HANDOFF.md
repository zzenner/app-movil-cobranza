# Handoff de sesión — Entorno Docker local CERRADO ✅ — Fase 5B-2 pendiente

**Fecha:** 2026-08-05
**Rama activa:** `feature/fase-5b-2-usuarios-escritura`

---

## Estado de Git

| Referencia | Hash | Descripción |
|---|---|---|
| `main` (local + origin) | `1a22c8a` | feat(infra): implementar entorno docker local integrado |
| `v0.15.0-entorno-docker-local` | `1a22c8a` | Tag Docker — publicado ✅ |
| `feature/fase-5b-2-usuarios-escritura` | HEAD | Igual a main + commit de contexto |
| `v0.14.0-usuarios-admin-readonly` | `d82d95d` | Tag Fase 5B-1 — publicado ✅ |

---

## Entorno Docker local — CERRADO ✅

### Comando principal

```bash
./scripts/levantar-entorno.sh   # PostgreSQL + API + Admin Web
./scripts/smoke-test.sh         # 24 pruebas de validación
```

### Validación definitiva

| Suite | Resultado |
|---|---|
| API — `mvn clean verify` | ✅ **329 tests — 0 failures — BUILD SUCCESS** |
| Angular — `npm run test:ci` | ✅ **50 tests — 0 failures** |
| Angular coverage | ✅ 84% statements / 82% branches |
| npm audit | ✅ 0 high/critical (3 moderate — no acción requerida) |
| Playwright e2e | ✅ **14/14 passed** |
| Docker compose | ✅ 3 servicios healthy |
| Smoke tests | ✅ **24/24 OK** |

### Servicios Docker (al cierre)

```
NAME                   SERVICE     STATUS          PORTS
cobranza-admin-web-1   admin-web   Up (healthy)    0.0.0.0:8080->80/tcp
cobranza-api-1         api         Up (healthy)    0.0.0.0:8081->8080/tcp
cobranza_postgres      postgres    Up (healthy)    0.0.0.0:5432->5432/tcp
```

### Archivos clave del entorno Docker

```
compose.yaml
scripts/generar-claves.sh
scripts/levantar-entorno.sh
scripts/smoke-test.sh
apps/api/Dockerfile + entrypoint.sh
apps/admin-web/Dockerfile + nginx.conf
docs/operacion/DOCKER_LOCAL.md
```

---

## Siguiente acción exacta

**"Revisar `UsuarioService`, `Usuario`, reglas de estado y contraseña para planificar Fase 5B-2".**

Leer en orden:
1. `docs/dominio/REGLAS_NEGOCIO.md` — RN-06 (roles), RN-28 (estado calculado)
2. `apps/api/src/main/java/cl/zzenner/cobranza/usuarios/aplicacion/UsuarioService.java`
3. `apps/api/src/main/java/cl/zzenner/cobranza/usuarios/dominio/Usuario.java`
4. `.claude/TASK_CURRENT.md` — alcance completo de 5B-2

---

## No repetir

- Entorno Docker CERRADO — commit `1a22c8a`, tag `v0.15.0-entorno-docker-local`
- Fase 5B-1 CERRADA — commit `d82d95d`, tag `v0.14.0-usuarios-admin-readonly`
- Fase 5A CERRADA — commit `71d47b2`, tag `v0.13.0-admin-base`
- Spring Modulith: exponer operaciones entre módulos solo via interfaces en `*.api` (@NamedInterface)
- En Alpine, `localhost` puede resolver a IPv6 — usar `127.0.0.1` en healthchecks
- `SolicitudLoginWeb.clave` (no `password`) es el campo de contraseña web
- JWT access token es stateless — no se invalida en logout, solo el refresh token (BD)
- `log_format` solo en bloque `http` de Nginx, no en `server`
- `su-exec` es el patrón correcto para drop de privilegios en Alpine
- `GlobalExceptionHandler` mapea `ConstraintViolationException` → 422; usar validación manual para 400
- `Map.of()` lanza NPE con claves null — usar `HashMap` cuando las claves pueden ser null
- `permissionGuard` ya implementado
- `WebOriginValidationFilter` ya implementado
