# Requisitos funcionales

## RF-01 Autenticación y sesión

| ID     | Descripción                                                                                         |
|--------|-----------------------------------------------------------------------------------------------------|
| RF-01a | El sistema autentica usuarios con credenciales (usuario y contraseña). El primer login requiere conexión. |
| RF-01b | Después de un login exitoso, la sesión local en Android persiste mientras el usuario no ejecute logout. La pérdida de red no cierra la sesión. |
| RF-01c | La app puede operar offline con los datos descargados mientras la sesión local esté activa.         |
| RF-01d | No se usa PIN local ni biometría en el MVP. Los teléfonos son corporativos.                         |
| RF-01e | Al recuperar conectividad, la app valida o renueva el access token antes de sincronizar.            |
| RF-01f | Si el usuario fue desactivado o el dispositivo revocado, el servidor rechaza la renovación y la app termina la sesión. |
| RF-01g | **Política de logout MVP (implementada en Fase 4C-A):** si `contarNoResueltas()=0` → logout inmediato; si hay gestiones en cualquier estado no-SINCRONIZADA (PENDIENTE_ENVIO, ENVIANDO, ERROR_REINTENTABLE, ERROR_PERMANENTE, CONFLICTO) → se ofrece "Sincronizar y cerrar"; si sincroniza y `contarNoResueltas()=0` → logout; si queda alguna no resuelta → logout bloqueado con opción de reintentar. **No existe opción "salir igualmente"**. No se eliminan silenciosamente gestiones ni fotografías pendientes. Cerrar la app no equivale a logout. Ver ADR-0040. |
| RF-01h | El administrador web se autentica con las mismas credenciales institucionales.                      |

## RF-02 Gestión de usuarios

| ID     | Descripción                                                                                         |
|--------|-----------------------------------------------------------------------------------------------------|
| RF-02a | El sistema soporta cuatro roles: `JEFE_SUPERVISORES`, `TECNOLOGIA`, `SUPERVISOR`, `EJECUTIVO_TERRENO`. |
| RF-02b | Un usuario con rol `TECNOLOGIA` o `JEFE_SUPERVISORES` puede crear, modificar y desactivar usuarios. |
| RF-02c | Un usuario desactivado no puede iniciar sesión.                                                     |
| RF-02d | Se gestiona la relación supervisor-ejecutivo con historial de cambios.                              |

## RF-03 Carteras y asignaciones

| ID     | Descripción                                                                                         |
|--------|-----------------------------------------------------------------------------------------------------|
| RF-03a | El sistema permite crear y gestionar carteras de cobranza.                                          |
| RF-03b | La asignación mensual (~300–400 personas por ejecutivo) se carga mediante CSV; futuro: API externa. |
| RF-03c | El **supervisor** crea y publica la asignación diaria (~50 personas) para cada ejecutivo de su equipo. |
| RF-03d | El ejecutivo no selecciona por sí mismo las personas del día.                                       |
| RF-03e | La app Android descarga exclusivamente asignaciones diarias en estado `PUBLICADA` del ejecutivo autenticado. |
| RF-03f | Se conserva historial completo de asignaciones mensuales y diarias.                                 |
| RF-03g | Una persona no puede estar asignada a dos ejecutivos simultáneamente.                               |

## RF-04 Consulta de información

| ID     | Descripción                                                                                         |
|--------|-----------------------------------------------------------------------------------------------------|
| RF-04a | El ejecutivo puede buscar personas en su asignación diaria local por RUT (sin conexión).            |
| RF-04b | **El ejecutivo puede buscar personas globalmente por RUT (Fase 4C-B ✅):** `POST /api/v1/personas/busquedas` con RUT en body por privacidad. La respuesta se persiste como snapshot en Room (`persona_directa`), permitiendo registrar la gestión sin conexión adicional. Ver ADR-0041, ADR-0042. |
| RF-04c | El ejecutivo puede ver las operaciones activas de una persona con sus cuotas: todas las vencidas vigentes y todas las futuras vigentes. No se descargan operaciones anuladas, cerradas sin saldo ni completamente pagadas. |
| RF-04d | El ejecutivo puede ver los avales de una persona (solo `rut_numero`, `rut_dv`, `nombre`).           |
| RF-04e | El ejecutivo puede ver las últimas 10 gestiones de cada RUT (pueden ser de otros ejecutivos).       |
| RF-04f | Los usuarios de la web administrativa pueden consultar personas y sus datos desde el servidor.      |

