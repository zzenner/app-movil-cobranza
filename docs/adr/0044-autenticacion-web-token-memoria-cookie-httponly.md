# ADR-0044: Autenticación web — access token en memoria, refresh token en cookie HttpOnly

**Fecha:** 2026-08-03
**Estado:** Aceptado
**Contexto:** Fase 5A — Autenticación del panel administrativo web

## Contexto

El panel web necesita autenticación JWT. La app Android guarda el refresh token en el dispositivo. Para SPA no aplica la misma estrategia: localStorage/sessionStorage son accesibles desde JavaScript y vulnerables a XSS.

## Decisión

### Access token
- Almacenado únicamente en **memoria de proceso** (`TokenStorageService`).
- Se pierde al recargar la página; se renegocia automáticamente vía refresh.
- Nunca en `localStorage`, `sessionStorage`, ni URL.

### Refresh token
- Solo viaja como **cookie HttpOnly; SameSite=Strict; Secure=true** (en prod).
- JavaScript nunca puede leer el valor.
- El servidor lo lee en `POST /api/v1/auth/web/refresh` via `@CookieValue`.
- **No aparece** en respuestas JSON, logs ni URLs.

### Separación de sesiones Android/WEB
- Nueva columna `tipo_cliente VARCHAR(10)` en `sesiones_autenticacion`.
- ANDROID: `dispositivo_id` NOT NULL; WEB: `dispositivo_id` NULL.
- Constraints CHECK garantizan la invariante en BD.
- Índices parciales únicos independientes por tipo.

### Single-flight refresh
- `AuthService.refresh()` retorna el mismo `Observable` para llamadas concurrentes.
- Evita renovaciones en paralelo que crearían múltiples refresh tokens activos.

### Bootstrap de sesión
- `APP_INITIALIZER` llama a `SessionBootstrapService.bootstrap()`.
- Intenta renovar → carga perfil → estado `AUTENTICADA`.
- En fallo → estado `NO_AUTENTICADA`.
- Guards esperan a que el estado salga de `INICIALIZANDO` antes de decidir.

## Consecuencias

**Positivas:**
- Inmune a ataques XSS que intenten robar el refresh token.
- Sin estado de sesión en el servidor (stateless JWT).
- Rotación de refresh tokens con reuse-detection (misma lógica que Android).

**Negativas / Riesgo:**
- El access token (en memoria) sobrevive a un XSS dentro de la página. Esta es la misma exposición que cualquier SPA con tokens. La mitigación es el CSP y la corta vida del token (15 min).
- Al recargar la página se pierde el AT; el bootstrap lo renegocia automáticamente.

**Referencias:**
- [OAuth 2.0 for Browser-Based Applications (RFC)](https://oauth.net/2/browser-based-apps/)
- ADR-0026: JWT RS256 y gestión de tokens (Android)
