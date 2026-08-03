# ADR-0040 — Política de logout con gestiones no resueltas (Fase 4C-A)

**Estado:** Aceptado — 2026-08-02
**Contexto:** Fase 4C-A — Gestiones offline desde ASIGNACION_DIARIA

---

## Contexto

Cuando el ejecutivo solicita cerrar sesión, puede haber gestiones en `gestion_local` que aún no han sido sincronizadas con el servidor. El logout llama a `BundleReplacementTransaction.limpiarTodo()`, que borra toda la tabla `gestion_local`. Si el ejecutivo cierra sesión con gestiones pendientes, esas gestiones se perderían permanentemente.

ADR-0024 (Política de sesión) y RN-24 establecen que el logout debe advertir sobre pendientes y **no borrar silenciosamente**. Esta decisión concreta cómo se implementa esa política en Android.

## Decisión

### Flujo de logout en HomeViewModel

1. El ejecutivo pulsa "Cerrar sesión".
2. `HomeViewModel.solicitarLogout()` llama a `gestionLocalDao.contarNoResueltas()`.
   - Si 0 → ejecuta logout inmediatamente.
   - Si > 0 → transiciona a `EstadoLogout.GestionesPendientes(cantidad)`.
3. La UI muestra un diálogo con dos opciones:
   - **Sincronizar y cerrar:** llama a `sincronizarYLogout()`.
   - **Cancelar:** vuelve a `EstadoLogout.Inactivo`. **No existe opción "salir igualmente".**
4. `sincronizarYLogout()` llama a `procesarOutbox()`, luego vuelve a contar:
   - Si 0 → ejecuta logout.
   - Si > 0 (no sincronizó todo) → transiciona a `EstadoLogout.ErrorSincronizacion(pendientes)`.
5. Desde `ErrorSincronizacion`, el ejecutivo puede reintentar o cancelar.

### No existe "salir igualmente"

No se ofrece al ejecutivo la opción de cerrar sesión sabiendo que hay gestiones no sincronizadas. Las opciones son siempre: sincronizar y cerrar, o cancelar. Esto es una decisión de negocio: las gestiones de cobranza tienen valor jurídico y no se pueden perder silenciosamente.

### Estados que bloquean el logout

Cualquier estado que no sea `SINCRONIZADA` bloquea el logout:
- `PENDIENTE_ENVIO`
- `ENVIANDO`
- `ERROR_REINTENTABLE`
- `ERROR_PERMANENTE`
- `CONFLICTO`

`contarNoResueltas()` usa `WHERE estadoSincronizacion != 'SINCRONIZADA'`, cubriendo todos estos estados.

### Limpieza en logout exitoso

Cuando el logout procede (0 no resueltas), `LogoutUseCase` ejecuta:
1. `gestionScheduler.cancelarTodo()` — cancela workers de envío.
2. `asignacionScheduler.cancelarTodo()` — cancela workers de descarga.
3. `bundleTransaction.limpiarTodo()` — borra Room (incluye `gestion_local`).
4. `sessionRepository.logout()` — invalida tokens en el servidor (best-effort con `runCatching`).

## Consecuencias

- Las gestiones de cobranza nunca se pierden por un cierre de sesión accidental.
- El ejecutivo puede reintentar la sincronización desde el diálogo de logout.
- Gestiones en `ERROR_PERMANENTE` o `CONFLICTO` (que no sincronizarán solos) seguirán bloqueando el logout. El ejecutivo debe contactar soporte o esperar que el supervisor las resuelva desde el Admin Web (funcionalidad futura).
- La política es consistente con ADR-0024 y RN-24.

## Alternativas descartadas

- **"Salir igualmente" con advertencia:** descartado. Las gestiones tienen valor jurídico; la advertencia no es suficiente salvaguarda.
- **Sincronización automática al detectar red antes del logout:** añade complejidad de estado sin eliminar la necesidad del diálogo.
- **Mantener gestiones en BD después del logout para el próximo ejecutivo:** descartado por seguridad; los datos de cobranza son confidenciales.
