# Decisiones funcionales pendientes

Este documento consolida las decisiones que aún no se han confirmado y bloquean o condicionan decisiones de implementación. Es la fuente de verdad de lo que está abierto; las preguntas resueltas se mueven a `REGLAS_NEGOCIO.md` o al ADR correspondiente.

**Última actualización:** 2026-07-26 (auditoría Fase 1A)

---

## Pendientes que condicionan la implementación

### DP-01 — ¿El ejecutivo puede registrar gestiones sobre personas fuera de su asignación diaria activa?

**Impacto:** Afecta la lógica de validación del registro de gestiones en Android y en la API. Si la respuesta es sí, la app debe permitir buscar personas globalmente y registrar gestiones sobre ellas, no solo sobre las de la asignación diaria.

**Bloqueante para:** Fase 3 (app Android, módulo de gestiones).

---

### DP-02 — ¿Los ejecutivos ven gestiones de otros ejecutivos sobre la misma persona en la app Android?

**Descripción:** Actualmente se definen "las últimas 10 gestiones por RUT" descargadas al teléfono. No está confirmado si esas gestiones incluyen las de otros ejecutivos o solo las del ejecutivo autenticado.

**Impacto:** Afecta el volumen de datos descargados, el diseño de la consulta de sincronización y la pantalla de historial de gestiones en la app.

**Bloqueante para:** Fase 2 (endpoint de sincronización) y Fase 3 (app Android).

---

### DP-03 — ¿Cuál es el catálogo completo de tipos de gestión?

**Descripción:** Los tres tipos iniciales están confirmados: `CONTACTO_FAMILIAR`, `COMPROMISO_PAGO`, `SIN_CONTACTO`. El equipo de negocio puede querer añadir más tipos antes del lanzamiento.

**Impacto:** Afecta el catálogo en la base de datos y la UI del registro de gestiones en Android.

**Bloqueante para:** Inicio de la Fase 3. No bloquea la Fase 1B.

---

### DP-04 — ¿La exportación a Excel forma parte de la Fase 1 o de una posterior?

**Impacto:** Si se incluye en Fase 1, debe añadirse al alcance de la administración web. Si no, se documenta como deuda técnica planificada.

**Bloqueante para:** Planificación de Fase 1 (admin web).

---

### DP-05 — ¿Puede el supervisor modificar una asignación `PUBLICADA` o debe crear una nueva?

**Descripción:** Actualmente se recomienda crear una nueva asignación si se requiere corrección. No se ha confirmado si se permitirá modificar una asignación ya publicada (con personas descargadas en el teléfono del ejecutivo).

**Impacto:** Si se permite modificar, el dispositivo debe poder detectar cambios en una asignación que ya descargó. Esto complica el protocolo de sincronización.

**Bloqueante para:** Fase 2 (módulo de asignaciones en la API) y Fase 3 (sincronización).

---

### DP-06 — ¿Se implementa el estado `CANCELADA` en el MVP?

**Descripción:** `CANCELADA` está documentado como opcional. Implementarlo requiere un flujo administrativo definido (¿quién puede cancelar?, ¿con qué restricciones?, ¿qué ocurre si el ejecutivo ya descargó la asignación?).

**Impacto:** Afecta el ciclo de vida de asignaciones en la API y en la app Android.

**Bloqueante para:** Fase 2 (módulo de asignaciones). No bloquea la Fase 1B.

---

### DP-07 — Confirmar `minSdk` definitivo con inventario de dispositivos

**Descripción:** El valor provisional es API 29 (Android 10). Para confirmar, se necesita el inventario de modelos corporativos con su versión de Android, RAM y almacenamiento disponible.

**Impacto:** Determina qué APIs Android están disponibles y si algún dispositivo corporativo queda excluido.

**Bloqueante para:** Creación del proyecto Android (Fase 3). No bloquea la Fase 1B.

**Referencia:** ADR-0011.

---

### DP-08 — ¿El aval se asocia a la persona o a una operación en el sistema externo?

**Descripción:** En el MVP, el aval está asociado a la persona. Si el sistema externo los asocia a operaciones específicas, el modelo de datos deberá ajustarse al importar los datos.

**Impacto:** Afecta el esquema de importación y el modelo de `avales`.

**Bloqueante para:** Diseño del módulo de importación (Fase 1B o Fase 2). No urgente.

---

## Pendientes técnicos (no funcionales)

### DT-002 — Cifrado de base de datos local Android
Ver `DEUDA_TECNICA.md`.

### DT-004 — Almacenamiento de fotografías (diseño)
Ver `DEUDA_TECNICA.md`.

### DT-005 — Política de backoff y número máximo de reintentos
Ver `DEUDA_TECNICA.md`.

### DT-006 — Integración con sistema externo por API
Ver `DEUDA_TECNICA.md`.

### DT-007 — Docker Desktop no integrado con WSL2
Ver `DEUDA_TECNICA.md`.

---

## Historial de decisiones resueltas

| ID anterior | Descripción | Dónde se resolvió |
|-------------|-------------|-------------------|
| P-01 (sesión 3) | Política de logout con operaciones pendientes | RN-24, CICLOS_DE_VIDA.md |
| P-05 (sesión 2) | Supervisor crea/publica asignación diaria | RN-21 |
| P-06/P-07 (sesión 2) | Sesión local persistente; no PIN/biometría | RN-24, ADR-0011 |
| P-07 (sesión 3) | Alcance de operaciones en la descarga | RN-10, RF-04c |
