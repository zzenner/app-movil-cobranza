-- V010: Gestiones de cobranza (Fase 3C)
-- Gestiones inmutables. UUID generado en dispositivo. Dos orígenes: ASIGNACION_DIARIA | BUSQUEDA_DIRECTA.
-- No se incluye fotografias_gestion (diferido a Fase 3D).
-- No hay fecha_actualizacion ni version: la tabla es append-only.

CREATE TABLE cobranza.gestiones (
    id                      UUID             NOT NULL,
    origen_gestion          VARCHAR(30)      NOT NULL,
    asignacion_diaria_id    UUID,                           -- NULL para BUSQUEDA_DIRECTA
    persona_id              UUID             NOT NULL,
    ejecutivo_id            UUID             NOT NULL,
    tipo_gestion            VARCHAR(30)      NOT NULL,
    fecha_gestion           TIMESTAMPTZ      NOT NULL,      -- timestamp del dispositivo
    observacion             TEXT,
    observacion_direccion   TEXT,
    latitud                 DOUBLE PRECISION NOT NULL,
    longitud                DOUBLE PRECISION NOT NULL,
    precision_metros        REAL             NOT NULL,
    proveedor_gps           VARCHAR(50),
    ubicacion_simulada      BOOLEAN          NOT NULL,      -- SIN DEFAULT: el dispositivo lo informa explícitamente
    fecha_captura_gps       TIMESTAMPTZ      NOT NULL,
    fecha_compromiso        DATE,
    fecha_creacion_servidor TIMESTAMPTZ      NOT NULL,      -- timestamp de recepción en el servidor

    CONSTRAINT pk_gestiones             PRIMARY KEY (id),
    CONSTRAINT fk_g_persona             FOREIGN KEY (persona_id)
        REFERENCES cobranza.personas(id),
    CONSTRAINT fk_g_ejecutivo           FOREIGN KEY (ejecutivo_id)
        REFERENCES cobranza.usuarios(id),
    CONSTRAINT fk_g_asignacion_diaria   FOREIGN KEY (asignacion_diaria_id)
        REFERENCES cobranza.asignaciones_diarias(id),

    CONSTRAINT ck_g_origen              CHECK (origen_gestion IN ('ASIGNACION_DIARIA','BUSQUEDA_DIRECTA')),
    CONSTRAINT ck_g_tipo                CHECK (tipo_gestion   IN ('CONTACTO_FAMILIAR','COMPROMISO_PAGO','SIN_CONTACTO')),

    -- Coherencia origen ↔ asignacion_diaria_id
    CONSTRAINT ck_g_origen_diaria       CHECK (
        (origen_gestion = 'ASIGNACION_DIARIA' AND asignacion_diaria_id IS NOT NULL)
        OR
        (origen_gestion = 'BUSQUEDA_DIRECTA'  AND asignacion_diaria_id IS NULL)
    ),

    -- Coherencia tipo ↔ fecha_compromiso
    CONSTRAINT ck_g_compromiso          CHECK (
        (tipo_gestion = 'COMPROMISO_PAGO' AND fecha_compromiso IS NOT NULL)
        OR
        (tipo_gestion <> 'COMPROMISO_PAGO' AND fecha_compromiso IS NULL)
    ),

    -- Rangos geográficos
    CONSTRAINT ck_g_latitud             CHECK (latitud  BETWEEN -90  AND 90),
    CONSTRAINT ck_g_longitud            CHECK (longitud BETWEEN -180 AND 180),
    CONSTRAINT ck_g_precision           CHECK (precision_metros >= 0)
);

CREATE INDEX idx_g_persona    ON cobranza.gestiones (persona_id);
CREATE INDEX idx_g_ejecutivo  ON cobranza.gestiones (ejecutivo_id);
CREATE INDEX idx_g_diaria     ON cobranza.gestiones (asignacion_diaria_id) WHERE asignacion_diaria_id IS NOT NULL;
CREATE INDEX idx_g_fecha      ON cobranza.gestiones (fecha_gestion DESC);
