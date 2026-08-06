# Handoff de sesión — Entorno Docker local AUDITADO Y VALIDADO ✅

**Fecha:** 2026-08-05
**Rama activa:** `feature/fase-5b-usuarios-admin`

---

## Estado de Git

| Referencia | Hash | Descripción |
|---|---|---|
| `main` | `6009e11` | docs: preparar contexto para fase 5b |
| `feature/fase-5b-usuarios-admin` | HEAD | Entorno Docker implementado y auditado — SIN COMMIT (pendiente autorización) |

> `git status --short` muestra archivos modificados y nuevos — todos relacionados con Docker.
> `git diff --check` — OK (sin errores de whitespace).

---

## Entorno Docker local — Implementado y auditado

### Archivos nuevos (untracked)

```
apps/api/Dockerfile
apps/api/.dockerignore
apps/api/entrypoint.sh
apps/api/src/main/resources/application-docker.yml
apps/api/src/main/java/cl/zzenner/cobranza/DevSeedRunner.java
apps/api/src/main/java/cl/zzenner/cobranza/usuarios/api/UsuarioSeedApi.java
apps/api/src/main/java/cl/zzenner/cobranza/usuarios/aplicacion/UsuarioSeedService.java
apps/api/src/test/java/cl/zzenner/cobranza/DevSeedRunnerTest.java
apps/admin-web/Dockerfile
apps/admin-web/.dockerignore
apps/admin-web/nginx.conf
infrastructure/nginx/admin-web.conf
scripts/generar-claves.sh
scripts/levantar-entorno.sh
scripts/smoke-test.sh
docs/operacion/DOCKER_LOCAL.md
```

### Archivos modificados

```
compose.yaml
.env.example
.gitignore                          — excluye infrastructure/dev-keys/
README.md
apps/api/README.md
apps/admin-web/README.md            — sección Docker añadida (Section 13, última acción)
docs/arquitectura/ARQUITECTURA_GENERAL.md
docs/operacion/DESARROLLO_LOCAL.md
docs/gestion/STATUS.md
docs/gestion/CHANGELOG.md
docs/gestion/ROADMAP.md
.claude/SESSION_HANDOFF.md
.claude/TASK_CURRENT.md
```

### Archivos generados localmente (no versionar)

```
infrastructure/dev-keys/private.pem   — gitignored ✅
infrastructure/dev-keys/public.pem    — gitignored ✅
.env                                  — gitignored ✅
```

---

## Resultados de auditoría completa (2026-08-05)

| Sección | Estado |
|---|---|
| 1. Protocolo de recuperación | ✅ |
| 2. Inventario de archivos | ✅ completo |
| 3. Docker Compose | ✅ healthchecks, dependencias, profiles |
| 4. Dockerfile API (su-exec, no root) | ✅ |
| 5. RSA keys (gitignored, permisos 400) | ✅ |
| 6. Dockerfile Angular/Nginx | ✅ |
| 7. DevSeedRunner (seed idempotente) | ✅ |
| 8. Scripts (levantar + smoke-test) | ✅ |
| 9. Reinicio limpio (down -v + build --no-cache + up) | ✅ |
| 10. Logs (sin secretos) | ✅ |
| 11. Pruebas manuales en browser | ✅ (login, /usuarios, /usuarios/:id, logout) |
| 12. Suites completas | ✅ 329 API + 50 Angular + 14 Playwright |
| 13. Documentación | ✅ (DOCKER_LOCAL, README raíz, api/README, admin-web/README) |
| 14. Estado final git | ✅ |

### Resultados de suites

| Suite | Resultado |
|---|---|
| API — `mvn clean verify` | ✅ **329 tests — 0 failures — BUILD SUCCESS** |
| Angular — `npm run test:ci` | ✅ **50 tests — 0 failures** |
| Angular coverage | ✅ 84% statements / 82% branches |
| npm audit | ✅ 0 high/critical |
| Playwright | ✅ **14/14 passed** |
| Docker — 3 servicios | ✅ postgres + api + admin-web **healthy** |
| Smoke tests | ✅ **24/24 OK** |

### Estado Docker (Section 14)

```
NAME                   SERVICE     STATUS          PORTS
cobranza-admin-web-1   admin-web   Up (healthy)    0.0.0.0:8080->80/tcp
cobranza-api-1         api         Up (healthy)    0.0.0.0:8081->8080/tcp
cobranza_postgres      postgres    Up (healthy)    0.0.0.0:5432->5432/tcp
```

---

## Correcciones aplicadas durante el entorno Docker

1. **Spring Modulith violation** — `DevSeedRunner` accedía a tipos no expuestos. Corregido con `UsuarioSeedApi` (interfaz pública) + `UsuarioSeedService` (implementación interna al módulo).
2. **Nginx `log_format`** — solo permitido en bloque `http`, no `server`. Removido.
3. **Alpine IPv6 healthcheck** — `localhost` resuelve a `::1`. Cambiado a `127.0.0.1` en compose.yaml (admin-web healthcheck).
4. **API Dockerfile UID frágil** — `adduser -u 1000` dependía del UID del host. Reemplazado con `su-exec` (entrypoint corre como root, copia claves con `install -m 400 -o appuser`, hace drop a `appuser`).
5. **Login smoke test** — campo es `clave`, no `password` (`SolicitudLoginWeb`).
6. **Proxy smoke test** — `/api/actuator/health` no existe (actuator en `/actuator/`). Cambiado a `/api/v1/auth/me` (no-5xx = proxy funcional).
7. **Post-logout token** — JWT access token es stateless; sigue válido hasta expirar. Test cambiado a verificar que el REFRESH token queda invalidado en BD (retorna 401 en `/auth/web/refresh`).
8. **Process user check** — `ps -o user` no disponible en Alpine sin `procps`. Fallback a `/proc/1/status Uid`.

---

## Siguiente acción exacta

Solicitar autorización para commit del entorno Docker e implementar Fase 5B-2.

Antes de implementar Fase 5B-2, leer:
1. `docs/dominio/REGLAS_NEGOCIO.md` — RN-06, RN-28
2. `apps/api/src/main/java/cl/zzenner/cobranza/usuarios/aplicacion/UsuarioService.java`
3. `apps/api/src/main/java/cl/zzenner/cobranza/usuarios/dominio/Usuario.java`
4. `.claude/TASK_CURRENT.md` — alcance completo de 5B-2

---

## No repetir

- Fase 5B-1 CERRADA — commit `d82d95d`, tag `v0.14.0-usuarios-admin-readonly`
- Spring Modulith: exponer operaciones entre módulos solo via interfaces en `*.api`
- En Alpine, `localhost` puede resolver a IPv6 — usar `127.0.0.1` en healthchecks
- `SolicitudLoginWeb.clave` (no `password`) es el campo de contraseña web
- JWT access token es stateless — no se invalida en logout, solo el refresh token (BD)
- `log_format` solo en bloque `http` de Nginx, no en `server`
- `su-exec` es el patrón correcto para drop de privilegios en Alpine (sin `gosu`, sin dependencia de UID host)
