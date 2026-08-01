-- V009: Asignaciones mensuales y diarias de personas a ejecutivos (Fase 3B)
-- Corrección: asignaciones_mensuales_personas usa UUID PK para permitir historial
-- individual por persona. FK compuesta garantiza coherencia de cartera_id en BD.
-- UUID generado por Java. No se usa DEFAULT gen_random_uuid() en columnas de entidad.

-- ============================================================
-- ASIGNACIONES_MENSUALES
-- Conjunto de personas asignadas a un ejecutivo por mes y cartera.
-- ============================================================
CREATE TABLE cobranza.asignaciones_mensuales (
    id                  UUID         NOT NULL,
    cartera_id          UUID         NOT NULL,
    ejecutivo_id        UUID         NOT NULL,
    supervisor_id       UUID         NOT NULL,
    fecha_inicio        DATE         NOT NULL,
    fecha_fin           DATE         NOT NULL,
    activa              BOOLEAN      NOT NULL DEFAULT TRUE,
    observacion         TEXT,
    fecha_creacion      TIMESTAMPTZ  NOT NULL,
    fecha_actualizacion TIMESTAMPTZ  NOT NULL,
    version             BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT pk_asignaciones_mensuales    PRIMARY KEY (id),
    CONSTRAINT fk_am_cartera                FOREIGN KEY (cartera_id)
        REFERENCES cobranza.carteras(id),
    CONSTRAINT ck_am_fechas                 CHECK (fecha_fin >= fecha_inicio),
    -- Necesario para que la FK compuesta desde asignaciones_mensuales_personas sea válida
    CONSTRAINT uq_am_id_cartera             UNIQUE (id, cartera_id)
);

-- Un ejecutivo tiene como máximo una AsignacionMensual activa por cartera
CREATE UNIQUE INDEX uq_am_ejecutivo_cartera_activa
    ON cobranza.asignaciones_mensuales (ejecutivo_id, cartera_id)
    WHERE activa = TRUE;

CREATE INDEX idx_am_cartera    ON cobranza.asignaciones_mensuales (cartera_id);
CREATE INDEX idx_am_ejecutivo  ON cobranza.asignaciones_mensuales (ejecutivo_id);

-- ============================================================
-- ASIGNACIONES_MENSUALES_PERSONAS
-- Historial individual de vínculos persona–asignación mensual.
-- PK UUID para permitir múltiples filas históricas por (asignacion_mensual_id, persona_id).
-- cartera_id denormalizada + FK compuesta garantizan coherencia con la mensual padre en BD.
-- ============================================================
CREATE TABLE cobranza.asignaciones_mensuales_personas (
    id                    UUID         NOT NULL,
    asignacion_mensual_id UUID         NOT NULL,
    persona_id            UUID         NOT NULL,
    cartera_id            UUID         NOT NULL,
    activa                BOOLEAN      NOT NULL DEFAULT TRUE,
    fecha_inicio          DATE         NOT NULL,
    fecha_fin             DATE,
    fecha_creacion        TIMESTAMPTZ  NOT NULL,
    fecha_actualizacion   TIMESTAMPTZ  NOT NULL,
    version               BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT pk_asignaciones_mensuales_personas   PRIMARY KEY (id),
    -- FK simple garantiza que la mensual existe
    CONSTRAINT fk_amp_asignacion                    FOREIGN KEY (asignacion_mensual_id)
        REFERENCES cobranza.asignaciones_mensuales(id),
    -- FK compuesta garantiza que cartera_id coincide con la de la mensual padre (BD-enforced)
    CONSTRAINT fk_amp_am_cartera                    FOREIGN KEY (asignacion_mensual_id, cartera_id)
        REFERENCES cobranza.asignaciones_mensuales (id, cartera_id),
    CONSTRAINT fk_amp_persona                       FOREIGN KEY (persona_id)
        REFERENCES cobranza.personas(id),
    -- activa=TRUE implica fecha_fin NULL; activa=FALSE implica fecha_fin informada
    CONSTRAINT ck_amp_coherencia_activa             CHECK (
        (activa = TRUE  AND fecha_fin IS NULL)
        OR
        (activa = FALSE AND fecha_fin IS NOT NULL)
    ),
    CONSTRAINT ck_amp_fechas                        CHECK (
        fecha_fin IS NULL OR fecha_fin >= fecha_inicio
    )
);

