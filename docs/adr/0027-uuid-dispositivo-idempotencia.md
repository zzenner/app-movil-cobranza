# ADR-0027: UUID generado en dispositivo e idempotencia concurrentemente segura

**Estado:** Aceptada  
**Fecha:** 2026-08-01  
**Revisada:** 2026-08-01

## Contexto

En un sistema offline-first, el dispositivo móvil genera la gestión antes de tener conectividad. Si el cliente reintenta el envío (por error de red, timeout, etc.), el servidor debe distinguir entre:

1. Reenvío del mismo registro (idempotente): no duplicar.
2. Colisión accidental de UUID con contenido diferente (conflicto): rechazar.

Adicionalmente, un mismo cliente puede disparar reintentos concurrentes (e.g., por HTTP timeout sin cancelación), lo que expone una ventana TOCTOU entre el `SELECT` de verificación y el `INSERT`.

## Decisión

El UUID de cada gestión es generado en el dispositivo Android (no en el servidor). El campo `id` es la clave primaria y no usa `@GeneratedValue`.

La estrategia de idempotencia es **en dos niveles**:

### Nivel 1 — Fast-path (caso común: reintento simple)
1. El servicio busca el UUID con `findById` antes de validar ni insertar.
2. Si existe y el contenido relevante es idéntico → responder con éxito (idempotente).
3. Si existe y el contenido difiere → lanzar `GestionConflictivaException` (HTTP 409).
4. Si no existe → continuar al Nivel 2.

### Nivel 2 — Insert atómico (caso de concurrencia)
5. Ejecutar `INSERT ... ON CONFLICT (id) DO NOTHING` via `GestionRepository.insertarSiNoExiste()`.
6. Si retorna 1 → fila insertada, flujo normal.
7. Si retorna 0 → otra solicitud concurrente con el mismo UUID ganó la carrera; releer y comparar contenido (pasos 2-3 del Nivel 1).

El "contenido relevante" comprende: `persona_id`, `ejecutivo_id`, `origen_gestion`, `asignacion_diaria_id`, `tipo_gestion`, `fecha_gestion`, `fecha_compromiso`, `latitud`, `longitud`.

## Por qué ON CONFLICT DO NOTHING y no manejo de excepción de PK

Si se usara `save()` y se capturara `DataIntegrityViolationException`, la transacción JPA quedaría marcada como rollback-only, requiriendo una transacción anidada (`REQUIRES_NEW`) para la recuperación. `ON CONFLICT DO NOTHING` resuelve el conflicto a nivel de sentencia SQL sin envenenar la transacción.

## Consecuencias

- No se usa `@GeneratedValue` en `Gestion`
- El dispositivo es responsable de generar UUIDs únicos (UUID v4)
- Los clientes deben implementar reintentos con backoff; colisiones accidentales de UUID son extremadamente improbables con UUID v4
- `GestionRepository.insertarSiNoExiste()` usa `@Modifying(clearAutomatically = true)` para invalidar la caché de primer nivel tras el INSERT nativo
- `GestionConflictivaException` se mapea a HTTP 409 en la capa web (Fase 3D)
- Un test de concurrencia (5 hilos simultáneos, mismo UUID) verifica la ausencia de duplicados
