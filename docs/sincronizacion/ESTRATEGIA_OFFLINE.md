# Estrategia offline

## Principio fundamental

La app Android es **offline-first**: la interfaz de usuario siempre lee desde Room (SQLite local), independientemente de si hay conexión. La API es la autoridad de datos financieros y asignaciones; Room es la proyección local de esos datos para el ejecutivo.

## Roles de cada componente

| Componente  | Rol                                                                        |
|-------------|----------------------------------------------------------------------------|
| Room        | Fuente de verdad local para la interfaz Android. Base de trabajo del ejecutivo. |
| API         | Autoridad de datos financieros, personas y asignaciones. Destino final de las gestiones. |
| WorkManager | Orquesta la sincronización en segundo plano con garantías de entrega.      |

## Unidad de descarga: asignación diaria

La app descarga exclusivamente la **asignación diaria vigente** y sus datos relacionados:
- ~50 personas.
- Operaciones de cada persona.
- ~3 cuotas por operación (vencidas y futuras).
- Últimas 10 gestiones de cada RUT (pueden ser de otros ejecutivos).
- Direcciones asociadas.
- Valores financieros vigentes.

## Datos que se almacenan localmente

- Asignación diaria activa: personas, **todas las operaciones activas**, **todas las cuotas vencidas y futuras vigentes**, gestiones históricas (últimas 10 por RUT), direcciones, avales.
- Gestiones registradas localmente (pendientes de sincronizar y ya sincronizadas).
- Tokens de autenticación (en almacenamiento seguro del SO, no en Room).
- Estado técnico de sincronización de cada gestión.
- No se descargan: operaciones anuladas, cerradas sin saldo, o completamente pagadas.

## Flujo de registro de gestión (sin conexión)

1. El ejecutivo registra una gestión en la app.
2. La geolocalización se captura en el momento; sin coordenadas no se permite guardar.
3. La gestión se guarda en Room con un UUID generado en el dispositivo y estado `PENDIENTE_ENVIO`.
4. La gestión se agrega a la cola outbox local.
5. WorkManager detecta conectividad y ejecuta la sincronización.
6. La gestión se envía a la API. La API la persiste de forma idempotente (clave: UUID).
7. Al recibir confirmación, Room actualiza el estado a `SINCRONIZADA`.
8. En caso de error reintentable, el estado queda en `ERROR_REINTENTABLE` y se programa reintento con backoff exponencial.

## Flujo de descarga de asignación

1. WorkManager programa sincronización al detectar conectividad, o el ejecutivo la inicia manualmente.
2. La API provee la asignación diaria vigente y los datos relacionados.
3. Room se actualiza con los nuevos datos financieros (reemplazan los locales).
4. **No se reemplazan:** gestiones creadas offline no enviadas, fotografías pendientes, operaciones pendientes de sincronización.
5. La interfaz refleja automáticamente los cambios desde Room (Flow/LiveData).

## Estados técnicos de sincronización de una gestión

Estos estados pertenecen al mecanismo técnico de sincronización, no al ciclo de negocio de la gestión:

| Estado               | Descripción                                                                 |
|----------------------|-----------------------------------------------------------------------------|
| `PENDIENTE_ENVIO`    | Registrada localmente, esperando envío.                                     |
| `ENVIANDO`           | En proceso de envío a la API.                                               |
| `SINCRONIZADA`       | Confirmada por la API.                                                      |
| `ERROR_REINTENTABLE` | Fallo temporal (red, timeout). Se reintentará con backoff exponencial.      |
| `ERROR_PERMANENTE`   | Fallo definitivo (error de validación, 4xx no recuperable). Requiere intervención. |

Las gestiones offline **nunca son reemplazadas** por datos descargados desde el servidor.

## Retención de datos locales

El dispositivo conserva principalmente la **asignación diaria vigente**. Cuando llegue una nueva asignación:

