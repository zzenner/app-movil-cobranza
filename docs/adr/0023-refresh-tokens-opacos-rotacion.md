# ADR-0023: Refresh tokens opacos con rotación atómica

- **Estado**: Aprobado
- **Fecha**: 2026-07-28
- **Autores**: Milton Salazar

## Contexto

Los access tokens de 15 minutos requieren un mecanismo de renovación sin re-autenticar al usuario. Se necesita protección contra robo de tokens y reutilización.

## Decisión

Se utiliza **refresh tokens opacos**: cadena aleatoria de 256 bits (32 bytes con `SecureRandom`), codificada en Base64URL sin padding. Solo el **hash SHA-256** (64 hex chars) se almacena en la tabla `cobranza.refresh_tokens`. El token crudo solo viaja en la respuesta HTTP; nunca se persiste.

**Rotación**: cada uso del refresh token lo marca como `CONSUMIDO` y genera un nuevo token. La operación es atómica: `@Transactional` con `@Lock(LockModeType.PESSIMISTIC_WRITE)` impide que dos renovaciones concurrentes produzcan dos tokens activos.

**Detección de reuso**: si se presenta un token `CONSUMIDO`, la sesión se marca como `COMPROMETIDA` y todos los tokens activos de la sesión se revocan.

El vencimiento del refresh token es el mínimo entre el límite deslizante (30 días por defecto) y el vencimiento absoluto de la sesión.

## Consecuencias

- La detección de reutilización compromete la sesión completa, no solo el token. El usuario deberá re-autenticarse.
- Los tokens consumidos se conservan en DB para detección de reutilización (crecimiento controlado; depuración operacional pendiente de Fase 3).
