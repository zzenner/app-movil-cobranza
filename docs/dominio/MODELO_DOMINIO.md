# Modelo de dominio

Modelo actualizado de entidades del dominio de cobranza en terreno, basado en las decisiones funcionales confirmadas. La unidad principal de gestión es la **persona**.

## Jerarquía principal

```
cartera
  └── persona
        ├── operaciones
        │     └── cuotas
        ├── avales
        ├── gestiones
        └── direcciones
```

## Entidades

### Persona
Unidad principal de gestión. Titular de operaciones de deuda. El cobrador visita personas, no operaciones.

**Atributos:**
- `rut_numero` — parte numérica del RUT (sin dígito verificador).
- `rut_dv` — dígito verificador del RUT.
- Nombre completo.
- Direcciones (una o más; ver entidad Dirección).
- Teléfonos de contacto.

**Reglas:**
- Puede pertenecer a una o más carteras activas simultáneamente (ver `carteras_personas`).
- Tiene como máximo un ejecutivo responsable activo por cartera.
- Puede acumular múltiples gestiones históricas.

---

### Operación
Obligación financiera de una persona. Equivale a un crédito o deuda vigente.

**Atributos:**
- Identificador de operación.
- Persona titular.
- Valores financieros: capital, interés, interés penal, gastos de cobranza, total vigente.

**Reglas:**
- Una persona puede tener varias operaciones.
- Los valores financieros se actualizan desde el servidor en cada sincronización.

---

### Cuota
Pago parcial programado de una operación.

**Atributos:**
- Número de cuota.
- Monto.
- Fecha de vencimiento.
- Estado (vencida / vigente / futura).
- Interés penal acumulado.

**Reglas:**
- Una operación puede tener varias cuotas (vencidas y futuras).
- El teléfono recibe un promedio de ~3 cuotas por operación.

---

### Aval
Información de solo lectura sobre quien garantiza una operación de la persona. Proviene de la carga CSV y del sistema externo.

**Atributos (MVP):**
- `rut_numero` — parte numérica del RUT del aval.
- `rut_dv` — dígito verificador del RUT del aval.
- `nombre` — nombre completo del aval.

**Reglas:**
- Una persona puede tener uno o más avales.
- El aval se muestra en el detalle de la persona en la app Android.
- El aval no recibe asignaciones, gestiones ni compromisos.
- El aval no genera rutas ni aparece en búsquedas directas.
- No requiere dirección ni teléfonos en el MVP.
- Solo lectura: no se registran gestiones sobre avales.

**PENDIENTE (no bloqueante):** confirmar si, en el sistema externo definitivo, el aval está asociado a la persona o a una operación específica. Para el MVP se modela directo a la persona.

---

### Cartera
Agrupación de personas para gestión de cobranza.

**Atributos:**
- Nombre descriptivo.
- Estado (activa / inactiva).

**Reglas:**
- Una persona puede pertenecer a una o más carteras activas simultáneamente.
- Para un mismo par persona–cartera puede existir como máximo un vínculo activo.
- El historial de vínculos se conserva sin borrado físico (tabla `carteras_personas`).

---

### Usuario
Persona con acceso al sistema.

**Roles confirmados:** `JEFE_SUPERVISORES`, `TECNOLOGIA`, `SUPERVISOR`, `EJECUTIVO_TERRENO`.

---

### Asignación mensual
Conjunto de personas asignadas a un ejecutivo de terreno para el mes.

**Atributos:**
- Ejecutivo asignado.
- Conjunto de personas (cartera del mes).
- Fecha de inicio y término.

**Reglas:**
- ~300–400 personas por ejecutivo por mes.
- Una persona no puede estar asignada a dos ejecutivos simultáneamente.
- Se conserva historial completo.
- Carga inicial: CSV. Futuro: API del sistema externo.

---

### Asignación diaria
Subconjunto de la asignación mensual preparado y publicado por el supervisor para el trabajo de un ejecutivo en un día.

**Atributos:**
- Fecha de la asignación.
- Ejecutivo destinatario.
- Supervisor que la creó.
- Personas incluidas (~50).
- Estado del ciclo de vida.
- Fecha de creación y de publicación.

**Estados funcionales** (ver `docs/dominio/CICLOS_DE_VIDA.md`):
- `BORRADOR` — en preparación por el supervisor.
- `PUBLICADA` — disponible para descarga por el ejecutivo.
- `FINALIZADA` — terminó su vigencia operacional.
- `CANCELADA` — opcional; requiere flujo administrativo definido antes de implementar.

La descarga no es un estado funcional de la asignación; es un evento técnico registrado en `descarga_asignacion_diaria`.

