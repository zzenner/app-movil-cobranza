# ADR-0031 — `identificadorInstalacion` como campo de login en lugar de `dispositivoId`

**Estado:** Aceptado — 2026-08-02
**Contexto:** Fase 4A — Base Android

---

## Contexto

Hasta la Fase 2, el endpoint `POST /api/v1/auth/login` recibía el campo `dispositivoId` como un UUID que referenciaba un dispositivo ya registrado en la tabla `dispositivos`. Esto implicaba que el dispositivo debía estar pre-registrado administrativamente antes de poder hacer login.

Al iniciar la Fase 4A (app Android), se encontró el problema siguiente: la app Android no conoce el UUID interno del dispositivo en la base de datos, porque ese UUID lo genera el servidor en el momento del registro. El cliente tampoco puede registrar el dispositivo antes del login, porque el registro requiere autenticación.

## Decisión

Se cambia el contrato del endpoint de login:

- **Antes:** `dispositivoId: UUID` — referencia a un registro preexistente.
- **Ahora:** `identificadorInstalacion: String (UUID canónico)` — identificador generado por el dispositivo Android en la primera instalación.

La lógica de registro del dispositivo se mueve **dentro del flujo de login**, **después** de validar las credenciales del usuario:

1. El servidor valida nombre de usuario y contraseña.
2. Si las credenciales son válidas, el servidor busca un dispositivo con ese `identificadorInstalacion`.
   - Si no existe → se crea automáticamente asociado al usuario autenticado.
   - Si existe y pertenece al mismo usuario → se reutiliza.
   - Si existe y pertenece a otro usuario → se retorna 409.
   - Si existe pero está revocado → se retorna 401.
3. Se emite el JWT usando el UUID interno del dispositivo como `did` del payload.

## Consecuencias

### Positivas

- La app Android solo necesita generar y persistir un UUID local (`identificadorInstalacion`). No hay pre-registro manual.
- El flujo de onboarding queda en una sola llamada de red.
- Las credenciales incorrectas no registran dispositivos (registro solo ocurre post-validación).
- El UUID interno del dispositivo (PK de la tabla) nunca sale de la API; el cliente solo conoce su `identificadorInstalacion`.

### Negativas / Restricciones

- Una instalación con el mismo `identificadorInstalacion` no puede ser usada por dos usuarios distintos. Si un dispositivo se reasigna a otro usuario corporativamente, el `identificadorInstalacion` debe cambiar (nueva instalación o limpieza de datos).
- `dispositivoId` (UUID interno) ya no tiene utilidad como campo de API pública.

## Alternativas descartadas

- **Pre-registro de dispositivo vía endpoint separado (no autenticado):** añade complejidad al flujo de onboarding y requiere protección contra abuso (rate-limiting, CAPTCHA).
- **Pre-registro de dispositivo vía Admin Web:** requiere intervención administrativa para cada nueva instalación.
- **Mantener `dispositivoId` con un endpoint de registro público:** mismo problema que la primera alternativa.
