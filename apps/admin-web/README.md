# Panel Administrativo — Cobranza

Aplicación Angular 22 para supervisores y administradores del sistema de cobranza.

## Stack

| Tecnología | Versión |
|---|---|
| Angular | 22.1.0 |
| TypeScript | ~6.0.2 |
| Angular Material | 22.1.0 |
| Vitest | ^4.0.8 |
| Playwright | ^1.62.1 |

## Requisitos

- Node.js 22+ (solo para desarrollo con hot reload fuera de Docker)
- API ejecutándose en `http://localhost:8080` (ver `apps/api/`)

## Entorno Docker local (recomendado para pruebas manuales)

El panel se sirve desde Nginx dentro del entorno Docker junto con la API y PostgreSQL.
**No requiere Node.js instalado localmente.**

```bash
# Desde la raíz del repositorio
cp .env.example .env           # Solo la primera vez — ajustar passwords
./scripts/generar-claves.sh    # Solo la primera vez — genera RSA en infrastructure/dev-keys/
./scripts/levantar-entorno.sh  # Levanta PostgreSQL + API + Admin Web
./scripts/smoke-test.sh        # Verifica 24 puntos de control
```

| URL | Descripción |
|-----|-------------|
| `http://localhost:8080` | Admin Web (Nginx sirve el SPA) |
| `http://localhost:8080/api/v1/...` | Proxy Nginx → API (mismo origen) |
| `http://localhost:8081/actuator/health` | API health check directo |

### Variables de entorno relevantes (`.env`)

| Variable | Descripción |
|----------|-------------|
| `ADMIN_WEB_PORT` | Puerto de Nginx (default: `8080`) |
| `WEB_ALLOWED_ORIGIN` | Origen permitido para cookies web (default: `http://localhost:8080`) |
| `WEB_COOKIE_SECURE` | `false` en HTTP local, `true` en producción HTTPS |
| `DEV_ADMIN_USERNAME` | Usuario de prueba creado automáticamente al levantar |
| `DEV_ADMIN_PASSWORD` | Contraseña del usuario de prueba |

### Nginx en Docker

- Imagen: `nginx:1.27-alpine`
- Sirve el build Angular desde `/usr/share/nginx/html`
- SPA fallback: todas las rutas devuelven `index.html` (`try_files`)
- Proxy `/api/*` → `api:8080` (preserva la ruta completa, incluye `proxy_set_header Origin`)
- Health check interno: `GET /nginx-health` → 200
- Configuración: `apps/admin-web/nginx.conf`

### Operación habitual

```bash
# Reiniciar solo el panel web (rebuild de Angular + Nginx)
docker compose up --build -d admin-web

# Ver logs
docker compose logs admin-web --tail=50 --follow

# Detener todo (conserva volumen PostgreSQL)
docker compose down

# Limpieza completa (elimina BD + volúmenes)
docker compose down -v
```

## Desarrollo local con hot reload (sin Docker)

Requiere que la API esté corriendo localmente (con perfil `local`) o en Docker.

```bash
cd apps/admin-web
npm ci
npm start          # http://localhost:4200, proxy a localhost:8080
```

El proxy de desarrollo (`proxy.conf.json`) redirige `/api/**` → `http://localhost:8080`.
Si la API está en Docker (puerto `:8081`), ajustar `proxy.conf.json` a `http://localhost:8081`
o levantar la API directamente con `./mvnw spring-boot:run`.

**Android** no forma parte del Compose local — conecta a la API directamente por su propia red.

## Pruebas

```bash
npm run test:ci        # Vitest, modo CI (sin watch)
npm run test:coverage  # Vitest con cobertura
npm run e2e            # Playwright (requiere servidor en :4200)
```

## Build producción

```bash
npm run build          # dist/admin-web/
```

## Funcionalidades implementadas

| Ruta | Permiso | Descripción |
|---|---|---|
| `/login` | — | Formulario de acceso. |
| `/home` | autenticado | Pantalla de inicio con perfil real. |
| `/usuarios` | `USUARIOS_VER` | Listado paginado de usuarios con filtros (nombreUsuario, estado, rol). |
| `/usuarios/:id` | `USUARIOS_VER` | Detalle de usuario: roles, permisos efectivos, supervisor, estado calculado. |
| `/forbidden` | — | Página 403. |

## Autenticación

- Login web en `POST /api/v1/auth/web/login` — no requiere `identificadorInstalacion`
- Access token: en memoria únicamente (nunca en localStorage/sessionStorage)
- Refresh token: cookie HttpOnly `rt_web`, SameSite=Strict, Path=/api/v1/auth/web/refresh
- Bootstrap: al iniciar la app, intenta renovar la sesión; si falla, redirige a /login
- Guards funcionales: `authGuard` (autenticación), `permissionGuard` (permisos — Fase 5B-1), `loginGuard` (redirect si ya autenticado)

## Seguridad

- Refresh y logout requieren header `Origin` con el origen permitido (defensa en profundidad)
- Origen permitido configurado con variable `WEB_ALLOWED_ORIGIN` (default: `http://localhost:4200`)
- Single-flight refresh: múltiples peticiones concurrentes de renovación comparten una sola solicitud HTTP

## Proxy de desarrollo

`proxy.conf.json` redirige `/api/**` a `http://localhost:8080` para evitar problemas de CORS en desarrollo.
En producción, el proxy/reverse proxy del servidor sirve el mismo origen.

## Resolución de problemas en Docker

| Síntoma | Causa habitual | Solución |
|---------|----------------|----------|
| Pantalla en blanco / 502 | API no lista aún | Esperar — el health check de Nginx depende de la API |
| Error CORS en consola | `WEB_ALLOWED_ORIGIN` no coincide con el origen real | Verificar variable en `.env` y reiniciar |
| Login falla (403) | Origin en la petición no coincide con `WEB_ALLOWED_ORIGIN` | Acceder desde el puerto correcto (`ADMIN_WEB_PORT`) |
| `<app-root>` no aparece | Build Angular no incluido en imagen | Ejecutar `docker compose up --build -d admin-web` |
| Cambios JS/HTML no se ven | Caché del navegador | Ctrl+Shift+R o reconstruir imagen |

```bash
# Diagnóstico rápido
docker compose ps
docker compose logs admin-web --tail=50
docker compose logs api --tail=50
```

## Funcionalidades implementadas (Fase 5B-2) ✅

Gestión administrativa de usuarios con permiso `USUARIOS_ADMINISTRAR`:

- **Crear usuario** — formulario con nombre de usuario, nombres, correo (opcional), contraseña y roles iniciales.
- **Editar usuario** — editar nombres y correo con protección de versión concurrente.
- **Activar / Desactivar usuario** — con diálogo de confirmación.
- **Bloquear / Desbloquear usuario** — con diálogo de confirmación.
- **Restablecer contraseña** — diálogo que solicita la nueva contraseña.
- Botón "Nuevo usuario" en el listado (visible solo con `USUARIOS_ADMINISTRAR`).
- Protección de propia cuenta: los botones desactivar/bloquear se ocultan cuando el usuario viendo el detalle es el propio actor.

## ADRs relacionados

- `docs/adr/0043-angular-22-arquitectura-admin-web.md`
- `docs/adr/0044-autenticacion-web-token-memoria-cookie-httponly.md`
- `docs/adr/0045-ejecucion-local-proxy-mismo-origen.md`
