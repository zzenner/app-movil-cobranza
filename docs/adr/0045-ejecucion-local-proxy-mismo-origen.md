# ADR-0045: Ejecución local del panel web — proxy same-origin

**Fecha:** 2026-08-03
**Estado:** Aceptado
**Contexto:** Fase 5A — Desarrollo local del panel administrativo web

## Contexto

En desarrollo, el panel Angular corre en `localhost:4200` y la API en `localhost:8080`. La cookie `SameSite=Strict` del refresh token se envía solo a requests del mismo origen, lo que rompería el flujo de autenticación con CORS tradicional.

## Decisión

Se usa **proxy de desarrollo** de Angular (`proxy.conf.json`) que redirige `/api/**` al puerto de la API:

```json
{ "/api": { "target": "http://localhost:8080", "secure": false, "changeOrigin": true } }
```

Esto hace que el browser vea todas las peticiones al **mismo origen** (`localhost:4200`), por lo que:
- La cookie `rt_web` se envía correctamente en cada `POST /api/v1/auth/web/refresh`.
- No se necesita CORS entre frontend y API en desarrollo.
- No se necesita `Secure=false` solo por CORS; aplica por ser HTTP local.

### Configuración de cookie
- `app.web.cookie.secure=${WEB_COOKIE_SECURE:false}` — false en local, true en producción.
- Se pasa como variable de entorno en el despliegue.

## Consecuencias

**Positivas:**
- Mismo comportamiento que producción respecto a cookies.
- No requiere headers CORS adicionales en la API durante desarrollo.

**Negativas / Riesgo:**
- En producción, el despliegue debe garantizar mismo origen o usar un balanceador de carga que proxie `/api` al backend. Esta decisión de infraestructura queda pendiente para fase posterior.

**No implementado en esta fase:**
- HTTPS local (mkcert o similar) — se puede agregar en fases posteriores si es necesario.
- Configuración de producción (reverse proxy Nginx/Traefik) — fuera del alcance de 5A.
