-- V013: Contrato CSV definitivo — Fase 5D
-- Finaliza el diseño del contrato de importación mensual.
-- NO editar migraciones anteriores.

-- ============================================================
-- 1. CATÁLOGO DE CARTERAS: codigo_origen + siembra inicial
-- Las 4 carteras estándar del sistema origen.
-- ============================================================
ALTER TABLE cobranza.carteras
    ADD COLUMN IF NOT EXISTS codigo_origen VARCHAR(10);

CREATE UNIQUE INDEX IF NOT EXISTS uq_carteras_codigo_origen
    ON cobranza.carteras (codigo_origen)
    WHERE codigo_origen IS NOT NULL;

INSERT INTO cobranza.carteras (id, nombre, codigo_origen, activa, fecha_creacion, fecha_actualizacion)
VALUES
    ('00000000-0000-0000-0001-000000000001'::uuid, 'Temprana',        '1', TRUE, NOW(), NOW()),
    ('00000000-0000-0000-0001-000000000002'::uuid, 'Vigente',         '2', TRUE, NOW(), NOW()),
    ('00000000-0000-0000-0001-000000000003'::uuid, 'Vigente Judicial','3', TRUE, NOW(), NOW()),
    ('00000000-0000-0000-0001-000000000004'::uuid, 'Castigada',       '4', TRUE, NOW(), NOW())
ON CONFLICT DO NOTHING;

-- ============================================================
-- 2. CODIGO_EJECUTIVO_ORIGEN en usuarios
-- Identifica al ejecutivo del sistema origen (ej: '2127').
-- No es el nombre_usuario de la plataforma.
-- Permite resolver CODIGO_EJECUTIVO del CSV → usuario interno.
-- ============================================================
ALTER TABLE cobranza.usuarios
    ADD COLUMN IF NOT EXISTS codigo_ejecutivo_origen VARCHAR(50);

CREATE UNIQUE INDEX IF NOT EXISTS uq_usuarios_codigo_ejecutivo_origen
    ON cobranza.usuarios (codigo_ejecutivo_origen)
    WHERE codigo_ejecutivo_origen IS NOT NULL;

-- ============================================================
-- 3. MARCA_JUDICIAL en carteras_personas
-- Indica si el caso tiene marca judicial según el CSV de origen.
-- Independiente del codigo_cartera (no hay regla entre ambos).
-- ============================================================
ALTER TABLE cobranza.carteras_personas
    ADD COLUMN IF NOT EXISTS marca_judicial CHAR(1);

ALTER TABLE cobranza.carteras_personas
    ADD CONSTRAINT ck_cp_marca_judicial CHECK (marca_judicial IN ('S', 'N'));

-- ============================================================
-- 4. IMPORTACIONES_MENSUALES: cartera_id y periodo opcionales
-- En el contrato v2, estos valores provienen del CSV por fila,
-- no del request HTTP. El registro de importación los almacena
-- como metadatos opcionales.
-- ============================================================
ALTER TABLE cobranza.importaciones_mensuales
    ALTER COLUMN cartera_id DROP NOT NULL;

ALTER TABLE cobranza.importaciones_mensuales
    ALTER COLUMN periodo DROP NOT NULL;

ALTER TABLE cobranza.importaciones_mensuales
    DROP CONSTRAINT IF EXISTS ck_im_periodo;

ALTER TABLE cobranza.importaciones_mensuales
    ADD CONSTRAINT ck_im_periodo
        CHECK (periodo IS NULL OR periodo ~ '^\d{4}-(0[1-9]|1[0-2])$');

-- ============================================================
-- 5. CLAVE NATURAL DE OPERACIÓN: numero_operacion
-- La importación identifica operaciones por sistema_origen + numero_operacion.
-- No se usa identificador_externo del CSV.
-- ============================================================
CREATE UNIQUE INDEX IF NOT EXISTS uq_operaciones_numero
    ON cobranza.operaciones (sistema_origen, numero_operacion)
    WHERE numero_operacion IS NOT NULL AND btrim(numero_operacion) <> '';
