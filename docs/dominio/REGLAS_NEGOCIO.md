# Reglas de negocio

Este documento registra las reglas de negocio confirmadas y las preguntas aún pendientes.

## Reglas confirmadas

### RN-01 Identificación de personas
- Las personas se identifican por RUT chileno.
- El RUT se almacena separado en dos columnas: `rut_numero` (parte numérica) y `rut_dv` (dígito verificador).
- El RUT es único en el sistema.

### RN-02 Persona como unidad principal
- La unidad principal de gestión es la **persona**, no el crédito ni ninguna otra entidad.
- La estructura de datos sigue la jerarquía: `cartera → persona → operaciones → cuotas`.
- No se usa `caso_cobranza` como unidad principal.

### RN-03 Pertenencia a cartera
- Una persona puede pertenecer simultáneamente a una o más carteras activas.
- Para un mismo par persona–cartera puede existir como máximo un vínculo activo.
- El historial de vínculos persona–cartera se conserva sin borrado físico.

### RN-04 Ejecutivo responsable
- Una persona puede tener solo un ejecutivo de terreno responsable activo a la vez.
- Se conserva historial completo de asignaciones.

### RN-05 Avales
- Una persona puede tener uno o más avales.
- El aval es información de solo lectura: `rut_numero`, `rut_dv`, `nombre`.
- Se muestra en el detalle de la persona en la app Android.
- No recibe asignaciones, gestiones, compromisos ni rutas.
- No requiere dirección ni teléfonos en el MVP.
- Proviene de la carga CSV y del sistema externo.

### RN-06 Roles del sistema
Los roles del sistema son:
- `JEFE_SUPERVISORES`
- `TECNOLOGIA`
- `SUPERVISOR`
- `EJECUTIVO_TERRENO`

### RN-07 Supervisión
- Un supervisor puede tener varios ejecutivos de terreno bajo su supervisión.
- Se conserva historial completo de cambios de supervisión.

### RN-08 Asignación mensual
- La asignación mensual carga aproximadamente entre 300 y 400 personas por ejecutivo.
- Registro de fecha de inicio y término.
- Se conserva historial completo de asignaciones mensuales.
- Formato inicial de carga: CSV (futuro: API del sistema externo).

### RN-09 Asignación diaria
- La asignación diaria es un subconjunto de la asignación mensual, de aproximadamente 50 personas.
- Es la base de la ruta de trabajo diario del ejecutivo.
- La crea y publica el **supervisor**, no el ejecutivo.
- El ejecutivo no selecciona por sí mismo las personas del día.
- Se registra el supervisor que la creó y la fecha de publicación.
- La app móvil descarga solo asignaciones en estado `PUBLICADA` para el ejecutivo autenticado.
- Se conserva historial completo de asignaciones diarias.
- Los estados del ciclo de vida son preliminares: ver `CICLOS_DE_VIDA.md`.

### RN-10 Datos descargados al teléfono
Por cada asignación diaria, el teléfono recibe por persona:
- Datos de las ~50 personas asignadas para el día.
- **Todas las operaciones activas** de cada persona.
- **Todas las cuotas vencidas vigentes** de esas operaciones.
- **Todas las cuotas futuras vigentes** de esas operaciones.
- Los valores financieros actuales de cada cuota (interés penal, gastos de cobranza, total vigente).
- Las últimas 10 gestiones de cada RUT (pueden ser de otros ejecutivos).
- Direcciones asociadas.
- Avales (rut_numero, rut_dv, nombre).

**No se descargan:**
- Operaciones anuladas.
- Operaciones cerradas sin saldo.
- Operaciones completamente pagadas (salvo que se defina una necesidad funcional posterior).
- Historial completo de snapshots financieros.

Los valores financieros del servidor reemplazan los valores locales en cada sincronización. Las gestiones offline y sus archivos pendientes nunca se reemplazan.

### RN-11 Tipos de gestión
Los tipos de gestión confirmados son:
- `CONTACTO_FAMILIAR` — contacto con familiar de la persona titular.
- `COMPROMISO_PAGO` — acuerdo verbal de pago.
- `SIN_CONTACTO` — visita sin resultado de contacto.

### RN-12 Orígenes de gestión
Toda gestión tiene un origen que determina el contexto de la visita (ADR-0026):
- **`ASIGNACION_DIARIA`**: el ejecutivo gestiona una persona de su asignación diaria activa. La asignación debe estar en estado `PUBLICADA` o `FINALIZADA` (gestión offline sincronizada). Requiere que la persona pertenezca a esa asignación diaria y que el ejecutivo sea su destinatario.
- **`BUSQUEDA_DIRECTA`**: el ejecutivo gestiona cualquier persona conocida en el sistema, sin restricción de cartera ni asignación activa. Disponible para cualquier ejecutivo con rol `EJECUTIVO_TERRENO`.

