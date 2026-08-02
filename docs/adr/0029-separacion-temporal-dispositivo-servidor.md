# ADR-0029: Separación entre tiempo del dispositivo y tiempo del servidor

**Estado:** Aceptada  
**Fecha:** 2026-08-01

## Contexto

En un flujo offline-first, el dispositivo registra la gestión con el reloj local y la envía al servidor más tarde. El timestamp de creación en el servidor puede ser horas posterior al timestamp del dispositivo. Mezclar ambos tiempos en una sola columna genera ambigüedad.

## Decisión

Se mantienen dos timestamps separados en la tabla `gestiones`:

- **`fecha_gestion TIMESTAMPTZ NOT NULL`**: momento en que el ejecutivo realizó la gestión, según el dispositivo. Puede ser anterior (offline) o casi simultáneo al envío. Es el tiempo de negocio.
- **`fecha_creacion_servidor TIMESTAMPTZ NOT NULL`**: momento en que el servidor recibió y persistió la gestión. Es el tiempo técnico de procesamiento.

La validación de `fecha_compromiso` se realiza en Java usando `fecha_gestion` como referencia, con la zona horaria de negocio (`America/Santiago`). No se depende únicamente de `fecha_gestion::date` (UTC) en PostgreSQL, que podría diferir de la fecha local en Chile en horas límite del día.

## Consecuencias

- Permite detectar gestiones enviadas con gran retraso (`fecha_creacion_servidor - fecha_gestion > umbral`)
- Facilita auditorías y análisis de latencia de sincronización
- La API web debe asignar `fecha_creacion_servidor = Instant.now()` en el momento de recepción (no confiar en el valor del cliente)
