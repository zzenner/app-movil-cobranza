# Matriz de autoridad de datos

Define quién es la fuente de verdad de cada tipo de dato en el sistema y qué componente puede leerlo o modificarlo.

**Última actualización:** 2026-07-26 (auditoría Fase 1A)

---

## Leyenda

| Símbolo | Significado |
|---|---|
| **A** | Autoridad — fuente de verdad. Solo esta fuente puede crear o modificar este dato. |
| **R** | Lectura — puede leer el dato pero no es su fuente de verdad. |
| **E** | Escritura — puede escribir el dato pero no es autoridad (sujeto a validación de la autoridad). |
| **—** | Sin acceso o no aplica. |

---

## Datos de identidad y acceso

| Dato | Sistema externo | API (PostgreSQL) | App Android (Room) | Admin Web |
|---|---|---|---|---|
| Usuarios y roles | — | **A** | R (token) | R |
| Contraseñas (hash) | — | **A** | — | — |
| Sesión local Android | — | — | **A** | — |
| Access token / refresh token | — | **A** (emite) | R (consume) | R (consume) |
| Dispositivos registrados | — | **A** | E (se registra) | R |
| Relaciones supervisor-ejecutivo | — | **A** | R | R |

---

## Datos de cartera y personas

| Dato | Sistema externo | API (PostgreSQL) | App Android (Room) | Admin Web |
|---|---|---|---|---|
| Personas (identidad, RUT, nombre) | **A** (origen) | R (copia operacional) | R (descarga) | R |
| Carteras | — | **A** | R | R |
| Asignación mensual | — | **A** | — | R |
| Avales (rut, nombre) | **A** (origen) | R (copia operacional) | R (descarga) | R |
| Direcciones importadas | **A** (origen) | R (copia operacional) | R (descarga) | R |

---

## Datos financieros

| Dato | Sistema externo | API (PostgreSQL) | App Android (Room) | Admin Web |
|---|---|---|---|---|
| Operaciones (capital, estado) | **A** (origen) | R (copia operacional) | R (descarga) | R |
| Cuotas (monto, vencimiento) | **A** (origen) | R (copia operacional) | R (descarga) | R |
| Valores vigentes (interés penal, gastos) | **A** (origen) | R (se actualiza en importación) | R (se actualiza en sync) | R |

Los valores financieros del servidor reemplazan los locales del dispositivo en cada sincronización. El sistema externo es la fuente original; la API mantiene la copia operacional vigente.

---

## Asignaciones diarias

| Dato | Sistema externo | API (PostgreSQL) | App Android (Room) | Admin Web |
|---|---|---|---|---|
| Asignación diaria (creación, estado) | — | **A** | R | R |
| Publicación de asignación | — | **A** (supervisor vía web) | — | E (supervisor publica) |
| Descarga de asignación | — | — | **A** (registra en Room) | — |
| Registro de descarga (`descargas_asignacion_diaria`) | — | **A** (registra el evento) | E (inicia la descarga) | — |

---

## Gestiones

| Dato | Sistema externo | API (PostgreSQL) | App Android (Room) | Admin Web |
|---|---|---|---|---|
| UUID de la gestión | — | — | **A** (genera en el dispositivo) | — |
| Gestión registrada (contenido) | — | **A** (destino final) | **A** (origen, inmutable) | R |
| Fotografías de gestión | — | **A** (destino final) | **A** (origen) | R |
| Estado de sincronización (técnico) | — | — | **A** | — |

Las gestiones tienen dos momentos de autoridad: se crean y son autoritativas en el dispositivo; al sincronizarse, la API es el repositorio permanente. El dispositivo nunca recibe correcciones de gestiones desde el servidor.

---

## Observaciones de dirección

| Dato | Sistema externo | API (PostgreSQL) | App Android (Room) | Admin Web |
|---|---|---|---|---|
| Observación de dirección | — | **A** (destino final) | **A** (origen, inmutable) | R |

La dirección original (importada) no se modifica. La observación es una anotación adicional.

---

## Datos técnicos de sincronización

| Dato | Sistema externo | API (PostgreSQL) | App Android (Room) | Admin Web |
|---|---|---|---|---|
| Cola outbox (operaciones a enviar) | — | — | **A** | — |
| Estado de sincronización por gestión | — | — | **A** | — |
| Timestamp de última sincronización | — | — | **A** | — |

---

## Reglas generales de autoridad

1. **El sistema externo** es la fuente original de personas, operaciones, cuotas y avales. La plataforma mantiene una copia operacional que se actualiza por importación.
2. **La API** es la autoridad de la copia operacional de datos financieros y de todas las entidades de negocio que se crean dentro de la plataforma (usuarios, asignaciones, etc.).
3. **El dispositivo Android** es la autoridad temporal de las gestiones y fotografías desde el momento de su creación hasta su sincronización. Una vez sincronizadas, la API es la referencia permanente.
4. **Los datos financieros** del servidor siempre reemplazan los locales al sincronizar. El dispositivo nunca es autoridad de datos financieros.
5. **Las gestiones son inmutables** en todos los componentes. Ningún componente puede modificarlas después de su creación.

---

## PENDIENTE

- Definir la autoridad sobre los estados financieros históricos (`estados_financieros_cuota` si se implementa como tabla separada).
- Confirmar si el Admin Web puede revocar dispositivos directamente o solo a través de la API.
- Definir qué ocurre con los datos locales del dispositivo cuando el dispositivo es revocado y luego reactivado.
