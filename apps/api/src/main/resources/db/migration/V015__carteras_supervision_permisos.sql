-- V015: Fase 6A — Permisos para carteras y supervisión administrativa
-- NO modificar migraciones anteriores.

-- ============================================================
-- 1. NUEVOS PERMISOS
-- ============================================================
-- V012 usó el UUID ...000000000008 para DATOS_IMPORTAR; continuamos desde 009.
INSERT INTO cobranza.permisos (id, codigo, nombre, descripcion) VALUES
    ('a1b2c3d4-0002-0002-0002-000000000009',
     'CARTERAS_VER',
     'Ver catálogo de carteras',
     'Consultar el catálogo de carteras disponibles en el sistema.'),
    ('a1b2c3d4-0002-0002-0002-000000000010',
     'SUPERVISION_VER',
     'Ver supervisión',
     'Consultar supervisores, ejecutivos y relaciones de supervisión.'),
    ('a1b2c3d4-0002-0002-0002-000000000011',
     'SUPERVISION_ADMINISTRAR',
     'Administrar supervisión',
     'Asignar, reasignar y finalizar relaciones supervisor–ejecutivo; configurar código externo del ejecutivo.');

-- ============================================================
-- 2. ASIGNACIÓN A ROLES
-- JEFE_SUPERVISORES y TECNOLOGIA: los tres permisos nuevos
-- SUPERVISOR: ver (no administrar)
-- EJECUTIVO_TERRENO: sin acceso administrativo
-- ============================================================

INSERT INTO cobranza.rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM cobranza.roles r, cobranza.permisos p
WHERE r.codigo IN ('JEFE_SUPERVISORES', 'TECNOLOGIA')
  AND p.codigo IN ('CARTERAS_VER', 'SUPERVISION_VER', 'SUPERVISION_ADMINISTRAR')
ON CONFLICT DO NOTHING;

INSERT INTO cobranza.rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM cobranza.roles r, cobranza.permisos p
WHERE r.codigo = 'SUPERVISOR'
  AND p.codigo IN ('CARTERAS_VER', 'SUPERVISION_VER')
ON CONFLICT DO NOTHING;
