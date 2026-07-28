# ADR-0013 — UUID de gestiones generado en el dispositivo Android

## Estado
Aceptado.

## Contexto
Las gestiones se crean en el dispositivo Android, posiblemente sin conexión a internet. Se necesita decidir quién genera el identificador único de la gestión: el dispositivo (en el momento del registro) o el servidor (al recibir la gestión).

## Decisión
El **UUID de cada gestión se genera en el dispositivo Android** en el momento del registro, antes de cualquier comunicación con el servidor. Este UUID es el identificador definitivo de la gestión tanto en Room como en la API.

## Consecuencias

**Positivas:**
- La gestión tiene un identificador permanente desde el momento de su creación, sin esperar confirmación del servidor.
- El reenvío de la misma gestión (por fallo de red, reintento de WorkManager) no genera duplicados: la API recibe el mismo UUID y lo acepta de forma idempotente.
- El dispositivo puede referenciar la gestión localmente (para adjuntar fotografías, vincular ubicación) desde el momento del registro.
- No se requiere ninguna lógica de reconciliación de identidades entre el UUID local y el ID del servidor.

**Negativas:**
- En la hipotética (y prácticamente imposible) colisión de UUID v4, la API deberá rechazar la gestión con error permanente. No se espera que ocurra en condiciones normales.
- El servidor no controla el rango ni el formato del identificador, aunque puede validar que sea un UUID v4 bien formado.

## Alternativas consideradas

**ID generado por el servidor:** El dispositivo envía la gestión sin ID; el servidor devuelve el ID asignado. Requiere conexión para obtener el ID antes de poder referenciar la gestión localmente. Incompatible con el modelo offline-first. Se descartó.

**ID secuencial local + ID del servidor:** El dispositivo usa un ID temporal local; al recibir confirmación del servidor, mapea al ID definitivo. Introduce complejidad en la capa de Room y en la lógica de fotografías y referencias. Se descartó.

## Referencias

- `docs/dominio/REGLAS_NEGOCIO.md` — RN-14 (estados de sincronización)
- `docs/sincronizacion/ESTRATEGIA_OFFLINE.md` — UUID en el dispositivo
- `docs/sincronizacion/PROTOCOLO_SINCRONIZACION.md` — Idempotencia por UUID
- `docs/adr/0009-gestiones-inmutables.md`