### RN-12b Reglas del registro de gestiones
- Toda gestión se registra sobre una **persona** (no sobre una operación específica).
- `COMPROMISO_PAGO` exige una fecha de compromiso. El compromiso **no** incluye monto.
- Las fotografías son opcionales; se permiten varias por gestión (implementación diferida, ADR-0030).
- La geolocalización puntual es **obligatoria** para registrar una gestión.
- No se registran pagos.
- No se solicita firma.

### RN-13 Inmutabilidad de gestiones
- Una gestión queda registrada inmediatamente en el teléfono, incluso sin conexión.
- Una gestión sincronizada **no puede modificarse**.
- Una gestión **no puede anularse**.
- Ningún usuario, incluido el administrador, puede corregirla.
- No se modelan rectificaciones ni anulaciones en el MVP.

### RN-14 Sincronización de gestiones — estados técnicos
Los estados técnicos de sincronización son distintos del ciclo de negocio de la gestión:
- `PENDIENTE_ENVIO` — registrada localmente, esperando envío.
- `ENVIANDO` — en proceso de envío (lease activo en el worker).
- `SINCRONIZADA` — confirmada por la API (201 o 200 idempotente).
- `ERROR_REINTENTABLE` — fallo temporal (5xx, IOException); se reintentará con backoff exponencial sin límite de intentos.
- `ERROR_PERMANENTE` — fallo definitivo (400, 403, 404, 422); requiere intervención manual.
- `CONFLICTO` — la API respondió 409 (mismo UUID, contenido diferente); requiere intervención manual.

Las gestiones offline **nunca** son reemplazadas por datos descargados desde el servidor. Ver ADR-0038.

### RN-15 Reemplazo de datos financieros
Los datos descargados desde el servidor **reemplazan** los valores locales vigentes:
- Interés penal.
- Gastos de cobranza.
- Otros valores financieros de cuotas.
- Nueva asignación diaria.
- Cambios de cartera y asignación.

**No se reemplazan:**
- Gestiones creadas offline que no hayan sido enviadas.
- Fotografías pendientes de envío.
- Operaciones pendientes de sincronización.

### RN-16 Geolocalización en gestiones
- La geolocalización puntual es **obligatoria** para guardar una gestión.
- Sin coordenadas disponibles: no se permite guardar.
- GPS desactivado: no se permite registrar la gestión.
- GPS con precisión baja: se permite y se registra la precisión.
- Se almacena: latitud, longitud, precisión en metros, fecha de captura, proveedor de ubicación, indicador de ubicación simulada.
- No se implementa tracking continuo ni se almacenan recorridos.

### RN-17 Observación de dirección desde terreno
- El ejecutivo puede registrar una observación cuando detecta que la dirección de una persona es incorrecta o incompleta.
- Se modela como `observacion_direccion`: no reemplaza la dirección original.
- La dirección importada continúa siendo la dirección operativa del sistema.
- La observación **no** modifica coordenadas ni rutas, ni activa proceso de aprobación en el MVP.
- La futura integración con el sistema externo podrá usar estas observaciones para corregir datos; ese proceso queda fuera del MVP.

### RN-18 Búsqueda por RUT
- **Búsqueda local:** en la asignación diaria descargada (sin conexión). Filtro en `:feature:asignacion`.
- **Búsqueda global:** `POST /api/v1/personas/busquedas` con el RUT en el body (no en la URL — ADR-0041). Rol requerido: `EJECUTIVO_TERRENO`.
- El resultado de la búsqueda global se persiste localmente como snapshot JSON en la tabla `persona_directa` de Room, permitiendo registrar una gestión con `BUSQUEDA_DIRECTA` aunque se pierda la conexión después de la búsqueda (ADR-0042).
- Auditoría: cada búsqueda genera un log estructurado `[BUSQUEDA_AUDITORIA]` con `ejecutivoId` y `personaId` (sin RUT en el log).
- La respuesta de búsqueda incluye `Cache-Control: no-store` para prevenir caché de PII.

### RN-19 Importación inicial
- Formato inicial: CSV.
- La importación valida estructura y contenido.
- Registra filas aceptadas, rechazadas y advertencias.
- Conserva historial de cargas.
- Futuro: XLSX si se requiere; luego integración por API con el sistema externo.

### RN-20 Estado de dispositivos
La aplicación administrativa muestra por cada dispositivo:
- Usuario asociado.
- Identificador del dispositivo.
- Última sincronización.
- Última versión de la aplicación.
- Cantidad de operaciones pendientes reportadas.
- Último error conocido.
- Estado: activo o revocado.

