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

- Node.js 22+
- API ejecutándose en `http://localhost:8080` (ver `apps/api/`)

## Desarrollo local

```bash
cd apps/admin-web
npm ci
npm start          # http://localhost:4200, proxy a localhost:8080
```

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

## ADRs relacionados

- `docs/adr/0043-angular-22-arquitectura-admin-web.md`
- `docs/adr/0044-autenticacion-web-token-memoria-cookie-httponly.md`
- `docs/adr/0045-ejecucion-local-proxy-mismo-origen.md`
