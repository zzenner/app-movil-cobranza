# Deuda técnica

Este documento registra deuda técnica real identificada, con contexto suficiente para que se entienda su impacto y se decida cuándo abordarla.

## Deuda activa

### ~~DT-001 — Mecanismo de autenticación offline por confirmar~~ → Resuelto

Ver **DT-R04** en la sección de deuda resuelta.

---

### DT-002 — Cifrado de base de datos local Android no evaluado
**Área:** Android.
**Descripción:** No se ha evaluado el impacto en rendimiento de SQLCipher u otra opción de cifrado para Room, ni la compatibilidad con las versiones de Android objetivo.
**Impacto:** Seguridad de datos en dispositivos no protegidos o perdidos.
**Decisión recomendada:** Evaluar en la Fase 3 antes de implementar Room. Dado que los teléfonos son corporativos, verificar si el cifrado del sistema operativo Android es suficiente.

---

### ~~DT-003 — Versión mínima de Android sin definir~~ → Resuelta provisionalmente

Ver **DT-R05** en la sección de deuda resuelta.

---

### DT-004 — Almacenamiento de fotografías sin diseño
**Área:** Android, API.
**Descripción:** El sistema contempla fotografías en gestiones, pero no se ha definido la solución de almacenamiento (S3 propio, MinIO, otro), el proceso de subida desde Android ni el tamaño máximo por imagen.
**Impacto:** Afecta el contrato OpenAPI, el módulo de sincronización Android y la infraestructura de producción.
**Decisión recomendada:** Diseñar antes de implementar el registro de gestiones en la Fase 3. El sistema se diseña para ser compatible con S3, pero no se implementa en Fase 1.

---

### DT-005 — Número máximo de reintentos de sincronización sin definir
**Área:** Android (WorkManager).
**Descripción:** No se ha definido el umbral de reintentos antes de marcar una gestión como `ERROR_PERMANENTE` ni los parámetros exactos del backoff exponencial (intervalo inicial, factor, máximo).
**Impacto:** Afecta cuánto tiempo puede quedar una gestión en estado `ERROR_REINTENTABLE` y la experiencia del ejecutivo en zonas de conectividad intermitente.
**Decisión recomendada:** Definir empíricamente en la Fase 3 mediante pruebas con conectividad real. Valores iniciales razonables: 3 reintentos, backoff de 30s–1min–2min.

---

### DT-006 — Integración con sistema externo pendiente de diseño
**Área:** API, importación.
**Descripción:** La asignación mensual se cargará inicialmente mediante CSV, pero la integración definitiva es mediante API del sistema corporativo externo. El contrato de esa API no está disponible todavía.
**Impacto:** El módulo de importación debe diseñarse para soportar ambos mecanismos. La migración de CSV a API es un cambio significativo.
**Decisión recomendada:** Diseñar el módulo de importación con una abstracción que permita cambiar la fuente (CSV, API externa) sin reescribir la lógica de carga y validación.

---

### ~~DT-007 — Docker Desktop no integrado con la distro WSL2 de desarrollo~~ → Resuelto

Ver **DT-R06** en la sección de deuda resuelta.

---

## Deuda resuelta

### DT-R04 — Mecanismo de reapertura de la app estando offline (resuelto 2026-07-26)
**Descripción:** No se usa PIN local ni biometría dentro de la app en el MVP. La sesión local Android persiste hasta que el usuario ejecute logout explícitamente. La seguridad física del dispositivo la gestiona el SO del teléfono corporativo (bloqueo de pantalla del sistema).
**Referencia:** RN-24, RF-01b, RF-01d.

### DT-R05 — Versión mínima de Android (provisionalmente resuelta 2026-07-26)
**Descripción:** Se establece `minSdk = API 29` (Android 10) como valor provisional. La decisión definitiva depende del inventario de dispositivos corporativos. No se crea el proyecto Android hasta confirmar con el inventario.
**Referencia:** ADR-0011, RN-27, STATUS.md P-07.

### DT-R06 — Docker Desktop integrado con WSL2 (resuelto 2026-07-27)
**Descripción:** Docker Desktop 4.60.0 con integración WSL2 habilitada. Docker Compose v5.0.2 disponible. PostgreSQL/PostGIS levantado correctamente con `./scripts/start.sh`.
**Referencia:** ADR-0017, Fase 1B.

### DT-R01 — Formato de RUT (resuelto 2026-07-26)
**Descripción:** Se almacena como `rut_numero` + `rut_dv` en dos columnas separadas.
**Referencia:** ADR-0007.

### DT-R02 — Origen de datos de personas y créditos (resuelto 2026-07-26)
**Descripción:** La plataforma consume datos del sistema externo; no los gestiona directamente. La carga inicial es por CSV; luego se integrará por API.
**Referencia:** RN-08, DT-006 (deuda residual de integración).

### DT-R03 — Catálogo de tipos de gestión (resuelto 2026-07-26)
**Descripción:** `CONTACTO_FAMILIAR`, `COMPROMISO_PAGO`, `SIN_CONTACTO`. Pueden añadirse más en el futuro.
**Referencia:** RN-11.
