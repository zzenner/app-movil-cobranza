-- V003: Catálogos iniciales de roles y permisos
-- UUIDs explícitos y estables: no cambiarán en migraciones posteriores.
-- Prefijo 'a1b2c3d4-0001-...' = roles; 'a1b2c3d4-0002-...' = permisos.

-- ============================================================
-- ROLES
-- ============================================================
INSERT INTO cobranza.roles (id, codigo, nombre, descripcion) VALUES
    ('a1b2c3d4-0001-0001-0001-000000000001',
     'JEFE_SUPERVISORES',
     'Jefe de Supervisores',
     'Administra supervisores y tiene visibilidad completa del sistema.'),
    ('a1b2c3d4-0001-0001-0001-000000000002',
     'TECNOLOGIA',
     'Tecnología',
     'Administra usuarios, dispositivos y configuración del sistema.'),
    ('a1b2c3d4-0001-0001-0001-000000000003',
     'SUPERVISOR',
     'Supervisor',
     'Crea y publica asignaciones diarias para ejecutivos de terreno.'),
    ('a1b2c3d4-0001-0001-0001-000000000004',
     'EJECUTIVO_TERRENO',
     'Ejecutivo de Terreno',
     'Registra gestiones de cobranza desde dispositivos móviles.');

-- ============================================================
-- PERMISOS
-- ============================================================
INSERT INTO cobranza.permisos (id, codigo, nombre, descripcion) VALUES
    ('a1b2c3d4-0002-0002-0002-000000000001',
     'USUARIOS_VER',
     'Ver usuarios',
     'Consultar el listado y detalle de usuarios del sistema.'),
    ('a1b2c3d4-0002-0002-0002-000000000002',
     'USUARIOS_ADMINISTRAR',
     'Administrar usuarios',
     'Crear, editar y desactivar usuarios del sistema.'),
    ('a1b2c3d4-0002-0002-0002-000000000003',
     'ASIGNACIONES_VER',
     'Ver asignaciones',
     'Consultar asignaciones mensuales y diarias.'),
    ('a1b2c3d4-0002-0002-0002-000000000004',
     'ASIGNACIONES_ADMINISTRAR',
     'Administrar asignaciones',
     'Crear, publicar y cancelar asignaciones diarias.'),
    ('a1b2c3d4-0002-0002-0002-000000000005',
     'GESTIONES_VER',
     'Ver gestiones',
     'Consultar gestiones registradas por ejecutivos de terreno.'),
    ('a1b2c3d4-0002-0002-0002-000000000006',
     'GESTIONES_CREAR',
     'Registrar gestiones',
     'Registrar gestiones de cobranza (uso de la API móvil).'),
    ('a1b2c3d4-0002-0002-0002-000000000007',
     'SINCRONIZACION_VER',
     'Ver sincronización',
     'Consultar el estado de sincronización de dispositivos.');

-- ============================================================
-- MATRIZ ROL-PERMISO
-- ============================================================

-- JEFE_SUPERVISORES: todos los permisos
INSERT INTO cobranza.rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM cobranza.roles r, cobranza.permisos p
WHERE r.codigo = 'JEFE_SUPERVISORES';

-- TECNOLOGIA: gestión de usuarios y sincronización
INSERT INTO cobranza.rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM cobranza.roles r, cobranza.permisos p
WHERE r.codigo = 'TECNOLOGIA'
  AND p.codigo IN ('USUARIOS_VER', 'USUARIOS_ADMINISTRAR', 'SINCRONIZACION_VER');

-- SUPERVISOR: asignaciones y consulta de gestiones
INSERT INTO cobranza.rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM cobranza.roles r, cobranza.permisos p
WHERE r.codigo = 'SUPERVISOR'
  AND p.codigo IN ('ASIGNACIONES_VER', 'ASIGNACIONES_ADMINISTRAR', 'GESTIONES_VER');

-- EJECUTIVO_TERRENO: registrar gestiones
INSERT INTO cobranza.rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM cobranza.roles r, cobranza.permisos p
WHERE r.codigo = 'EJECUTIVO_TERRENO'
  AND p.codigo IN ('GESTIONES_CREAR');
