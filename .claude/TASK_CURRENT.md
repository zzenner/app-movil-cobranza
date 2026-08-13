# Tarea actual

## Identificación

- **Fase:** 5D — Contrato CSV definitivo de importación mensual
- **Estado:** CERRADA ✅ — COMMIT LOCAL 9133f49 (pendiente push con autorización)
- **Rama activa:** `feature/fase-5c-importacion-mensual`
- **Base funcional:** `b3c4c8a feat: implementar gestion administrativa de usuarios fase 5b-2`
- **Tag de fase anterior:** `v0.16.0-usuarios-admin-write`

## Objetivo

Cerrar el contrato CSV de importación mensual con el formato definitivo del sistema origen:
26 columnas, UTF-8, fechas YYYY-MM-DD, PERIODO como columna, CODIGO_CARTERA resuelto por catálogo, CODIGO_EJECUTIVO resuelto por código de origen.

## Alcance Fase 5D (cierre definitivo)

- CSV de 26 columnas (contrato v2)
- Encoding UTF-8 estricto (rechazo de no-UTF-8)
- Fechas YYYY-MM-DD
- PERIODO como columna por fila (YYYY-MM)
- CODIGO_CARTERA resolución por catálogo (1-4, no auto-creación)
- CODIGO_EJECUTIVO resolución por `codigo_ejecutivo_origen` en usuarios
- MARCA_JUDICIAL por fila (S o N)
- CUOTA_ESTADO requerido
- Filas vacías ignoradas silenciosamente
- Clave posición: PERIODO+RUT+OP+CUOTA+CARTERA
- V013 completo (carteras, ejecutivos, marca_judicial, nullable)

## Bloques de implementación

- [x] V013 — migración Flyway completa
- [x] FilaCsv.java — 26 columnas
- [x] CsvImportacionParser.java — UTF-8, YYYY-MM-DD, PERIODO, CARTERAS_VALIDAS
- [x] ValidadorIntraArchivo.java — clave posición/ejecutivo nueva
- [x] ImportacionMensual.java — carteraId/periodo nullable
- [x] ImportacionMensualRepository.java — queries simplificadas
- [x] ImportacionService.java — sin carteraId/periodo en endpoint
- [x] ImportacionPersistenciaService.java — resolverCarteras/Ejecutivos, upsertCarteraPersona con marcaJudicial
- [x] ImportacionProcesamientoWorker.java — firma actualizada
- [x] ImportacionValidacionWorker.java — búsqueda por sistemaOrigen
- [x] ImportacionAdminController.java — endpoint v2
- [x] ImportacionAdminRestTest.java — subirCsv sin carteraId/periodo, código_ejecutivo_origen, Order(91)
- [x] CsvImportacionParserTest.java — 23 tests
- [x] ValidadorIntraArchivoTest.java — 8 tests
- [x] Fixtures CSV — 26 columnas, UTF-8, YYYY-MM-DD
- [x] importacion_valida_2026-09.csv — nuevo fixture
- [x] not_a_csv.txt — nuevo fixture
- [x] Documentación — FORMATO_IMPORTACION_MENSUAL.md reescrito
- [x] STATUS.md / CHANGELOG.md actualizados
- [x] SESSION_HANDOFF.md actualizado
- [x] **Commit final** `9133f49 feat(importacion): cerrar contrato csv definitivo fase 5d`
- [x] Angular contrato v2: `crear()` sin carteraId/periodo, UI sin selector ni período
- [x] OpenAPI: 5 endpoints importacion + 7 schemas documentados
- [ ] **Push** (requiere autorización explícita)

## No incluye en 5D

- Cambios en Angular/Admin Web (el endpoint es el mismo URL, solo cambia el cuerpo)
- XLSX
- Auto-creación de ejecutivos o carteras

## Pendiente para el commit

1. Activar Docker Desktop con integración WSL2
2. `cd apps/api && ./mvnw clean verify` — debe pasar 100% incluyendo Testcontainers
3. Commit con autorización explícita: `feat(importacion): cerrar contrato csv definitivo fase 5d`
4. Tag: `v0.17.0-importacion-mensual`

## Fases anteriores — CERRADAS ✅

| Fase | Tag | Commit |
|---|---|---|
| 5B-2 Gestión administrativa usuarios (escritura) | `v0.16.0-usuarios-admin-write` | `b3c4c8a` |
| Entorno Docker local | `v0.15.0-entorno-docker-local` | `1a22c8a` |
| 5B-1 Consulta admin usuarios (solo lectura) | `v0.14.0-usuarios-admin-readonly` | `d82d95d` |
| 5A Base admin web + auth web | `v0.13.0-admin-base` | `71d47b2` |
| 4C-B Búsqueda directa | `v0.12.0-busqueda-directa` | `4cddf50` |
| 4C-A Gestiones offline | `v0.11.0-gestiones-offline` | `dec7b18` |
