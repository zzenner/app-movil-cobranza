# Tarea actual

## Identificación

- **Fase:** 5B-2 — Gestión administrativa de usuarios (escritura)
- **Estado:** PENDIENTE — En progreso: entorno Docker local (tarea previa)
- **Rama activa:** `feature/fase-5b-2-usuarios-escritura`
- **Base funcional:** `d82d95d feat: implementar consulta administrativa de usuarios fase 5b-1`
- **Tag de fase anterior:** `v0.14.0-usuarios-admin-readonly`

## Objetivo

Implementar operaciones de escritura sobre usuarios desde el panel administrativo.
Acceso mediante permiso `USUARIOS_ADMINISTRAR`.

## Decisión de autorización

- **JEFE_SUPERVISORES:** acceso (posee todos los permisos)
- **TECNOLOGIA:** acceso (posee `USUARIOS_ADMINISTRAR`)
- **SUPERVISOR:** sin acceso
- **EJECUTIVO_TERRENO:** sin acceso

La API usará la autoridad `PERM_USUARIOS_ADMINISTRAR`.
Angular protegerá formularios y botones por permiso `USUARIOS_ADMINISTRAR`.

## Alcance Fase 5B-2

### API

- `POST /api/v1/admin/usuarios` — crear usuario
- `PUT /api/v1/admin/usuarios/{id}` — editar datos básicos (nombres, correo)
- `POST /api/v1/admin/usuarios/{id}/activar` — activar usuario inactivo
- `POST /api/v1/admin/usuarios/{id}/desactivar` — desactivar usuario
- `POST /api/v1/admin/usuarios/{id}/desbloquear` — desbloquear (manual)
- `POST /api/v1/admin/usuarios/{id}/contrasena` — cambio administrativo de contraseña
- Permiso requerido: `USUARIOS_ADMINISTRAR`
- Actualizar OpenAPI

### Angular

- Formulario de creación de usuario
- Formulario de edición (datos básicos)
- Botones de acción: activar/desactivar, desbloquear, cambiar contraseña
- Protección por `permissionGuard` con `USUARIOS_ADMINISTRAR`

## No incluye en 5B-2

- Asignación de roles a usuario
- Asignación de supervisor
- Eliminación física de usuarios
- Carteras o asignaciones

## Siguiente acción exacta

"Revisar las reglas de creación, actualización, estado y contraseña de usuarios para planificar Fase 5B-2".

Ver:
- `docs/dominio/REGLAS_NEGOCIO.md` — RN-06 (roles), RN-28 (estado calculado)
- `apps/api/src/main/java/cl/zzenner/cobranza/usuarios/aplicacion/UsuarioService.java`
- `apps/api/src/main/java/cl/zzenner/cobranza/usuarios/dominio/Usuario.java`

## Fases anteriores — CERRADAS ✅

| Fase | Tag | Commit |
|---|---|---|
| 5B-1 Consulta admin usuarios (solo lectura) | `v0.14.0-usuarios-admin-readonly` | `d82d95d` |
| 5A Base admin web + auth web | `v0.13.0-admin-base` | `71d47b2` |
| 4C-B Búsqueda directa | `v0.12.0-busqueda-directa` | `4cddf50` |
| 4C-A Gestiones offline | `v0.11.0-gestiones-offline` | `dec7b18` |
| 4B Cartera offline | `v0.10.0-descarga-offline` | (ver ROADMAP) |
