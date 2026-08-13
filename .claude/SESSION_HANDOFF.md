# Estado de sesión — Fase 6A CERRADA

**Fecha:** 2026-08-13
**Rama:** main (merge desde feature/fase-6a-carteras-supervision)
**Estado:** COMPLETADA Y CERRADA ✅ — commit 6af58ca, tag v0.20.0-carteras-supervision

## Resumen de lo implementado

### Backend (API Java/Spring Boot)
- V015 — 3 nuevos permisos: CARTERAS_VER, SUPERVISION_VER, SUPERVISION_ADMINISTRAR
- CarteraAdminController: nuevo GET /api/v1/admin/carteras con PERM_CARTERAS_VER
- SupervisionAdminController: 5 endpoints REST de supervisión
- SupervisionAdminQueryService: consultas con filtros ejecutivos/supervisores
- SupervisionService: reescritura con reasignar atómico (saveAndFlush)
- CodigoEjecutivoDuplicadoException + codigoEjecutivoOrigen en Usuario
- 461 tests, 0 fallos

### Angular (Admin Web)
- /carteras: lista de las 4 carteras del catálogo
- /supervision: lista ejecutivos con supervisor, diálogos asignar/reasignar y código
- Menú lateral: items Carteras (CARTERAS_VER) y Supervisión (SUPERVISION_VER)
- 176 tests, 0 fallos

## Estado del repositorio

- Rama: main
- HEAD: 6af58ca
- Tag: v0.20.0-carteras-supervision (pendiente push a origin)
- origin/main: v0.19.0 (a796fde) — pendiente push

## Siguiente acción exacta

Para publicar en el repositorio remoto (con autorización explícita del usuario):
```bash
git push origin main
git push origin v0.20.0-carteras-supervision
```

## Próxima fase recomendada

**Fase 6B — Asignaciones diarias desde supervisión**
- Asignar carteras a ejecutivos por mes
- Distribuir personas/operaciones a ejecutivos diariamente
- Requiere UI en Admin Web y sincronización con app Android

Alternativa: **Fase 6C — Supervisión en app Android**
- Ver lista de ejecutivos a cargo
- Ver estado de gestiones por ejecutivo