### RN-21 Responsable de la asignación diaria
- El **supervisor** crea y publica la asignación diaria de cada ejecutivo de terreno.
- Selecciona hasta ~50 personas desde la asignación mensual del ejecutivo.
- Se registra `supervisor_id`, `fecha_creacion` y `fecha_publicacion` en la asignación diaria.
- La app Android descarga solo asignaciones con estado `PUBLICADA` del ejecutivo autenticado.

### RN-22 Estados de asignación diaria
Estados funcionales confirmados (ver `docs/dominio/CICLOS_DE_VIDA.md`):
- `BORRADOR` — en preparación por el supervisor. No visible para el ejecutivo.
- `PUBLICADA` — disponible para descarga. La app Android la reconoce y descarga.
- `FINALIZADA` — terminó su vigencia operacional. Solo lectura histórica.
- `CANCELADA` — opcional; requiere que se defina el flujo administrativo correspondiente antes de implementar.

`DESCARGADA` **no es un estado funcional** de la asignación. La descarga es un evento técnico registrado por separado (ver concepto `descarga_asignacion_diaria` en `CICLOS_DE_VIDA.md`).

### RN-23 Retención de datos en el dispositivo Android
- El dispositivo conserva la asignación diaria vigente como proyección completa.
- En cada descarga de bundle, `BundleReplacementTransaction.reemplazar()` **reemplaza en su totalidad** los datos de personas, operaciones, cuotas, direcciones, avales y gestiones históricas.
- La tabla `gestion_local` (outbox) **no tiene FK a persona** y **no se elimina** durante la descarga de un nuevo bundle. Las gestiones pendientes sobreviven a cualquier descarga.
- Los campos de persona (`personaRutNumero`, `personaRutDv`, `personaNombre`) están **desnormalizados** en `gestion_local` para no depender de que la persona siga en Room.
- Al cerrar sesión (logout), `limpiarTodo()` elimina `gestion_local` junto con todas las demás tablas.
- La limpieza selectiva por asignación anterior **no se implementa en el MVP**. Ver ADR-0037.

### RN-24 Sesión local persistente y tokens de API
El primer login siempre requiere conexión y usa usuario y contraseña.

Tres conceptos separados:
- **Sesión local Android** — persiste mientras el usuario no ejecute logout. La pérdida de red no la cierra.
- **Access token** — para consumir la API. No tiene duración indefinida; se renueva con el refresh token.
- **Refresh token** — para renovar el access token cuando vuelva la conexión.

Reglas:
- La app puede operar offline con los datos descargados.
- No se usa PIN local ni biometría en el MVP.
- Al recuperar conectividad, la app valida o renueva la autenticación, verifica que el usuario esté activo y que el dispositivo no esté revocado, y sincroniza.
- La revocación administrativa es efectiva cuando el teléfono recupera conectividad.
- Cerrar la aplicación (background/kill) **no equivale** a cerrar sesión.
- No se permite que otro usuario inicie sesión mientras existan datos pendientes del usuario actual.

**Logout — política MVP:**

Se consideran pendientes: gestiones no enviadas, fotografías no enviadas, ubicaciones asociadas no enviadas, operaciones del outbox en estado distinto de sincronizado.

- Si **no hay pendientes**: logout permitido. Se eliminan sesión y credenciales locales.
- Si **hay pendientes y hay conexión**: la app intenta sincronizar.
  - Si la sincronización termina correctamente: logout permitido.
  - Si la sincronización falla: la sesión se mantiene abierta.
- Si **hay pendientes y no hay conexión**: no se permite logout.
- No se eliminan silenciosamente gestiones ni fotografías pendientes bajo ninguna circunstancia.

### RN-25 Observación de dirección
- Entidad `observacion_direccion` registrada por el ejecutivo desde terreno.
- Es solo una anotación, no una corrección activa en el MVP.
- No activa proceso de aprobación ni modifica datos del sistema externo.

### RN-26 Avales — solo lectura mínima
- Los avales se muestran solo en el detalle de la persona en la app Android.
- Solo se exponen: `rut_numero`, `rut_dv`, `nombre`.
- No generan ninguna acción en el sistema.

### RN-28 Estado calculado de usuario

El estado de un usuario para la vista administrativa se calcula en el momento de la consulta con la siguiente precedencia (de mayor a menor):

1. **INACTIVO** — `activo = false`. El usuario no puede iniciar sesión.
2. **BLOQUEADO** — `activo = true` y `bloqueado = true`. Bloqueo permanente hasta desbloqueo explícito.
3. **BLOQUEADO_TEMPORAL** — `activo = true`, `bloqueado = false`, y `bloqueado_hasta > ahora`. Bloqueo automático con expiración.
4. **ACTIVO** — ninguna condición anterior aplica.

