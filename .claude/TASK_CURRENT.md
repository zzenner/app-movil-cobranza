# Tarea actual

## Identificación

- **Fase:** 5C — Importación mensual administrativa de datos
- **Estado:** VALIDADA ✅ — LISTA PARA CIERRE
- **Rama activa:** `feature/fase-5c-importacion-mensual`
- **Base funcional:** `b3c4c8a feat: implementar gestion administrativa de usuarios fase 5b-2`
- **Tag de fase anterior:** `v0.16.0-usuarios-admin-write`

## Objetivo

Permitir cargar mensualmente mediante archivo CSV la información que alimenta el sistema de cobranza:
personas deudoras, operaciones de crédito, cuotas y asignaciones de carteras.

## Alcance definitivo Fase 5C

Ver el plan aprobado en el prompt de inicio de sesión. En resumen:

- CSV de 29 columnas (Persona 4, Dirección 5, Operación 9, Cuota 10, Asignación 1)
- Workflow: RECIBIDA → VALIDANDO → VALIDADA → PROCESANDO → COMPLETADA / CON_ERRORES / FALLIDA / EXPIRADA
- Validación previa sin escritura; confirmación explícita
- Upsert batch de personas, direcciones, operaciones, cuotas, asignaciones mensuales
- Supervisor resuelto desde supervision_usuarios
- Cartera por carteraId en metadata del POST (no en CSV)
- Idempotencia: hash+periodo+carteraId+sistemaOrigen
- Storage seguro en volumen Docker dedicado (importaciones_tmp)
- Async AFTER_COMMIT para workers de validación y procesamiento
- Recuperación automática de VALIDANDO/PROCESANDO huérfanos
- TTL de archivos VALIDADA
- Angular: /importacion, /importacion/nueva, /importacion/:id
- Permiso DATOS_IMPORTAR en V012
- UNIQUE (persona_id) WHERE activa=TRUE en carteras_personas

## Bloques de implementación

- [x] Recuperación y lectura de contexto
- [x] TASK_CURRENT.md actualizado
- [x] pom.xml — agregar Apache Commons CSV
- [x] V012 — migración Flyway
- [x] Módulo importacion — domain
- [x] Módulo importacion — aplicacion
- [x] Módulo importacion — infraestructura
- [x] Módulo importacion — web
- [x] Módulo carteras — endpoint listar activas
- [x] GlobalExceptionHandler — nuevas excepciones
- [x] application.yml — configuración storage/TTL/async
- [x] Angular — modelos
- [x] Angular — service
- [x] Angular — components
- [x] Angular — routes
- [x] Tests API — 404/404 — 2 corridas sin flakiness
- [x] Tests Angular — 148/148 — specs importacion creados (DT-IMX-002 resuelto)
- [x] Playwright — 14 escenarios importacion — 40/40 total
- [x] Docker — compose.yaml + volumen — 3 servicios healthy
- [x] Smoke tests — 49/49 OK — sección 8 importacion (DT-IMX-003 resuelto)
- [x] Fixture CSV
- [x] Documentación — STATUS/CHANGELOG/DEUDA_TECNICA actualizados
- [x] SESSION_HANDOFF.md actualizado

## No incluye en 5C

- XLSX
- Integración automática con API corporativa
- Jobs programados desde frontend
- Importación Android
- Importación de usuarios (Fase 5B-2)
- Avales (postergado)
- CRUD de carteras

## Fases anteriores — CERRADAS ✅

| Fase | Tag | Commit |
|---|---|---|
| 5B-2 Gestión administrativa usuarios (escritura) | `v0.16.0-usuarios-admin-write` | `b3c4c8a` |
| Entorno Docker local | `v0.15.0-entorno-docker-local` | `1a22c8a` |
| 5B-1 Consulta admin usuarios (solo lectura) | `v0.14.0-usuarios-admin-readonly` | `d82d95d` |
| 5A Base admin web + auth web | `v0.13.0-admin-base` | `71d47b2` |
| 4C-B Búsqueda directa | `v0.12.0-busqueda-directa` | `4cddf50` |
| 4C-A Gestiones offline | `v0.11.0-gestiones-offline` | `dec7b18` |
