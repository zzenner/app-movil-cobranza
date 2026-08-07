# Tarea actual

## Identificación

- **Fase:** 5C — Importación mensual administrativa de datos
- **Estado:** EN PREPARACIÓN — pendiente análisis de modelo y diseño de contrato
- **Rama activa:** `feature/fase-5c-importacion-mensual`
- **Base funcional:** `b3c4c8a feat: implementar gestion administrativa de usuarios fase 5b-2`
- **Tag de fase anterior:** `v0.16.0-usuarios-admin-write`

## Objetivo

Permitir cargar mensualmente mediante archivo CSV la información que alimenta el sistema de cobranza:
personas deudoras, operaciones de crédito, cuotas y asignaciones de carteras.

## Alcance preliminar Fase 5C

### Datos a importar

- Personas (deudores y avales)
- Operaciones de crédito
- Cuotas de cada operación
- Asignaciones/carteras (qué ejecutivo atiende qué cartera con qué personas)
- Identificadores externos: `persona_ext_id`, `operacion_ext_id`, `cuota_ext_id`

### Funcionalidades requeridas

- Carga mediante archivo CSV desde admin-web
- Validación previa al persistir (errores por fila, resumen de la carga)
- Procesamiento controlado (transaccional, sin estado inconsistente)
- Resumen post-carga: filas procesadas, creadas, actualizadas, rechazadas
- Prevención de duplicados (comportamiento ante registros existentes por ext_id)
- Trazabilidad: quién importó, cuándo, cuántos registros afectados
- Posibilidad de probar el proceso completo desde admin-web

## No incluye en 5C (fuera de alcance inicial)

- XLSX (salvo decisión explícita posterior)
- Integración automática con API externa corporativa
- Jobs programados / importación automática
- Importación Android
- Importación de usuarios (cubierta en Fase 5B-2)

## Preguntas abiertas — a resolver antes de implementar

- Formato definitivo del CSV (columnas, separador, encoding, cabecera)
- Comportamiento ante registros existentes: ¿reemplazar, ignorar o rechazar?
- ¿Atomicidad por lote completo o por fila?
- Carga mensual vs. carga inicial histórica: ¿mismo flujo?
- Tamaño esperado de archivos (para decidir streaming vs. carga completa)
- Significado exacto de `persona_ext_id`, `operacion_ext_id`, `cuota_ext_id`
- Relación entre RUT, operación y número de cuota en el CSV corporativo

## Siguiente acción exacta

"Analizar el modelo de datos y los documentos existentes para diseñar el contrato CSV
y el flujo de importación mensual de Fase 5C".

Leer en este orden:
1. `docs/dominio/REGLAS_NEGOCIO.md` — RN-01 a RN-15 (carteras, personas, operaciones, cuotas)
2. `docs/dominio/DIAGRAMA_ENTIDAD_RELACION.md`
3. `docs/dominio/DICCIONARIO_DATOS_PRELIMINAR.md`
4. Migraciones Flyway: `apps/api/src/main/resources/db/migration/` (V006–V009, carteras/personas/operaciones/asignaciones)
5. Entidades JPA: `Persona`, `Operacion`, `Cuota`, `Cartera`, `AsignacionMensual`
6. `docs/producto/REQUISITOS_FUNCIONALES.md` — RF-03 a RF-04 (gestión de carteras y asignaciones)
7. `docs/producto/HISTORIAS_USUARIO.md` — HU-008 (Asignar cartera a cobrador)
8. ADR existentes sobre carga inicial (si los hay)

## Fases anteriores — CERRADAS ✅

| Fase | Tag | Commit |
|---|---|---|
| 5B-2 Gestión administrativa usuarios (escritura) | `v0.16.0-usuarios-admin-write` | `b3c4c8a` |
| Entorno Docker local | `v0.15.0-entorno-docker-local` | `1a22c8a` |
| 5B-1 Consulta admin usuarios (solo lectura) | `v0.14.0-usuarios-admin-readonly` | `d82d95d` |
| 5A Base admin web + auth web | `v0.13.0-admin-base` | `71d47b2` |
| 4C-B Búsqueda directa | `v0.12.0-busqueda-directa` | `4cddf50` |
| 4C-A Gestiones offline | `v0.11.0-gestiones-offline` | `dec7b18` |
