# Deuda técnica

Este documento registra deuda técnica real identificada, con contexto suficiente para que se entienda su impacto y se decida cuándo abordarla.

## Deuda activa

### DT-001 — Mecanismo de autenticación offline por confirmar
**Área:** API, Android.
**Descripción:** El primer login es online. La app debe poder seguir operando cuando pierda conectividad. El mecanismo exacto para reabrir la app estando offline (¿huella dactilar, PIN de app, PIN del teléfono, sin desbloqueo adicional?) no está definido.
**Impacto:** Afecta el diseño del módulo de autenticación, el almacenamiento de tokens en Android y la experiencia de usuario en campo. Es una decisión de seguridad con implicaciones operacionales.
**Decisión recomendada:** Resolver antes de implementar autenticación. Los teléfonos son corporativos, lo que simplifica algunas opciones.

---

### DT-002 — Cifrado de base de datos local Android no evaluado
**Área:** Android.
**Descripción:** No se ha evaluado el impacto en rendimiento de SQLCipher u otra opción de cifrado para Room, ni la compatibilidad con las versiones de Android objetivo.
**Impacto:** Seguridad de datos en dispositivos no protegidos o perdidos.
**Decisión recomendada:** Evaluar en la Fase 3 antes de implementar Room. Dado que los teléfonos son corporativos, verificar si el cifrado del sistema operativo Android es suficiente.

---

### DT-003 — Versión mínima de Android sin definir
**Área:** Android.
**Descripción:** No se ha definido `minSdk` porque el inventario de dispositivos corporativos no está disponible. La recomendación preliminar es Android 10 (API 29), sujeta al inventario real.
**Impacto:** Determina qué APIs del sistema están disponibles y qué porcentaje de dispositivos son compatibles.
**Decisión recomendada:** Obtener el inventario de modelos y versiones Android de los dispositivos corporativos antes de iniciar el proyecto Android.

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

### DT-007 — Docker Desktop no integrado con la distro WSL2 de desarrollo
**Área:** Infraestructura local.
**Descripción:** Docker Desktop no responde en el PATH de la distro WSL2 activa. Los scripts de entorno local no funcionan hasta habilitar la integración.
**Impacto:** No se puede validar localmente el Docker Compose ni levantar PostgreSQL desde WSL2.
**Resolución:** Docker Desktop → Settings → Resources → WSL Integration → habilitar la distro activa.
**Decisión recomendada:** Resolver antes de iniciar la Fase 1.

---

## Deuda resuelta

### DT-R01 — Formato de RUT (resuelto 2026-07-26)
**Descripción:** Se almacena como `rut_numero` + `rut_dv` en dos columnas separadas.
**Referencia:** ADR-0007.

### DT-R02 — Origen de datos de personas y créditos (resuelto 2026-07-26)
**Descripción:** La plataforma consume datos del sistema externo; no los gestiona directamente. La carga inicial es por CSV; luego se integrará por API.
**Referencia:** RN-08, DT-006 (deuda residual de integración).

### DT-R03 — Catálogo de tipos de gestión (resuelto 2026-07-26)
**Descripción:** `CONTACTO_FAMILIAR`, `COMPROMISO_PAGO`, `SIN_CONTACTO`. Pueden añadirse más en el futuro.
**Referencia:** RN-11.
