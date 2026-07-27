# Ciclos de vida

Este documento describe los ciclos de vida de las entidades más relevantes del sistema, incluyendo sus estados, transiciones y reglas de retención de datos.

---

## Ciclo de vida de la asignación diaria

### Estados funcionales

```
BORRADOR ──► PUBLICADA ──► FINALIZADA
                 │
                 └──► CANCELADA  (opcional, requiere flujo administrativo)
```

| Estado       | Descripción                                                                               | Quién transiciona                      |
|--------------|-------------------------------------------------------------------------------------------|----------------------------------------|
| `BORRADOR`   | El supervisor está seleccionando personas. No visible para el ejecutivo.                  | Sistema al crear; supervisor al editar.|
| `PUBLICADA`  | Disponible para que el ejecutivo la descargue. La app Android la reconoce.                | Supervisor al publicar.                |
| `FINALIZADA` | Terminó su vigencia operacional. Solo lectura histórica.                                  | Sistema al finalizar el día o manual.  |
| `CANCELADA`  | Anulada antes de ser utilizada. **Opcional:** requiere flujo administrativo definido.      | Supervisor o administrador.            |

### Reglas de transición
- `BORRADOR` → `PUBLICADA` requiere al menos una persona en la asignación.
- `PUBLICADA` → `FINALIZADA` ocurre al terminar el día operacional o por acción manual.
- `PUBLICADA` → `CANCELADA` es opcional y requiere que se defina el flujo administrativo correspondiente antes de implementar.
- Una asignación `PUBLICADA` no debería modificarse. Si se requiere corrección, crear una nueva (política definitiva: PENDIENTE).
- Un ejecutivo puede tener como máximo una asignación `PUBLICADA` por día.

### La descarga es un evento técnico, no un estado funcional

`DESCARGADA` no es un estado funcional de la asignación. La descarga es un evento de sincronización que se registra por separado para trazabilidad.

**Concepto:** `descarga_asignacion_diaria`

| Atributo                | Descripción                                                         |
|-------------------------|---------------------------------------------------------------------|
| `asignacion_diaria_id`  | Asignación que se descargó.                                         |
| `dispositivo_id`        | Dispositivo que realizó la descarga.                                |
| `fecha_primera_descarga`| Cuándo se descargó por primera vez.                                 |
| `fecha_ultima_descarga` | Cuándo se descargó por última vez (puede descargarse más de una vez).|
| `version_descargada`    | Versión de la app al momento de la descarga.                        |

Una asignación puede descargarse más de una vez (nueva sincronización, reinstalación de la app, etc.).

---

## Ciclo de vida de una gestión

Las gestiones **no tienen ciclo de vida de negocio**: son hechos inmutables desde el momento de su registro. El único estado que evoluciona es el **estado técnico de sincronización**.

```
[Registrada en dispositivo]
        │
        ▼
  PENDIENTE_ENVIO
        │
        ├──► ENVIANDO ──► SINCRONIZADA  ✓ (terminal exitoso)
        │
        ├──► ERROR_REINTENTABLE ──► (reintento) ──► ENVIANDO
        │                                   └──► ERROR_PERMANENTE  ✗ (terminal con error)
        │
        └──► ERROR_PERMANENTE  ✗ (en primer intento, si el error es definitivo)
```

| Estado               | Es terminal | Descripción                                                  |
|----------------------|-------------|--------------------------------------------------------------|
| `PENDIENTE_ENVIO`    | No          | Registrada localmente, esperando envío.                      |
| `ENVIANDO`           | No          | En proceso activo de envío.                                  |
| `SINCRONIZADA`       | Sí          | Confirmada por la API. La gestión existe en el servidor.     |
| `ERROR_REINTENTABLE` | No          | Fallo temporal. Se reintentará con backoff exponencial.      |
| `ERROR_PERMANENTE`   | Sí          | Fallo definitivo. Requiere intervención. No se reintenta.    |

**Regla clave:** la gestión existe en el dominio desde que se registra en el dispositivo. El estado técnico no representa su validez de negocio, solo si llegó al servidor.

---

## Ciclo de vida de la sesión local en Android

```
[Sin sesión]
     │
     ▼ (login exitoso con conexión)
[Sesión activa + access token + refresh token]
     │
     ├──► (pérdida de red) ──► [Sesión activa, modo offline]
     │                               │
     │                               └──► (recupera red) ──► [Validar/renovar token] ──► [Sesión activa]
     │
     ├──► (access token expira + hay red) ──► [Renovar con refresh token] ──► [Sesión activa]
     │
     ├──► (usuario desactivado o dispositivo revocado en servidor) ──► [Sesión terminada, logout forzado]
     │
     └──► (logout manual) ──► [Sin sesión]
```

### Reglas
- La **sesión local** (datos en Room, preferencias del usuario) persiste hasta logout explícito.
- El **access token** tiene duración limitada y se renueva con el refresh token.
- La pérdida de red **no termina la sesión local** ni borra datos descargados.
- Al recuperar red: validar o renovar token antes de sincronizar.
- Si el usuario fue desactivado o el dispositivo revocado, el servidor rechaza la renovación y la app termina la sesión.

### Logout con operaciones pendientes

Se considera **pendiente** cualquier elemento en estado distinto de sincronizado:
- Gestiones no enviadas.
- Fotografías no enviadas.
- Ubicaciones asociadas no enviadas.
- Operaciones del outbox pendientes.

**Reglas del MVP:**

```
[Logout solicitado]
       │
       ├── ¿Hay pendientes? ── No ──► Logout permitido. Eliminar sesión y credenciales.
       │
       └── Sí ──► ¿Hay conexión?
                       │
                       ├── Sí ──► Intentar sincronizar
                       │              │
                       │              ├── Éxito ──► Logout permitido.
                       │              └── Fallo ──► Mantener sesión abierta. Mostrar error.
                       │
                       └── No ──► No se permite logout. Mostrar mensaje explicativo.
```

- Cerrar la aplicación (background/kill) **no equivale** a cerrar sesión.
- No se permite que otro usuario inicie sesión mientras existan datos pendientes del usuario actual.
- No se eliminan silenciosamente gestiones ni fotografías pendientes bajo ninguna circunstancia.

---

## Retención de datos locales en Android

### Política de retención

| Dato                                | ¿Se puede eliminar al llegar nueva asignación? |
|-------------------------------------|------------------------------------------------|
| Personas de asignación anterior     | Sí, salvo que tengan operaciones pendientes    |
| Operaciones y cuotas anteriores     | Sí, salvo que estén en el outbox               |
| Gestiones `SINCRONIZADA`            | Sí (ya están en el servidor)                   |
| Gestiones `PENDIENTE_ENVIO`         | **No** — deben enviarse primero                |
| Gestiones `ERROR_REINTENTABLE`      | **No** — hay reintento pendiente               |
| Fotografías sincronizadas           | Sí                                             |
| Fotografías pendientes de envío     | **No** — deben enviarse primero                |
| Referencias mínimas de persona      | **No** si la persona tiene pendientes          |

### Regla general
> Una vez que todas las gestiones, fotografías y operaciones de outbox asociadas a una persona están en estado `SINCRONIZADA`, los datos de esa persona de asignaciones anteriores pueden eliminarse del almacenamiento local.

### Estado de implementación
La limpieza automática **no se implementa en el MVP**. Este documento define el comportamiento esperado para implementación futura.
