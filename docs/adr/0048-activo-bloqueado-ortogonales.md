# ADR-0048: Campos activo y bloqueado como estados ortogonales

**Estado:** Aceptado
**Fecha:** 2026-08-06
**Decisores:** Equipo de desarrollo

## Contexto

El modelo de usuario tiene dos flags de control de acceso: `activo` y `bloqueado`. Existe la pregunta
de si son mutuamente excluyentes o pueden coexistir.

## Decisión

`activo` y `bloqueado` son **ortogonales** (independientes entre sí). Un usuario puede estar al mismo
tiempo `activo=false` y `bloqueado=true`.

El estado calculado sigue la precedencia:
1. `activo=false` → INACTIVO (máxima prioridad)
2. `bloqueado=true` → BLOQUEADO
3. `bloqueadoHasta != null && ahora.isBefore(bloqueadoHasta)` → BLOQUEADO_TEMPORAL
4. En otro caso → ACTIVO

La operación `desbloquear` limpia **ambos** mecanismos de bloqueo: `bloqueado=false`,
`bloqueadoHasta=null` e `intentosFallidos=0`.

## Consecuencias

- Un usuario desactivado que además fue bloqueado seguirá viendo INACTIVO al reactivarlo sin desbloquear primero.
- La UI debe mostrar ambos flags por separado en el detalle para que el administrador comprenda el estado completo.
- El bug preexistente en `Usuario.desbloquear()` (que no limpiaba `bloqueadoHasta`) fue corregido en Fase 5B-2.