## RF-05 Registro de gestiones

| ID     | Descripción                                                                                         |
|--------|-----------------------------------------------------------------------------------------------------|
| RF-05a | El ejecutivo puede registrar una gestión desde la app Android, con o sin conexión.                  |
| RF-05b | Tipos de gestión: `CONTACTO_FAMILIAR`, `COMPROMISO_PAGO`, `SIN_CONTACTO`.                          |
| RF-05c | `COMPROMISO_PAGO` requiere una fecha de compromiso. No incluye monto.                               |
| RF-05d | La geolocalización puntual es **obligatoria**. Sin GPS o sin coordenadas disponibles, no se puede guardar. |
| RF-05e | Las fotografías son opcionales. Se permiten varias por gestión.                                     |
| RF-05f | Las observaciones son texto libre opcional.                                                         |
| RF-05g | Una gestión es inmutable desde su creación. No se puede modificar ni anular por ningún usuario.    |
| RF-05h | El ejecutivo puede registrar una observación de dirección cuando detecta que la dirección de una persona es incorrecta. Esta observación **no** modifica la dirección original del sistema. |
| RF-05i | **Dos orígenes de gestión (ADR-0026):** `ASIGNACION_DIARIA` — solo personas de la diaria activa (PUBLICADA o FINALIZADA); `BUSQUEDA_DIRECTA` — cualquier persona del sistema, sin restricción de cartera. |
| RF-05j | La API acepta gestiones con idempotencia atómica por UUID del dispositivo: mismo UUID + mismo contenido → éxito; mismo UUID + contenido distinto → HTTP 409. Ver ADR-0027. |

## RF-06 Sincronización

| ID     | Descripción                                                                                         |
|--------|-----------------------------------------------------------------------------------------------------|
| RF-06a | La app sincroniza automáticamente al recuperar conectividad.                                        |
| RF-06b | El ejecutivo puede iniciar sincronización manual: "Sincronizar asignación" y "Enviar gestiones pendientes". |
| RF-06c | Las gestiones no sincronizadas quedan en la cola local (outbox) hasta poder enviarse.               |
| RF-06d | La API acepta gestiones con idempotencia por UUID del dispositivo.                                  |
| RF-06e | La app muestra permanentemente: modo offline, última sincronización, gestiones pendientes de envío, errores. |
| RF-06f | Al sincronizar, los datos financieros del servidor reemplazan los locales. Las gestiones pendientes y fotografías no se reemplazan. |
| RF-06g | Los datos de personas de asignaciones anteriores pueden eliminarse del dispositivo solo cuando todas sus gestiones estén sincronizadas. |

## RF-07 Visualización y administración web

| ID     | Descripción                                                                                         |
|--------|-----------------------------------------------------------------------------------------------------|
| RF-07f | **El administrador puede consultar el listado y detalle de usuarios del sistema (Fase 5B-1 ✅):** `GET /api/v1/admin/usuarios` (listado paginado con filtros: nombreUsuario, estado, rol) y `GET /api/v1/admin/usuarios/{id}` (detalle con roles, permisos efectivos, supervisor y estado calculado). Permiso requerido: `USUARIOS_VER`. Solo JEFE_SUPERVISORES y TECNOLOGIA tienen acceso. Solo lectura — sin escritura. Ver ADR-0046. |
| RF-07a | El supervisor y roles superiores pueden ver las gestiones registradas por sus ejecutivos.           |
| RF-07b | El supervisor puede ver el estado de dispositivos de sus ejecutivos (última sync, versión, errores).|
| RF-07c | El administrador puede revocar un dispositivo; la revocación es efectiva al recuperar conectividad. |
| RF-07d | La aplicación administrativa permite importar asignaciones mensuales desde CSV.                     |
| RF-07e | La aplicación administrativa muestra el resultado de cada importación (filas aceptadas, rechazadas, advertencias). |

## PENDIENTE

- Definir si la exportación a Excel forma parte de la Fase 1.
- Confirmar versión mínima de Android definitiva según inventario de dispositivos corporativos (provisional: API 29 / Android 10).
- ~~Confirmar si el ejecutivo puede registrar gestiones sobre personas fuera de su asignación diaria activa.~~ **Resuelto:** dos orígenes (ADR-0026, Fase 4C-B ✅).
- Confirmar si los ejecutivos ven gestiones de otros ejecutivos sobre la misma persona.
