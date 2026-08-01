-- V008: Permitir personas en múltiples carteras
-- Reemplaza personas.cartera_id por la tabla de relación carteras_personas.
-- Una persona puede pertenecer a una o más carteras activas (RN-03 revisada).
-- Flyway es el propietario exclusivo de todos los objetos de base de datos.

-- ============================================================
-- CARTERAS_PERSONAS
-- Relación N:M entre personas y carteras con historial de altas y bajas.
-- UUID generado por Java en ejecución normal.
-- En esta migración se usa gen_random_uuid() como excepción de transición.
-- ============================================================
CREATE TABLE cobranza.carteras_personas (
    id                  UUID         NOT NULL,
    cartera_id          UUID         NOT NULL,
    persona_id          UUID         NOT NULL,
    activa              BOOLEAN      NOT NULL DEFAULT TRUE,
    fecha_inicio        DATE         NOT NULL,
    fecha_fin           DATE,
    fecha_creacion      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    fecha_actualizacion TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version             BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT pk_carteras_personas         PRIMARY KEY (id),
    CONSTRAINT fk_cp_cartera                FOREIGN KEY (cartera_id)
        REFERENCES cobranza.carteras(id),
    CONSTRAINT fk_cp_persona                FOREIGN KEY (persona_id)
        REFERENCES cobranza.personas(id),
    CONSTRAINT ck_cp_coherencia_activa      CHECK (
        (activa = TRUE  AND fecha_fin IS NULL)
        OR
        (activa = FALSE AND fecha_fin IS NOT NULL)
    ),
    CONSTRAINT ck_cp_fechas                 CHECK (
        fecha_fin IS NULL OR fecha_fin >= fecha_inicio
    )
);

-- Solo un vínculo activo por par (cartera, persona)
CREATE UNIQUE INDEX uq_cp_activa
    ON cobranza.carteras_personas (cartera_id, persona_id)
    WHERE activa = TRUE;

CREATE INDEX idx_cp_cartera ON cobranza.carteras_personas (cartera_id);
CREATE INDEX idx_cp_persona  ON cobranza.carteras_personas (persona_id);

-- ============================================================
-- MIGRACIÓN DE DATOS
-- Copia vínculos existentes desde personas.cartera_id a carteras_personas.
-- gen_random_uuid() se usa aquí como excepción de transición.
-- ============================================================
INSERT INTO cobranza.carteras_personas
    (id, cartera_id, persona_id, activa, fecha_inicio, fecha_creacion, fecha_actualizacion, version)
SELECT
    gen_random_uuid(),
    p.cartera_id,
    p.id,
    TRUE,
    CURRENT_DATE,
    now(),
    now(),
    0
FROM cobranza.personas p
WHERE p.cartera_id IS NOT NULL;

-- ============================================================
-- VERIFICACIÓN DE INTEGRIDAD
-- ============================================================
DO $$
DECLARE
    v_personas_con_cartera  INTEGER;
    v_vinculos_migrados     INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_personas_con_cartera
    FROM cobranza.personas
    WHERE cartera_id IS NOT NULL;

    SELECT COUNT(*) INTO v_vinculos_migrados
    FROM cobranza.carteras_personas;

    IF v_personas_con_cartera <> v_vinculos_migrados THEN
        RAISE EXCEPTION
            'Migración incompleta: % personas con cartera_id, % vínculos migrados',
            v_personas_con_cartera, v_vinculos_migrados;
    END IF;
END $$;

-- ============================================================
-- ELIMINAR RELACIÓN DIRECTA personas.cartera_id
-- La columna ya fue reemplazada por carteras_personas.
-- ============================================================
ALTER TABLE cobranza.personas DROP CONSTRAINT IF EXISTS fk_personas_cartera;
DROP INDEX IF EXISTS cobranza.idx_personas_cartera;
ALTER TABLE cobranza.personas DROP COLUMN IF EXISTS cartera_id;
