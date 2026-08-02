# ADR-0026: Dos orígenes de gestión — ASIGNACION_DIARIA y BUSQUEDA_DIRECTA

**Estado:** Aceptada  
**Fecha:** 2026-08-01  
**Resuelve:** P-01 — ¿El ejecutivo puede registrar gestiones sobre personas fuera de su asignación diaria activa?

## Contexto

La especificación original asumía que toda gestión proviene de la asignación diaria del ejecutivo. Se identificó el caso de uso de "búsqueda directa": el ejecutivo localiza a un deudor fuera de su ruta planificada y debe poder registrar la gestión igualmente.

## Decisión

Se introduce el campo obligatorio `origen_gestion` con dos valores posibles:

- **`ASIGNACION_DIARIA`**: la gestión se origina en la ruta planificada. Requiere `asignacion_diaria_id` (NOT NULL). La diaria debe estar en estado `PUBLICADA` o `FINALIZADA` (este último acepta gestiones offline sincronizadas). El ejecutivo debe ser el destinatario de la diaria. La persona debe estar incluida en la diaria.
- **`BUSQUEDA_DIRECTA`**: el ejecutivo gestiona a una persona fuera de su ruta. `asignacion_diaria_id` debe ser NULL. La persona solo debe existir en el sistema. No hay restricción de cartera ni de asignación. El ejecutivo debe tener rol `EJECUTIVO_TERRENO` activo.

La coherencia entre `origen_gestion` y `asignacion_diaria_id` se refuerza en la base de datos mediante un CHECK constraint.

## Consecuencias

- `gestiones.origen_gestion VARCHAR(30) NOT NULL`
- `gestiones.asignacion_diaria_id UUID NULL` (FK nullable a `asignaciones_diarias`)
- Las validaciones de negocio difieren según el origen
- El módulo `gestiones` depende de `asignaciones::api` solo para validar la diaria en ASIGNACION_DIARIA
- BUSQUEDA_DIRECTA amplía el alcance operativo del ejecutivo sin restricción de cartera
