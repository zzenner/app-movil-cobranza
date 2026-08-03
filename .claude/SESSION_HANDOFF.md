# Handoff de sesión — Fase 4C-A cerrada / Fase 4C-B lista

**Fecha:** 2026-08-03
**Rama activa:** `feature/fase-4c-b-busqueda-directa`

---

## Estado de Git

| Referencia | Hash | Descripción |
|---|---|---|
| `main` (local + origin) | `dec7b18` | feat(android): implementar gestiones offline fase 4c-a |
| `feature/fase-4c-gestiones-offline` | `dec7b18` | Fase 4C-A — cerrada, mergeada en main |
| `feature/fase-4c-b-busqueda-directa` | `dec7b18` | Base = main actualizado; rama vacía, sin cambios aún |
| `tag v0.11.0-gestiones-offline` | `dec7b18` | Fase 4C-A etiquetada |
| `tag v0.10.0-descarga-offline` | `f9382a2` | Fase 4B etiquetada |

**Árbol:** limpio. No hay cambios no confirmados.

---

## Cierre Fase 4C-A — resumen ejecutivo

| Suite | Resultado |
|---|---|
| API `./mvnw clean verify` | ✅ **248 tests — 0 failures — BUILD SUCCESS** |
| Android `assembleDebug` | ✅ BUILD SUCCESSFUL |
| Android `lint` | ✅ BUILD SUCCESSFUL — sin errores |
| Android `testDebugUnitTest` | ✅ **143 tests — 0 failures — 0 errors** |

Pasos de cierre completados:

1. ✅ Commit `feat(android): implementar gestiones offline fase 4c-a` (hash `dec7b18`)
2. ✅ `git push origin feature/fase-4c-gestiones-offline`
3. ✅ `git switch main && git pull --ff-only origin main`
4. ✅ `git merge --ff-only feature/fase-4c-gestiones-offline`
5. ✅ `git push origin main`
6. ✅ `git tag -a v0.11.0-gestiones-offline -m "Fase 4C-A: gestiones offline, GPS y outbox"`
7. ✅ `git push origin v0.11.0-gestiones-offline`
8. ✅ `git switch -c feature/fase-4c-b-busqueda-directa && git push -u origin feature/fase-4c-b-busqueda-directa`

---

## Siguiente acción exacta

**Objetivo:** Implementar Fase 4C-B — Búsqueda directa por RUT.

### Paso 1 — API

Agregar endpoint en módulo `personas`:

```
GET /api/v1/personas/buscar?rut=<numero>-<dv>
@PreAuthorize("hasRole('EJECUTIVO_TERRENO')")
→ 200: datos de persona con operaciones activas
→ 404: persona no encontrada
→ 400: RUT inválido
```

- Leer `apps/api/src/main/java/cl/zzenner/cobranza/personas/` para seguir la estructura existente.
- Test de integración: 200, 404, 400.

### Paso 2 — Android: NetworkModule

Agregar `PersonaBusquedaApi` en `:core:network`:

```kotlin
interface PersonaBusquedaApi {
    @GET("personas/buscar")
    suspend fun buscarPorRut(@Query("rut") rut: String): Response<PersonaBusquedaDto>
}
```

### Paso 3 — Android: UI búsqueda

Nueva pantalla (o bottom sheet desde Home) con:
- Campo de texto RUT con formato `XXXXXXXX-X`
- Llamada a API al confirmar
- Resultado: navegar a formulario de gestión con `origenGestion = BUSQUEDA_DIRECTA`

### Paso 4 — Tests

- `:feature:gestion` (o nuevo `:feature:busqueda`) con tests para el nuevo repositorio/ViewModel.
- Total objetivo: ≥ 155 tests Android JVM (143 existentes + ≥12 nuevos).

---

## No repetir

- Toda la Fase 4C-A (completa y cerrada)
- Room migración v1→v2 (completa — no crear nueva migración a menos que se agregue columna)
- Lease/CAS/backoff (completo — no modificar)
- GPS (completo — no modificar)
- Logout (completo — no modificar)
- ADR-0037..0040 (creados)
- BUSQUEDA_DIRECTA ya es un origen válido en `GestionValidator` y en `gestion_local` (solo falta la UI y el endpoint API)

---

## Contexto técnico relevante para Fase 4C-B

- `GestionLocalEntity.asignacionDiariaId: String?` — nullable, soporta BUSQUEDA_DIRECTA sin cambios en Room
- `GestionValidator.validarOrigen()`: BUSQUEDA_DIRECTA → asignacionDiariaId debe ser null ✅ ya implementado
- `GestionRepository.guardarEnOutbox()`: sin cambios necesarios
- `GestionApi.crearGestion()`: `asignacionDiariaId` es nullable en el DTO ✅ ya implementado
- Módulo API `personas`: ya existe en `apps/api/src/main/java/cl/zzenner/cobranza/personas/`
