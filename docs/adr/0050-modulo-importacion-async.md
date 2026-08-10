# ADR-0050 — Procesamiento asíncrono del módulo de importación

**Estado:** Aceptado
**Fecha:** 2026-08-09
**Autores:** Milton Salazar

## Contexto

La validación y el procesamiento de un archivo CSV de importación mensual pueden tomar varios segundos (o minutos para archivos grandes). No es viable bloquear el hilo HTTP durante ese tiempo.

Se requiere un mecanismo de disparo asíncrono que:
- Sea transaccionalmente seguro (no dispare si la transacción principal hace rollback).
- Permita reintentos si el servidor se reinicia antes de completar la tarea.
- Sea integrable con el modelo de módulos de Spring Modulith.

## Decisión

Se usa el patrón **`@TransactionalEventListener(phase = AFTER_COMMIT)` + `@Async`**:

1. `ImportacionService.recibirImportacion()` / `confirmar()` guarda el estado y publica un evento de dominio (`ValidarImportacionEvento` / `ProcesarImportacionEvento`) dentro de la transacción.
2. Spring solo dispara el listener si la transacción hace commit exitosamente.
3. El listener está marcado `@Async("importacionExecutor")`, por lo que se ejecuta en el pool dedicado, liberando el hilo HTTP inmediatamente.
4. El worker interno usa `@Transactional(propagation = REQUIRES_NEW)` en beans separados (`ImportacionEstadoService`) para evitar el problema de proxy de Spring con auto-invocación.

La **recuperación de huérfanas** (`RecuperacionImportacionJob`) cubre el caso de reinicios de servidor: al arrancar, detecta importaciones en estado VALIDANDO/PROCESANDO más antiguas que el umbral configurado y las marca FALLIDA.

## Alternativas consideradas

1. **Cola de mensajes (RabbitMQ / Kafka)**: Mayor resiliencia y reintentos automáticos, pero añade infraestructura. Reservado para cuando el volumen lo justifique (ver `DEUDA_TECNICA.md`).
2. **`@Scheduled` polling sobre BD**: Más simple pero añade latencia y carga constante a la BD.
3. **`@Async` sin `AFTER_COMMIT`**: Riesgo de disparar el worker antes de que el estado esté persistido (condición de carrera).

## Consecuencias

- El executor `importacionExecutor` tiene core=2, max=4, cola=50. Para archivos muy grandes o importaciones simultáneas, puede necesitar ajuste.
- El timeout de `@Scheduled` y el `threshold-huerfanas` deben ser coherentes con la duración máxima esperada de un procesamiento.
- No hay reintentos automáticos ante fallos del worker: si el worker falla, el estado queda en VALIDANDO/PROCESANDO hasta que el job de recuperación lo marque FALLIDA. El operador debe volver a subir el archivo.
- Esta decisión es candidata a revisarse cuando se implemente la cola de mensajes (DT-IMX-001).
