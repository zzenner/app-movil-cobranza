# ADR-0024: Sesiones por par usuario-dispositivo con expiración absoluta

- **Estado**: Aprobado
- **Fecha**: 2026-07-28
- **Autores**: Milton Salazar

## Contexto

Se requiere controlar cuántas sesiones activas tiene un usuario y durante cuánto tiempo pueden mantenerse activas, independientemente de la actividad de renovación de tokens.

## Decisión

Cada par `(usuario_id, dispositivo_id)` puede tener **como máximo una sesión `ACTIVA`** a la vez (índice único parcial en DB). Un nuevo login cierra automáticamente la sesión previa del mismo dispositivo.

Cada sesión tiene un campo `fecha_vencimiento_abs` (no nulo) que define el vencimiento absoluto independientemente de las renovaciones de refresh tokens. Por defecto: **90 días**. El refresh token de una sesión nunca puede vencer después del `fecha_vencimiento_abs` de su sesión.

Los estados de sesión son: `ACTIVA`, `CERRADA` (logout normal o expiración), `COMPROMETIDA` (reuso de token detectado).

## Consecuencias

- El usuario deberá re-autenticarse cada 90 días aunque renueve tokens continuamente.
- El par `(usuario, dispositivo)` como clave de sesión simplifica la gestión y el análisis de sesiones activas.
- Si el dispositivo es revocado, la sesión activa de ese dispositivo queda inválida en el próximo refresh (no se invalida activamente en tiempo real en el MVP).