- Los datos de personas de asignaciones anteriores **pueden eliminarse** del almacenamiento local.
- **No se pueden eliminar** mientras existan:
  - Gestiones con estado `PENDIENTE_ENVIO` o `ERROR_REINTENTABLE`.
  - Fotografías pendientes de envío.
  - Operaciones del outbox.
  - Referencias mínimas de la persona relacionada.
- Una vez sincronizadas todas las operaciones pendientes, los datos de la asignación anterior pueden eliminarse.

La limpieza automática **no se implementa en el MVP**. Ver `docs/dominio/CICLOS_DE_VIDA.md` para el modelo completo de retención.

## Indicadores en la interfaz

La app debe mostrar permanentemente:
- **Modo offline:** indicador visible cuando no hay conexión.
- **Última sincronización:** fecha y hora de la última sincronización exitosa.
- **Gestiones pendientes de envío:** contador de gestiones con estado `PENDIENTE_ENVIO`.
- **Errores de sincronización:** notificación cuando existe algún `ERROR_PERMANENTE`.

## Sincronización manual

Además de la sincronización automática al recuperar conectividad, el ejecutivo puede iniciar manualmente:
- **"Sincronizar asignación"** — descarga la asignación diaria vigente del servidor.
- **"Enviar gestiones pendientes"** — envía gestiones con estado `PENDIENTE_ENVIO` cuando existan datos locales no sincronizados.

## Sesión local y tokens

Tres conceptos separados que evolucionan de forma independiente:

| Concepto            | Descripción                                                                    |
|---------------------|--------------------------------------------------------------------------------|
| Sesión local Android| Persiste mientras no haya logout. La pérdida de red no la cierra.             |
| Access token        | Para consumir la API. Duración limitada. Se renueva con el refresh token.     |
| Refresh token       | Para renovar el access token. Se valida al recuperar conectividad.            |

**Reglas:**
- El primer login siempre requiere conexión y credenciales (usuario + contraseña).
- La app puede operar offline con los datos descargados mientras la sesión local esté activa.
- No se usa PIN local ni biometría en el MVP. Los teléfonos son corporativos.
- Al recuperar conectividad, la app verifica o renueva el access token, confirma que el usuario esté activo y que el dispositivo no esté revocado, luego sincroniza.
- La revocación administrativa de usuario o dispositivo es efectiva al recuperar conectividad.

**Logout — política MVP:**

Se consideran pendientes: gestiones no enviadas, fotografías no enviadas, ubicaciones asociadas no enviadas, y operaciones del outbox en estado distinto de sincronizado.

- Sin pendientes: logout permitido.
- Con pendientes y conexión: la app intenta sincronizar. Si termina con éxito, permite logout; si falla, mantiene la sesión abierta.
- Con pendientes y sin conexión: no se permite logout.
- Cerrar la app (background/kill) no equivale a logout.
- No se puede iniciar sesión con otro usuario mientras haya datos pendientes del usuario actual.
- No se eliminan silenciosamente gestiones ni fotografías pendientes.

## UUID en el dispositivo

- Cada gestión recibe un UUID v4 generado en el momento del registro, en el dispositivo.
- Este UUID es el identificador de la gestión en la API y garantiza idempotencia en reenvíos.

## Política de reintentos

- Los reintentos usan backoff exponencial con jitter.
- WorkManager usa constraints de red conectada.
- El número máximo de reintentos antes de marcar `ERROR_PERMANENTE`: **PENDIENTE de definir**.

## PENDIENTE

- Definir número máximo de reintentos antes de `ERROR_PERMANENTE`.
- Definir política exacta de backoff (intervalo inicial, factor multiplicador).
- Definir qué datos de la asignación diaria se sincronizan de forma incremental (delta) vs. reemplazo completo.
- Definir el mecanismo de autenticación offline (cómo reabrir la app sin conexión).
- Evaluar cifrado de la base de datos local Room.
