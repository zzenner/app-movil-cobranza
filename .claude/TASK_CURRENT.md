# Tarea actual

## Identificación

- **Fase:** 5B — Listado de usuarios (solo lectura)
- **Estado:** EN PREPARACIÓN
- **Rama activa:** `feature/fase-5b-usuarios-admin`
- **Base funcional:** `71d47b2 feat: implementar base admin web y autenticacion fase 5a`
- **Tag de fase anterior:** `v0.13.0-admin-base`

## Objetivo

Implementar la primera funcionalidad administrativa real del panel web:
un listado de usuarios de **solo lectura** que consume el endpoint
`GET /api/v1/admin/usuarios` (a crear en la API).

## Alcance

### API (cambios)

- Endpoint: `GET /api/v1/admin/usuarios`
  - Requiere rol `ADMINISTRADOR`
  - Respuesta paginada: página, tamaño, total, items
  - Campos por usuario: `id`, `nombre`, `correo`, `rol`, `estado`, `fechaCreacion`
- Actualizar OpenAPI (`contracts/openapi/cobranza-api.yaml`)
- Pruebas de integración para el endpoint (autenticado, sin rol, sin sesión)

### Angular

- Módulo `features/usuarios`
  - Componente `UsuariosListadoComponent` (tabla Material con paginación)
  - Servicio `UsuariosService` (solo lectura)
  - Ruta `/usuarios` protegida con `authGuard` + `roleGuard(['ADMINISTRADOR'])`
- Enlace en sidenav para navegar al listado
- Pruebas unitarias del servicio y componente

## No incluye

- Crear usuario
- Editar usuario
- Eliminar usuario
- Cambiar contraseña
- Carteras, asignaciones ni gestiones

## Fases anteriores — CERRADAS ✅

| Fase | Tag | Commit |
|---|---|---|
| 5A Base admin web + auth web | `v0.13.0-admin-base` | `71d47b2` |
| 4C-B Búsqueda directa | `v0.12.0-busqueda-directa` | `4cddf50` |
| 4C-A Gestiones offline | `v0.11.0-gestiones-offline` | `dec7b18` |
| 4B Cartera offline | `v0.10.0-descarga-offline` | (ver ROADMAP) |
