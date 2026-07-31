-- V006: Carteras, personas, avales y direcciones
-- Flyway es el propietario exclusivo de todos los objetos de base de datos.
-- La API es autoridad de carteras. Personas, avales y direcciones son copias operacionales del sistema externo.

-- ============================================================
-- CARTERAS
-- La API es autoridad de carteras: no provienen del sistema externo.
-- ============================================================
CREATE TABLE cobranza.carteras (
    id                  UUID         NOT NULL,
    nombre              VARCHAR(200) NOT NULL,
    descripcion         TEXT,
    activa              BOOLEAN      NOT NULL DEFAULT TRUE,
    fecha_creacion      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    fecha_actualizacion TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version             BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT pk_carteras                  PRIMARY KEY (id),
    CONSTRAINT ck_carteras_nombre_no_vacio  CHECK (btrim(nombre) <> '')
);

-- ============================================================
-- PERSONAS
-- Copia operacional del sistema externo.
-- Una persona puede pertenecer a como máximo una cartera activa.
-- ============================================================
CREATE TABLE cobranza.personas (
    id                          UUID         NOT NULL,
    rut_numero                  VARCHAR(8)   NOT NULL,
    rut_dv                      VARCHAR(1)   NOT NULL,
    nombre                      VARCHAR(300) NOT NULL,
    cartera_id                  UUID,
    codigo_externo              VARCHAR(100),
    sistema_origen              VARCHAR(50)  NOT NULL DEFAULT 'LEGADO',
    fecha_actualizacion_origen  TIMESTAMPTZ,
    fecha_importacion           TIMESTAMPTZ,
    fecha_creacion              TIMESTAMPTZ  NOT NULL DEFAULT now(),
    fecha_actualizacion         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version                     BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT pk_personas                  PRIMARY KEY (id),
    CONSTRAINT fk_personas_cartera          FOREIGN KEY (cartera_id)
        REFERENCES cobranza.carteras(id),
    CONSTRAINT uq_personas_rut              UNIQUE (rut_numero, rut_dv),
    CONSTRAINT ck_personas_rut_dv           CHECK (rut_dv IN ('0','1','2','3','4','5','6','7','8','9','K')),
    CONSTRAINT ck_personas_rut_numero       CHECK (rut_numero ~ '^[0-9]+$'),
    CONSTRAINT ck_personas_nombre_no_vacio  CHECK (btrim(nombre) <> '')
);

-- Búsqueda por parte numérica del RUT (sin DV)
CREATE INDEX idx_personas_rut_numero ON cobranza.personas (rut_numero);
CREATE INDEX idx_personas_cartera    ON cobranza.personas (cartera_id)
    WHERE cartera_id IS NOT NULL;

-- Unicidad del identificador externo por sistema de origen
CREATE UNIQUE INDEX uq_personas_codigo_externo
    ON cobranza.personas (sistema_origen, codigo_externo)
    WHERE codigo_externo IS NOT NULL;

-- ============================================================
-- AVALES
-- Datos de solo lectura. Inmutables una vez importados.
-- Asociados a la persona en el MVP (DP-08 pendiente no bloqueante).
-- ============================================================
CREATE TABLE cobranza.avales (
    id              UUID         NOT NULL,
    persona_id      UUID         NOT NULL,
    rut_numero      VARCHAR(8)   NOT NULL,
    rut_dv          VARCHAR(1)   NOT NULL,
    nombre          VARCHAR(300) NOT NULL,
    codigo_externo  VARCHAR(100),
    sistema_origen  VARCHAR(50)  NOT NULL DEFAULT 'LEGADO',
    fecha_creacion  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_avales                PRIMARY KEY (id),
    CONSTRAINT fk_avales_persona        FOREIGN KEY (persona_id)
        REFERENCES cobranza.personas(id),
    CONSTRAINT uq_avales_persona_rut    UNIQUE (persona_id, rut_numero, rut_dv),
    CONSTRAINT ck_avales_rut_dv         CHECK (rut_dv IN ('0','1','2','3','4','5','6','7','8','9','K')),
    CONSTRAINT ck_avales_rut_numero     CHECK (rut_numero ~ '^[0-9]+$'),
    CONSTRAINT ck_avales_nombre_no_vacio CHECK (btrim(nombre) <> '')
);

CREATE INDEX idx_avales_persona ON cobranza.avales (persona_id);

-- ============================================================
-- DIRECCIONES
-- Copia operacional del sistema externo. El contenido de dirección
-- no es modificado por la plataforma (RN-17). Los indicadores
-- es_principal y vigente son gestionados por la API.
-- ============================================================
CREATE TABLE cobranza.direcciones (
    id                         UUID         NOT NULL,
    persona_id                 UUID         NOT NULL,
    tipo                       VARCHAR(30)  NOT NULL DEFAULT 'DOMICILIO',
    texto                      TEXT         NOT NULL,
    comuna                     VARCHAR(100),
    ciudad                     VARCHAR(100),
    region                     VARCHAR(100),
    referencia                 TEXT,
    es_principal               BOOLEAN      NOT NULL DEFAULT FALSE,
    vigente                    BOOLEAN      NOT NULL DEFAULT TRUE,
    codigo_externo             VARCHAR(100),
    sistema_origen             VARCHAR(50)  NOT NULL DEFAULT 'LEGADO',
    fecha_actualizacion_origen TIMESTAMPTZ,
    fecha_creacion             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_direcciones               PRIMARY KEY (id),
    CONSTRAINT fk_direcciones_persona       FOREIGN KEY (persona_id)
        REFERENCES cobranza.personas(id),
    CONSTRAINT ck_direcciones_tipo          CHECK (tipo IN ('DOMICILIO','TRABAJO','COMERCIAL','OTRO')),
    CONSTRAINT ck_direcciones_texto_no_vacio CHECK (btrim(texto) <> '')
);

-- Una sola dirección principal activa por persona
CREATE UNIQUE INDEX uq_direcciones_principal_activa
    ON cobranza.direcciones (persona_id)
    WHERE es_principal = TRUE AND vigente = TRUE;

CREATE INDEX idx_direcciones_persona ON cobranza.direcciones (persona_id);
