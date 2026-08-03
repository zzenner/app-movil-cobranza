# ADR-0041 — Endpoint de búsqueda por RUT con POST por privacidad

**Estado:** Aceptado
**Fecha:** 2026-08-03
**Fase:** 4C-B — Búsqueda directa por RUT

---

## Contexto

La Fase 4C-B introduce la capacidad de buscar personas globalmente por RUT desde la app Android. El RUT es un dato personal sensible (PII). La decisión de diseño del endpoint afecta: privacidad, trazabilidad en logs, comportamiento de proxies y caché HTTP.

Se evaluaron dos enfoques:

| Alternativa | Ejemplo |
|---|---|
| GET con RUT en query string | `GET /api/v1/personas?rutNumero=12345678&rutDv=9` |
| POST con RUT en el body | `POST /api/v1/personas/busquedas` |

---

## Decisión

**Usar `POST /api/v1/personas/busquedas` con el RUT en el body del request.**

Nombre del path: `/busquedas` (sustantivo en plural) para ajustarse a la semántica REST de recursos y permitir que el path no contenga el RUT en ningún momento.

---

## Fundamentos

### El GET expone PII en múltiples vectores

Un GET con parámetros en la URL provoca que el RUT aparezca en:
- Logs de acceso de la API (por defecto `INFO` en Spring Boot).
- Cabeceras `Referer` del navegador o cliente HTTP.
- Cachés intermedias de proxies y CDNs (los proxies pueden almacenar la URL completa).
- Historial del cliente (navegadores, apps CLI como `curl`).
- Registros de infraestructura (balanceadores, WAF, Nginx).

En Chile el RUT es un identificador personal regulado; su exposición innecesaria viola principios básicos de privacidad.

### El POST mantiene el RUT en el body

El body de un POST no aparece en logs de acceso estándar, no es parte del URI y no es visible para proxies intermedios a menos que hayan sido específicamente configurados para inspeccionar el body (lo que requiere TLS termination y configuración explícita).

### Semántica REST: POST para operaciones que no son idempotentes por naturaleza

Una búsqueda por RUT registra una auditoría en el lado del servidor (log estructurado `[BUSQUEDA_AUDITORIA]`). Es una operación con efecto secundario observable (la auditoría), por lo que el uso de POST está justificado.

### Cache-Control: no-store

La respuesta incluye `Cache-Control: no-store` para prevenir que cualquier capa intermedia almacene los datos personales de la persona encontrada.

---

## Módulo que implementa el endpoint

El endpoint vive en el módulo `sincronizacion`, no en `personas`.

**Razón:** `sincronizacion` ya orquesta consultas a `PersonaConsultaApi`, `OperacionConsultaApi` y `GestionConsultaApi` (Fase 3D). Crear el endpoint en `personas` requeriría que ese módulo importe `GestionConsultaApi` y `OperacionConsultaApi`, lo que crearía dependencias circulares en el grafo de Spring Modulith.

El módulo `sincronizacion` tiene `allowedDependencies = {"asignaciones::api", "personas::api", "operaciones::api", "gestiones::api"}` — todas las dependencias ya están declaradas.

---

## Contrato del endpoint

```
POST /api/v1/personas/busquedas
Authorization: Bearer <token>
Roles: EJECUTIVO_TERRENO

Body (application/json):
{
  "rutNumero": "15000001",
  "rutDv": "7"
}

Respuesta 200 (application/json):
{
  "version": 1,
  "generadoEn": "2026-08-03T12:00:00Z",
  "persona": { /* DatosPersonaDescarga */ }
}

Respuesta 400 — RUT inválido (dígito verificador incorrecto):
  code: RUT_INVALIDO

Respuesta 404 — Persona no existe:
  code: PERSONA_NO_ENCONTRADA
```

---

## Consecuencias

**Positivas:**
- El RUT no aparece en ningún log de acceso estándar.
- Compatible con la política de privacidad requerida para datos personales.
- El módulo `sincronizacion` puede validar el RUT a través de `RutValidacionApi` sin exponer el tipo de dominio interno `Rut`.

**Negativas:**
- POST para búsqueda es menos intuitivo que GET para consumidores de la API.
- No es cacheable por HTTP (deliberado).

---

## Referencias

- [ADR-0026](0026-dos-origenes-gestion.md) — Orígenes de gestión: ASIGNACION_DIARIA y BUSQUEDA_DIRECTA
- [ADR-0042](0042-persistencia-snapshot-directo-room-v3.md) — Snapshot en Room v3 para busqueda directa
- `contracts/openapi/cobranza-api.yaml` — Definición completa del endpoint
