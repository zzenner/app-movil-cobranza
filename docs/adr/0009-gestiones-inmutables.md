# ADR-0009 — Gestiones inmutables: sin rectificaciones ni anulaciones en el MVP

## Estado
Aceptado.

## Contexto
En un sistema offline-first, las gestiones se crean en el dispositivo y se sincronizan con el servidor. Se necesita definir si las gestiones pueden modificarse o anularse una vez registradas.

## Decisión
Las gestiones son **inmutables** desde el momento de su creación. No se implementan mecanismos de rectificación ni anulación en el MVP. Esta regla aplica a todos los roles, incluido el administrador.

## Consecuencias

**Positivas:**
- Simplifica significativamente el modelo de datos: no se necesitan tablas de versiones, eventos de corrección ni estados de anulación.
- Garantiza integridad del registro histórico: cada gestión es un hecho inmutable del pasado.
- Elimina conflictos de sincronización complejos que surgirían si una gestión offline pudiera ser anulada remotamente antes de llegar al servidor.
- Simplifica la lógica de idempotencia: el UUID identifica unívocamente el hecho registrado.

**Negativas:**
- Si un ejecutivo comete un error (tipo de gestión incorrecto, persona equivocada), no puede corregirlo. Puede registrar una nueva gestión aclaratoria, pero la errónea permanece en el historial.
- Los administradores tampoco pueden corregir gestiones. Esto puede generar fricción operacional.
- En el futuro, si se requiere un mecanismo de corrección, añadirlo implica trabajo adicional.

## Alternativas consideradas

**Gestiones mutables por administrador:** Permite corrección de errores, pero introduce complejidad: control de versiones, auditoría de cambios, posibles conflictos con gestiones offline. Se descartó para el MVP.

**Modelo de eventos de corrección:** Una gestión puede tener "eventos de anulación" o "gestiones de corrección" asociadas. Más correcto conceptualmente, pero demasiado complejo para el MVP. Se postergó para una fase futura si se confirma la necesidad.

## Notas
Esta decisión puede revisarse si el equipo de negocio confirma una necesidad operacional concreta de corrección. Al revisar, se recomienda evaluar el modelo de eventos de corrección (no mutación directa).
