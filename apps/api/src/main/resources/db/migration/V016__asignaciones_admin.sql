-- V016: Fase 6B — Asignaciones diarias administrativas
-- Agrega publicado_por_id a asignaciones_diarias y acceso de TECNOLOGIA a asignaciones.
-- NO modifica migraciones anteriores. ASIGNACIONES_VER y ASIGNACIONES_ADMINISTRAR ya
-- existen en V003 (IDs 000000000003 y 000000000004) y están asignados a JEFE_SUPERVISORES
-- y SUPERVISOR.

-- ============================================================
-- 1. CAMPO DE AUDITORÍA EN ASIGNACIONES_DIARIAS
-- registra qué usuario realizó la publicación
-- ============================================================
ALTER TABLE cobranza.asignaciones_diarias
    ADD COLUMN publicado_por_id UUID
        REFERENCES cobranza.usuarios(id);

-- ============================================================
-- 2. TECNOLOGIA obtiene acceso de solo lectura y administración
--    de asignaciones (para soporte y demo)
-- ============================================================
INSERT INTO cobranza.rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM cobranza.roles r, cobranza.permisos p
WHERE r.codigo = 'TECNOLOGIA'
  AND p.codigo IN ('ASIGNACIONES_VER', 'ASIGNACIONES_ADMINISTRAR')
ON CONFLICT DO NOTHING;
