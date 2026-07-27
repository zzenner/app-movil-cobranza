# Glosario del dominio

Términos utilizados en el sistema de cobranza en terreno. Los identificadores técnicos (tablas, columnas, clases) siguen las convenciones del proyecto: español sin tildes ni eñes, snake_case en SQL.

## Roles del sistema

| Término              | Identificador técnico    | Descripción                                                                    |
|----------------------|--------------------------|--------------------------------------------------------------------------------|
| Jefe de supervisores | `JEFE_SUPERVISORES`      | Administra supervisores y tiene visibilidad global del sistema.                 |
| Tecnología           | `TECNOLOGIA`             | Administrador técnico del sistema. Gestiona usuarios, importaciones y configuración. |
| Supervisor           | `SUPERVISOR`             | Responsable de un grupo de ejecutivos de terreno. Supervisa gestiones y resultados. |
| Ejecutivo de terreno | `EJECUTIVO_TERRENO`      | Cobrador que opera con la app Android. Recibe asignaciones y registra gestiones. |

## Entidades principales

| Término              | Identificador técnico    | Descripción                                                                    |
|----------------------|--------------------------|--------------------------------------------------------------------------------|
| Persona              | `persona`                | Unidad principal de gestión. Titular de operaciones de deuda. Se identifica por RUT. |
| RUT                  | `rut_numero` / `rut_dv`  | Rol Único Tributario. Se almacena separado: número y dígito verificador.       |
| Operación            | `operacion`              | Obligación financiera de una persona. Equivale a un crédito o deuda vigente.  |
| Cuota                | `cuota`                  | Pago parcial programado de una operación. Puede estar vencida o vigente.       |
| Aval                 | `aval`                   | Persona que garantiza una operación. Puede ser a su vez titular de otras operaciones. |
| Cartera              | `cartera`                | Agrupación de personas asignadas para gestión de cobranza.                     |
| Ejecutivo responsable| `ejecutivo_responsable`  | Ejecutivo de terreno con asignación mensual activa sobre una persona. Solo uno activo a la vez. |

## Asignaciones

| Término              | Identificador técnico    | Descripción                                                                    |
|----------------------|--------------------------|--------------------------------------------------------------------------------|
| Asignación mensual   | `asignacion_mensual`     | Conjunto de personas asignadas a un ejecutivo para el mes. ~300–400 personas. Cargada desde CSV (futuro: API). |
| Asignación diaria    | `asignacion_diaria`      | Subconjunto creado y publicado por el supervisor. ~50 personas. Base de la ruta de trabajo. La descarga el ejecutivo. |
| Ruta de trabajo      | —                        | Conjunto de personas de la asignación diaria que el ejecutivo visitará. Futuro: generación automática de ruta. |

### Estados funcionales de la asignación diaria (confirmados)

| Estado       | Descripción                                                                               |
|--------------|-------------------------------------------------------------------------------------------|
| `BORRADOR`   | El supervisor está preparando la asignación. No visible al ejecutivo.                    |
| `PUBLICADA`  | Disponible para descarga por la app Android.                                              |
| `FINALIZADA` | Terminó su vigencia operacional. Solo lectura histórica.                                  |
| `CANCELADA`  | Anulada antes de ser utilizada. Opcional — requiere flujo administrativo definido antes de implementar. |

`DESCARGADA` **no es un estado funcional.** La descarga es un evento técnico registrado por separado.

### Descarga de asignación diaria

| Término                      | Identificador técnico              | Descripción                                                                  |
|------------------------------|------------------------------------|------------------------------------------------------------------------------|
| Descarga de asignación diaria| `descarga_asignacion_diaria`       | Evento técnico que registra cuándo y desde qué dispositivo se descargó una asignación diaria. Incluye primera descarga, última descarga y versión de la app. Una asignación puede descargarse más de una vez. |

## Gestiones

