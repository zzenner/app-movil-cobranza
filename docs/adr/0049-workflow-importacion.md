# ADR-0049 — Workflow de estados para importaciones mensuales

**Estado:** Aceptado
**Fecha:** 2026-08-09
**Autores:** Milton Salazar

## Contexto

El proceso de importación mensual de datos de cobranza requiere pasar por varias etapas (recepción, validación, confirmación, procesamiento) antes de que los datos queden disponibles para los ejecutivos. La validación y el procesamiento son operaciones pesadas que no pueden ejecutarse de forma sincrónica en la petición HTTP.

Se necesita un workflow de estados que:
- Permita al usuario revisar los errores de validación antes de confirmar.
- Garantice que no haya dos importaciones del mismo período procesándose al mismo tiempo.
- Soporte recuperación automática de procesos interrumpidos (fallos de servidor, reinicios).
- Prevenga importaciones duplicadas (idempotencia por hash + período + cartera + sistema).

## Decisión

Se adopta un workflow lineal de 8 estados con las siguientes transiciones válidas:

```
RECIBIDA → VALIDANDO → VALIDADA → PROCESANDO → COMPLETADA
                     ↓                        ↘
               CON_ERRORES              CON_ERRORES (parcial)
VALIDANDO/PROCESANDO → FALLIDA (recuperación)
VALIDADA → EXPIRADA (TTL superado sin confirmar)
```

La **idempotencia** se garantiza con un índice único sobre `(hash_archivo, periodo, cartera_id, sistema_origen)` restringido a `estado = 'COMPLETADA'`. Un mismo archivo completado para el mismo período no puede reimportarse.

El **período regresivo** está prohibido: no se puede importar un período anterior al último completado para la misma cartera y sistema.

La **confirmación explícita** (VALIDADA → PROCESANDO) es obligatoria. Esto da al operador visibilidad sobre errores de validación antes de persistir datos.

## Alternativas consideradas

1. **Workflow automático sin confirmación**: La validación dispara el procesamiento directamente. Rechazado porque no permite al operador revisar advertencias antes de persistir.
2. **Estados en tabla separada (event sourcing)**: Más auditable, pero sobredimensionado para este MVP. Se puede introducir más adelante.
3. **Procesamiento síncrono**: Imposible por el volumen de datos y el límite de timeout HTTP.

## Consecuencias

- Los clientes deben implementar polling para detectar transiciones de estado.
- La recuperación de huérfanas (`RecuperacionImportacionJob`) debe ejecutarse al arranque y periódicamente para limpiar estados VALIDANDO/PROCESANDO abandonados.
- El período de TTL de importaciones VALIDADA no confirmadas (`app.importacion.recuperacion.ttl-validadas`) debe configurarse según la política operativa.
