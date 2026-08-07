# Tarea actual

## Identificación

- **Fase:** 5B-2 — Gestión administrativa de usuarios (escritura)
- **Estado:** VALIDADA ✅ — LISTA PARA CIERRE
- **Rama activa:** `feature/fase-5b-2-usuarios-escritura`
- **Base funcional:** `1a22c8a feat(infra): implementar entorno docker local integrado`
- **Tag de fase anterior:** `v0.15.0-entorno-docker-local`

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

Fase 5B-2 VALIDADA. Candidatos para siguiente fase:
- Fase 5B-3: Gestión de roles post-creación (asignar/revocar roles a usuario existente)
- Fase 5C: Importación mensual administrativa de datos (personas, operaciones, cuotas, asignaciones — CSV)
- Fase 5D: Gestión de carteras y asignaciones en Admin Web

Hacer commit en rama `feature/fase-5b-2-usuarios-escritura` y merge a `main` antes de iniciar la siguiente fase.

## Fases anteriores — CERRADAS ✅

| Fase | Tag | Commit |
|---|---|---|
| Entorno Docker local | `v0.15.0-entorno-docker-local` | `1a22c8a` |
| 5B-1 Consulta admin usuarios (solo lectura) | `v0.14.0-usuarios-admin-readonly` | `d82d95d` |
| 5A Base admin web + auth web | `v0.13.0-admin-base` | `71d47b2` |
| 4C-B Búsqueda directa | `v0.12.0-busqueda-directa` | `4cddf50` |
| 4C-A Gestiones offline | `v0.11.0-gestiones-offline` | `dec7b18` |
