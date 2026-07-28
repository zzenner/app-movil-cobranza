# ADR-0014 — Copia operacional local de datos del sistema externo

## Estado
Aceptado.

## Contexto
Los datos de personas, operaciones y cuotas provienen de un sistema corporativo externo. La app Android necesita acceso a esos datos en terreno, frecuentemente sin conexión. Se necesita decidir si la app consume los datos externos en tiempo real o si mantiene una copia operacional local.

## Decisión
La plataforma mantiene una **copia operacional** de los datos del sistema externo: personas, operaciones, cuotas y valores financieros se importan a la base de datos PostgreSQL y se descargan al dispositivo Android. La API es la autoridad de esa copia. El sistema externo es la fuente original.

La copia operacional se actualiza:
- En la importación masiva (CSV en Fase 1; API del sistema externo en el futuro).
- En cada sincronización entre el dispositivo y la API: los valores financieros del servidor reemplazan los locales.

## Consecuencias

**Positivas:**
- La app Android opera completamente offline sin depender del sistema externo durante el trabajo en terreno.
- Los valores financieros del servidor son siempre la referencia vigente (interés penal, gastos de cobranza acumulados).
- El módulo de importación puede validar, filtrar y enriquecer los datos antes de persistirlos, sin afectar el sistema externo.
- La arquitectura es independiente del proveedor externo; cambiar de fuente (CSV → API externa) no requiere reescribir el modelo de datos.

**Negativas:**
- Los datos pueden estar desactualizados si no se sincroniza con frecuencia. Los valores financieros cambian diariamente.
- Se mantiene una copia de datos sensibles en la base de datos propia, con las responsabilidades de seguridad y protección que eso implica.
- Los cambios en el esquema del sistema externo requieren actualizar el módulo de importación.

## Regla de autoridad

| Dato | Autoridad |
|------|-----------|
| Valores financieros (interés, cuotas, totales) | API (se actualizan en cada sincronización) |
| Personas, operaciones, cuotas | API (importación periódica) |
| Gestiones registradas en terreno | Dispositivo (el servidor es el destino final) |

## Alternativas consideradas

**Proxy en tiempo real:** La API retransmite datos del sistema externo bajo demanda. Requiere conexión permanente y dependencia de disponibilidad del sistema externo. Incompatible con offline-first. Se descartó.

**Sin copia: acceso offline solo a caché del navegador:** No aplica en el contexto de una app Android con requisito de operación offline completa. Se descartó.

## Referencias

- `docs/dominio/REGLAS_NEGOCIO.md` — RN-10, RN-15
- `docs/sincronizacion/ESTRATEGIA_OFFLINE.md` — Roles de cada componente
- `docs/gestion/DEUDA_TECNICA.md` — DT-006 (integración con sistema externo)