| Término              | Identificador técnico    | Descripción                                                                    |
|----------------------|--------------------------|--------------------------------------------------------------------------------|
| Gestión              | `gestion`                | Registro de un contacto o acción realizada por un ejecutivo sobre una persona. Inmutable desde su creación. |
| Contacto familiar    | `CONTACTO_FAMILIAR`      | Tipo de gestión: contacto con un familiar de la persona titular.               |
| Compromiso de pago   | `COMPROMISO_PAGO`        | Tipo de gestión: acuerdo verbal de pago. Requiere fecha de compromiso.         |
| Sin contacto         | `SIN_CONTACTO`           | Tipo de gestión: visita sin resultado de contacto.                             |
| Fotografía           | `fotografia_gestion`     | Imagen opcional adjuntada a una gestión. Se permiten varias por gestión.       |
| Geolocalización      | `geolocalizacion`        | Coordenadas capturadas obligatoriamente al momento de registrar la gestión.    |

## Sincronización y sesión

| Término               | Descripción                                                                          |
|-----------------------|--------------------------------------------------------------------------------------|
| `PENDIENTE_ENVIO`     | Estado técnico: gestión registrada localmente, esperando envío al servidor.          |
| `ENVIANDO`            | Estado técnico: gestión en proceso de envío a la API.                                |
| `SINCRONIZADA`        | Estado técnico: gestión confirmada por la API y persistida en el servidor.           |
| `ERROR_REINTENTABLE`  | Estado técnico: fallo temporal. Se reintentará con backoff exponencial.             |
| `ERROR_PERMANENTE`    | Estado técnico: fallo definitivo. Requiere intervención.                             |
| Modo offline          | Operación de la app Android sin conexión a internet, usando datos locales Room.      |
| Sincronización manual | Acción iniciada por el ejecutivo: "Sincronizar asignación" o "Enviar gestiones pendientes". |
| Sesión local Android  | Estado de autenticación persistente en el dispositivo. No se cierra al perder red.  |
| Access token          | Credencial temporal para consumir la API. Se renueva con el refresh token.           |
| Refresh token         | Credencial de larga duración para renovar el access token al recuperar conectividad. |
| Retención de datos    | Política de qué datos se conservan en Room cuando llega una nueva asignación diaria. |

## Direcciones

| Término                  | Identificador técnico        | Descripción                                                                               |
|--------------------------|------------------------------|-------------------------------------------------------------------------------------------|
| Dirección importada      | `direccion`                  | Dirección original de la persona, proveniente del sistema externo. No se sobrescribe.     |
| Observación de dirección | `observacion_direccion`      | Anotación del ejecutivo cuando detecta un error en la dirección. No es una corrección activa en el MVP. |

## Términos técnicos (en inglés por convención estándar)

| Término          | Descripción                                                                    |
|------------------|--------------------------------------------------------------------------------|
| Outbox           | Patrón de escritura local que garantiza entrega eventual de eventos al servidor.|
| UUID             | Identificador único universal. Generado en el dispositivo para gestiones.       |
| WorkManager      | Componente Android para sincronización en segundo plano con garantías de entrega.|
| Room             | Capa de abstracción sobre SQLite en Android, usada como base de datos local.    |
| Flyway           | Herramienta de migraciones de esquema para PostgreSQL.                          |
| PostGIS          | Extensión geoespacial de PostgreSQL. Almacena coordenadas de gestiones.         |
| Spring Modulith  | Extensión de Spring Boot para estructurar módulos de negocio en un monolito.   |
| DAO              | Data Access Object. Interfaz de acceso a datos en Room.                         |
| CSV              | Comma-Separated Values. Formato inicial para importación de asignaciones.       |

## PENDIENTE

- Confirmar el catálogo completo de tipos de resultado de gestión (los tres iniciales están confirmados).
- Definir mecanismo de reapertura de la app estando offline (autenticación offline).
- Confirmar versión mínima de Android definitiva según inventario (provisional: API 29 / Android 10).
- Confirmar si `CANCELADA` se implementa en el MVP (requiere flujo administrativo definido).
- Confirmar si el aval se asocia a persona u operación en el sistema externo definitivo (no bloqueante).