**Reglas:**
- Es la unidad de descarga al teléfono.
- Es la base de la ruta de trabajo diario.
- La crea y publica el **supervisor**, no el ejecutivo.
- La app Android descarga exclusivamente asignaciones en estado `PUBLICADA` del ejecutivo autenticado.
- El ejecutivo no selecciona por sí mismo las personas del día.
- Se registra el supervisor que la creó y la fecha de publicación.
- Se conserva historial completo de asignaciones diarias.

---

### Gestión
Registro inmutable de un contacto o acción realizada por un ejecutivo sobre una persona.

**Atributos:**
- UUID generado en el dispositivo.
- Ejecutivo.
- Persona visitada.
- Tipo: `CONTACTO_FAMILIAR`, `COMPROMISO_PAGO`, `SIN_CONTACTO`.
- Fecha y hora del registro en el dispositivo.
- Geolocalización puntual (obligatoria).
- Observaciones (texto libre).
- Fecha de compromiso (solo para `COMPROMISO_PAGO`).
- Fotografías (opcionales, varias permitidas).
- Estado técnico de sincronización.

**Reglas:**
- Registrada sobre una persona (no sobre una operación específica).
- Inmutable desde su creación. No hay rectificaciones ni anulaciones.
- La geolocalización es obligatoria; sin coordenadas no se puede guardar.

---

### Fotografía de gestión
Imagen adjuntada a una gestión.

**Atributos:**
- Referencia al archivo (futuro: compatible con S3).
- Gestión asociada.
- Fecha de captura.

---

### Dirección
Dirección de contacto de una persona, proveniente del sistema externo. No se sobrescribe ni modifica.

**Reglas:**
- Es solo lectura en el MVP.
- Continúa siendo la dirección operativa del sistema.

---

### Observación de dirección
Anotación que el ejecutivo puede registrar desde terreno cuando detecta que la dirección de una persona es incorrecta o incompleta. No es una corrección activa; es una observación para revisión futura.

**Atributos:**
- `id`
- `persona_id` — persona a la que refiere.
- `direccion_id` — dirección original a la que refiere (opcional).
- `observacion` — texto libre describiendo el problema o corrección.
- `direccion_reportada` — nueva dirección sugerida (opcional).
- `usuario_id` — ejecutivo que registró la observación.
- `dispositivo_id` — dispositivo desde el que se registró.
- `fecha_dispositivo` — momento del registro en el dispositivo.
- `fecha_servidor` — momento de recepción en la API.

**Reglas:**
- No reemplaza la dirección original.
- No modifica coordenadas ni rutas.
- No activa proceso de aprobación en el MVP.
- No actualiza la base de datos externa.
- La futura integración podrá usar estas observaciones para corregir datos, pero ese proceso queda fuera del MVP.

---

### Supervisión
Relación entre un supervisor y los ejecutivos bajo su cargo.

**Atributos:**
- Supervisor.
- Ejecutivo.
- Fecha de inicio.
- Fecha de fin (NULL = relación activa).

**Reglas:**
- Se conserva historial completo de cambios de supervisión.

---

### Dispositivo
Teléfono corporativo asociado a un usuario ejecutivo.

**Atributos:**
- Identificador del dispositivo.
- Usuario asociado.
- Última sincronización.
- Última versión de la app.
- Cantidad de operaciones pendientes reportadas.
- Último error conocido.
- Estado: activo o revocado.

---

## Relaciones principales

```
Cartera             N --- M  Persona              (via carteras_personas, con historial)
Persona             1 --- N  Operacion
Operacion           1 --- N  Cuota
Persona             1 --- N  Aval
Persona             1 --- N  Gestion
Gestion             1 --- N  FotografiaGestion
Persona             1 --- N  Direccion
Persona             1 --- N  ObservacionDireccion
AsignacionMensual   N --- M  Persona
AsignacionDiaria    N --- M  Persona
AsignacionDiaria    N --- 1  AsignacionMensual
AsignacionDiaria    N --- 1  Supervisor (creador)
Supervisor          1 --- N  EjecutivoTerreno  (via Supervision)
Usuario             1 --- 1  Dispositivo
```

## PENDIENTE

- Confirmar si una asignación diaria puede modificarse una vez publicada (P-05).
- Confirmar si `CANCELADA` se implementa en el MVP (P-06).
- Confirmar versión mínima de Android definitiva con inventario (provisional: API 29 — ver ADR-0011, P-07).
- Confirmar si el aval se asocia a la persona o a una operación específica en el sistema externo definitivo (P-08, no bloqueante).