-- Una persona puede tener como máximo un ejecutivo responsable activo por cartera (RN-04).
-- Permite múltiples filas históricas (activa=FALSE) para el mismo (persona_id, cartera_id).
CREATE UNIQUE INDEX uq_amp_persona_cartera_activa
    ON cobranza.asignaciones_mensuales_personas (persona_id, cartera_id)
    WHERE activa = TRUE;

CREATE INDEX idx_amp_mensual ON cobranza.asignaciones_mensuales_personas (asignacion_mensual_id);
CREATE INDEX idx_amp_persona  ON cobranza.asignaciones_mensuales_personas (persona_id);

-- ============================================================
-- ASIGNACIONES_DIARIAS
-- Subconjunto diario preparado por el supervisor para el ejecutivo.
-- Estados: BORRADOR, PUBLICADA, FINALIZADA, CANCELADA.
-- ============================================================
CREATE TABLE cobranza.asignaciones_diarias (
    id                    UUID         NOT NULL,
    asignacion_mensual_id UUID         NOT NULL,
    ejecutivo_id          UUID         NOT NULL,
    supervisor_id         UUID         NOT NULL,
    fecha                 DATE         NOT NULL,
    estado                VARCHAR(20)  NOT NULL DEFAULT 'BORRADOR',
    fecha_publicacion     TIMESTAMPTZ,
    motivo_cancelacion    TEXT,
    fecha_creacion        TIMESTAMPTZ  NOT NULL,
    fecha_actualizacion   TIMESTAMPTZ  NOT NULL,
    version               BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT pk_asignaciones_diarias      PRIMARY KEY (id),
    CONSTRAINT fk_ad_mensual                FOREIGN KEY (asignacion_mensual_id)
        REFERENCES cobranza.asignaciones_mensuales(id),
    CONSTRAINT ck_ad_estado                 CHECK (estado IN ('BORRADOR','PUBLICADA','FINALIZADA','CANCELADA')),
    CONSTRAINT ck_ad_publicada              CHECK (
        (estado = 'PUBLICADA'  AND fecha_publicacion IS NOT NULL)
        OR estado <> 'PUBLICADA'
    ),
    CONSTRAINT ck_ad_cancelada              CHECK (
        (estado = 'CANCELADA'  AND motivo_cancelacion IS NOT NULL AND motivo_cancelacion <> '')
        OR estado <> 'CANCELADA'
    )
);

-- Un ejecutivo tiene como máximo una diaria en BORRADOR o PUBLICADA por fecha
CREATE UNIQUE INDEX uq_ad_ejecutivo_fecha_activa
    ON cobranza.asignaciones_diarias (ejecutivo_id, fecha)
    WHERE estado IN ('BORRADOR','PUBLICADA');

CREATE INDEX idx_ad_mensual   ON cobranza.asignaciones_diarias (asignacion_mensual_id);
CREATE INDEX idx_ad_ejecutivo ON cobranza.asignaciones_diarias (ejecutivo_id);

-- ============================================================
-- ASIGNACIONES_DIARIAS_PERSONAS
-- Personas incluidas en una asignación diaria.
-- ============================================================
CREATE TABLE cobranza.asignaciones_diarias_personas (
    asignacion_diaria_id  UUID         NOT NULL,
    persona_id            UUID         NOT NULL,
    fecha_creacion        TIMESTAMPTZ  NOT NULL,
    CONSTRAINT pk_asignaciones_diarias_personas  PRIMARY KEY (asignacion_diaria_id, persona_id),
    CONSTRAINT fk_adp_asignacion                 FOREIGN KEY (asignacion_diaria_id)
        REFERENCES cobranza.asignaciones_diarias(id),
    CONSTRAINT fk_adp_persona                    FOREIGN KEY (persona_id)
        REFERENCES cobranza.personas(id)
);

CREATE INDEX idx_adp_diaria  ON cobranza.asignaciones_diarias_personas (asignacion_diaria_id);
CREATE INDEX idx_adp_persona ON cobranza.asignaciones_diarias_personas (persona_id);
