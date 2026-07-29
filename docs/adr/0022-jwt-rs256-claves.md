# ADR-0022: JWT con RS256 y gestión de claves RSA externas

- **Estado**: Aprobado
- **Fecha**: 2026-07-28
- **Autores**: Milton Salazar

## Contexto

El sistema emite access tokens para autenticar llamadas de la app Android a la API. Se requiere un formato estándar, firmado y verificable sin consultar base de datos.

## Decisión

Se utiliza JWT con algoritmo **RS256** (RSA 2048-bit, PKCS#1). Las claves se cargan desde rutas del sistema de archivos configuradas por variables de entorno (`JWT_RSA_PRIVADA_PATH`, `JWT_RSA_PUBLICA_PATH`). **Las claves privadas nunca se almacenan en el repositorio**.

Claims del access token: `sub` (UUID usuario), `sid` (UUID sesión), `did` (UUID dispositivo), `preferred_username`, `roles`, `permisos`, `iss`, `aud`, `iat`, `exp`, `jti`, `kid`.

Duración del access token: **15 minutos** (configurable).

El `JwtDecoder` valida explícitamente: algoritmo RS256, firma, fechas (`iat`/`exp`), emisor (`iss`) y audiencia (`aud`).

En pruebas se genera un par RSA en memoria con `KeyPairGenerator`; nunca se usa el sistema de archivos.

## Consecuencias

- Los access tokens son stateless: no se puede revocar un token antes de su expiración (riesgo aceptado del MVP, documentado).
- Se requiere gestión segura del par de claves fuera del repositorio (responsabilidad operacional).
- La rotación de claves implica actualizar variables de entorno y reiniciar el servicio.
