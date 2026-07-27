# Resolución de conflictos

## Contexto

En un sistema offline-first, un conflicto ocurre cuando los datos locales del dispositivo y los datos del servidor divergen de forma que no es posible reconciliarlos automáticamente sin riesgo de pérdida de información.

## Tipos de conflicto identificados

### Conflicto de gestión (UUID duplicado)
Una gestión enviada desde el dispositivo tiene el mismo UUID que una ya existente en el servidor, pero con contenido diferente.

**Este caso no debería ocurrir en operación normal**, dado que:
- El UUID se genera en el dispositivo en el momento del registro.
- Las gestiones son inmutables desde su creación.
- La API acepta el mismo UUID múltiples veces como idempotencia normal.

Si ocurriera contenido diferente con el mismo UUID, la API rechaza el reenvío con `409 Conflict`. El estado en Room se marca `ERROR_PERMANENTE` y se notifica al ejecutivo. No se intenta resolver automáticamente.

---

### Conflicto de asignación (nueva asignación con operaciones pendientes)
Llega una nueva asignación diaria mientras el dispositivo aún tiene gestiones de la asignación anterior en estado `PENDIENTE_ENVIO` o `ERROR_REINTENTABLE`.

**Estrategia:**
- Las gestiones pendientes **no se eliminan**. Siguen en la cola del outbox.
- Los datos mínimos de las personas relacionadas se conservan en Room hasta que todas sus gestiones estén `SINCRONIZADA`.
- Se descarga la nueva asignación en paralelo.
- La interfaz distingue visualmente las gestiones pendientes de la asignación anterior.

---

### Conflicto de datos financieros
Los valores financieros (interés penal, gastos de cobranza) en el dispositivo difieren de los del servidor.

**Estrategia:** la API es autoridad. Los valores del servidor **siempre reemplazan** los locales al sincronizar.

---

### Conflicto de sesión (usuario o dispositivo revocado)
El usuario fue desactivado o el dispositivo fue revocado administrativamente mientras el teléfono operaba offline.

**Estrategia:**
- Al recuperar conectividad, el servidor rechaza la renovación del access token.
- La app termina la sesión local y solicita nuevo login.
- Las gestiones pendientes **no se borran**: se conservan en Room.
- El ejecutivo puede iniciar sesión con otro dispositivo autorizado y sincronizar las gestiones desde allí, o el administrador puede restaurar el acceso.

**PENDIENTE:** definir el flujo exacto de recuperación de gestiones cuando un dispositivo queda revocado con operaciones pendientes.

---

### Conflicto de logout con operaciones pendientes
El ejecutivo intenta hacer logout mientras hay gestiones con estado `PENDIENTE_ENVIO` u operaciones del outbox pendientes.

**Estrategia (política MVP confirmada):**
- **Sin pendientes:** logout permitido. Se eliminan sesión y credenciales locales.
- **Con pendientes y conexión disponible:** la app intenta sincronizar antes de hacer logout.
  - Si la sincronización termina correctamente: logout permitido.
  - Si la sincronización falla: la sesión se mantiene abierta; se muestra error al ejecutivo.
- **Con pendientes y sin conexión:** no se permite logout. Se muestra mensaje explicativo.
- En ningún caso se eliminan silenciosamente gestiones, fotografías ni operaciones pendientes.
- Cerrar la aplicación (background/kill) no equivale a logout.
- No se permite que otro usuario inicie sesión mientras existan datos pendientes del usuario actual.

Ver `docs/dominio/CICLOS_DE_VIDA.md` para el diagrama de flujo completo.

---

## Reglas generales de resolución

1. **La API es autoridad** en datos financieros (cuotas, operaciones, personas).
2. **El dispositivo conserva** todas las gestiones registradas localmente, incluso si hay conflicto de asignación.
3. **No se elimina ninguna gestión o fotografía pendiente** sin sincronización exitosa previa.
4. **Los conflictos se notifican al ejecutivo** de forma comprensible, no como errores técnicos.
5. **Gestiones pendientes de asignaciones anteriores** coexisten con la nueva asignación hasta sincronizarse.

## Referencia

- [`ESTRATEGIA_OFFLINE.md`](ESTRATEGIA_OFFLINE.md) — estados de sincronización y retención de datos.
- [`PROTOCOLO_SINCRONIZACION.md`](PROTOCOLO_SINCRONIZACION.md) — endpoints y flujo técnico.
- [`docs/dominio/CICLOS_DE_VIDA.md`](../dominio/CICLOS_DE_VIDA.md) — ciclos de vida de asignación, gestión y sesión.
