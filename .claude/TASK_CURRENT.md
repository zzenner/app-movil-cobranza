# Tarea actual

## Identificación

- **Fase:** 5B-1 — Consulta administrativa de usuarios (solo lectura)
- **Estado:** COMPLETADA ✅
- **Rama activa:** `feature/fase-5b-usuarios-admin`
- **Base funcional:** `71d47b2 feat: implementar base admin web y autenticacion fase 5a`
- **Tag de fase anterior:** `v0.13.0-admin-base`

## Objetivo

Implementar listado y detalle de usuarios en modo solo lectura para el panel administrativo.
Acceso únicamente mediante permiso `USUARIOS_VER`.

## Decisión de autorización

- **JEFE_SUPERVISORES:** acceso (posee `USUARIOS_VER`)
- **TECNOLOGIA:** acceso (posee `USUARIOS_VER`)
- **SUPERVISOR:** sin acceso
- **EJECUTIVO_TERRENO:** sin acceso

La API usa la autoridad `PERM_USUARIOS_VER` (GrantedAuthority del JWT).
Angular controla menú y rutas por permiso `USUARIOS_VER`, no por nombre de rol.

## Alcance Fase 5B-1

### API (cambios)

- `GET /api/v1/admin/usuarios` — listado paginado con filtros
- `GET /api/v1/admin/usuarios/{id}` — detalle de usuario
- Requiere permiso `USUARIOS_VER` (autoridad `PERM_USUARIOS_VER`)
- Estado calculado: ACTIVO / BLOQUEADO_TEMPORAL / BLOQUEADO / INACTIVO
- Prevención N+1 mediante batch queries
- Orden fijo: nombreUsuario ASC, id ASC
- Actualizar OpenAPI (`contracts/openapi/cobranza-api.yaml`)

### Angular (cambios)

- `permissionGuard` funcional basado en permiso
- Feature `features/usuarios/`:
  - `UsuariosListComponent` (tabla Material, filtros, paginación)
  - `UsuarioDetailComponent` (detalle, solo lectura)
  - `UsuariosService`
  - Modelos TypeScript
  - Rutas lazy-loaded
- Menú "Usuarios" visible solo con `USUARIOS_VER`
- Ruta `/usuarios` protegida por `permissionGuard`

## No incluye en 5B-1

- Crear / Editar / Activar / Desactivar / Bloquear / Desbloquear usuario
- Cambiar contraseña / Asignar roles / Asignar supervisor / Eliminar
- Carteras, asignaciones
- Operaciones de escritura de cualquier tipo

## Fase 5B-2 — PENDIENTE

Operaciones de escritura: creación, edición, bloqueo/desbloqueo, cambio de contraseña,
asignación de roles y supervisor. Requiere permiso `USUARIOS_ADMINISTRAR`.

## Fases anteriores — CERRADAS ✅

| Fase | Tag | Commit |
|---|---|---|
| 5A Base admin web + auth web | `v0.13.0-admin-base` | `71d47b2` |
| 4C-B Búsqueda directa | `v0.12.0-busqueda-directa` | `4cddf50` |
| 4C-A Gestiones offline | `v0.11.0-gestiones-offline` | `dec7b18` |
| 4B Cartera offline | `v0.10.0-descarga-offline` | (ver ROADMAP) |