El campo `bloqueado_hasta` no afecta el estado si el usuario está `INACTIVO` o `BLOQUEADO`.
El estado calculado no se persiste en la base de datos; se deriva de los campos base en cada consulta.

### RN-27 Versión mínima Android (provisional)
- `minSdk`: API 29 — Android 10.
- Esta decisión es **provisional** y depende del inventario real de teléfonos corporativos.
- No crear el proyecto Android ni archivos Gradle hasta confirmar con el inventario.

**Pendiente (no bloqueante):** obtener del inventario de dispositivos: modelo, versión Android, RAM, almacenamiento disponible, cantidad de equipos, y política de actualización de los teléfonos.

### RN-29 Último administrador activo (Fase 5B-2 ✅)
- Si una operación (desactivar, bloquear) dejaría al sistema sin ningún usuario con rol `JEFE_SUPERVISORES` o `TECNOLOGIA` que esté `activo=true` y `bloqueado=false`, la operación se rechaza con HTTP 409 `ULTIMO_ADMINISTRADOR`.
- El conteo excluye al propio usuario target de la operación.

### RN-30 Auto-lockout prohibido (Fase 5B-2 ✅)
- Un administrador no puede desactivar ni bloquear su propia cuenta. Error HTTP 409 `AUTO_LOCKOUT`.
- Sí puede editar sus propios datos básicos y restablecer su propia contraseña.

### RN-31 Contraseña máximo 72 bytes UTF-8 (Fase 5B-2 ✅)
- BCrypt trunca silenciosamente a 72 bytes. Se valida antes de hashear: si `contrasena.getBytes(UTF_8).length > 72` → HTTP 400.
- La validación aplica tanto en creación como en restablecimiento de contraseña.

### RN-32 Locking optimista en edición de usuarios (Fase 5B-2 ✅)
- El detalle de usuario (`GET /admin/usuarios/{id}`) incluye un campo `version` (entero incremental).
- La edición de datos básicos (`PUT .../datos-basicos`) requiere enviar la `version` actual del recurso.
- Si hay conflicto de escritura concurrente, la API devuelve HTTP 409 `CONFLICTO_VERSION`.

### RN-33 activo y bloqueado son ortogonales (Fase 5B-2 ✅)
- `activar()` solo cambia `activo=true`. No toca el campo `bloqueado`.
- Un usuario puede estar en estado `activo=true, bloqueado=true` simultáneamente.
- Para que un usuario pueda acceder al sistema se requiere: `activo=true AND bloqueado=false AND (bloqueado_hasta IS NULL OR bloqueado_hasta > ahora)`.
- Ver también RN-28 (estado calculado).

### RN-34 Revocación de sesiones al modificar seguridad (Fase 5B-2 ✅)
- Al desactivar, bloquear o restablecer la contraseña de un usuario, se publica `SeguridadUsuarioModificadaEvent`.
- El listener en el módulo `autenticacion` (`UsuarioAdminEventListener`) revoca todas las sesiones activas y refresh tokens del usuario target.
- La revocación es atómica con la operación principal (listener `BEFORE_COMMIT`).
- El JWT de acceso sigue siendo válido hasta su TTL (máx 15 min); esta ventana residual es aceptada por diseño. Ver ADR-0047, ADR-0048.

### RN-35 Roles iniciales en creación de usuarios (Fase 5B-2 ✅)
- Los roles se asignan exclusivamente durante la creación del usuario (`POST /admin/usuarios`).
- No existe endpoint para modificar roles post-creación en la fase actual.
- El endpoint `GET /admin/roles` expone el catálogo de roles disponibles para poblar el formulario de creación.

## PENDIENTE — Reglas por confirmar

| ID   | Pregunta                                                                                                    |
|------|-------------------------------------------------------------------------------------------------------------|
| P-01 | ¿El ejecutivo puede registrar gestiones sobre personas fuera de su asignación diaria activa?                |
| P-02 | ¿Los ejecutivos ven gestiones de otros ejecutivos sobre la misma persona en la app Android?                 |
| P-03 | ¿Cuál es el catálogo completo futuro de tipos de gestión? (los tres iniciales están confirmados)            |
| P-04 | ¿Se implementa exportación a Excel en la Fase 1 o en una posterior?                                         |
| P-05 | ¿Puede el supervisor modificar una asignación `PUBLICADA` o debe crear una nueva?                           |
| P-06 | ¿Se implementa `CANCELADA` como estado de asignación diaria en el MVP?                                      |
| P-07 | Confirmar minSdk definitivo con inventario de dispositivos corporativos (ver RN-27).                        |
| P-08 | Confirmar si el aval se asocia a persona u operación en el sistema externo definitivo (no bloqueante).      |
