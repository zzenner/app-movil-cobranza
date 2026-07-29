-- V004: Sesiones de autenticación y refresh tokens
-- Los UUID de PK son generados por Java (no se usa DEFAULT gen_random_uuid()).
-- El vencimiento absoluto de sesión se copia a cada refresh token para calcular
-- min(vencimiento_deslizante, vencimiento_abs_sesion) en el servicio.

-- ============================================================
-- SESIONES DE AUTENTICACIÓN
-- Una sola sesión ACTIVA por par (usuario, dispositivo).
-- ============================================================
CREATE TABLE cobranza.sesiones_autenticacion (
    id                      UUID         NOT NULL,
    usuario_id              UUID         NOT NULL,
    dispositivo_id          UUID         NOT NULL,
    estado                  VARCHAR(20)  NOT NULL DEFAULT 'ACTIVA',
    ip_origen               VARCHAR(45),
    user_agent              VARCHAR(500),
    fecha_creacion          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    fecha_ultimo_acceso     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    fecha_vencimiento_abs   TIMESTAMPTZ  NOT NULL,
    fecha_cierre            TIMESTAMPTZ,
    motivo_cierre           VARCHAR(30),
    version                 BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT pk_sesiones              PRIMARY KEY (id),
    CONSTRAINT fk_sesiones_usuario      FOREIGN KEY (usuario_id)
        REFERENCES cobranza.usuarios(id),
    CONSTRAINT fk_sesiones_dispositivo  FOREIGN KEY (dispositivo_id)
        REFERENCES cobranza.dispositivos(id),
    CONSTRAINT ck_sesiones_estado       CHECK (
        estado IN ('ACTIVA', 'CERRADA', 'COMPROMETIDA')
    ),
    CONSTRAINT ck_sesiones_cierre       CHECK (
        (estado = 'ACTIVA'  AND fecha_cierre IS NULL    AND motivo_cierre IS NULL)
        OR
        (estado != 'ACTIVA' AND fecha_cierre IS NOT NULL AND motivo_cierre IS NOT NULL)
    )
);

-- Un solo estado ACTIVA por par usuario-dispositivo
CREATE UNIQUE INDEX uq_sesiones_activa_usuario_dispositivo
    ON cobranza.sesiones_autenticacion (usuario_id, dispositivo_id)
    WHERE estado = 'ACTIVA';

CREATE INDEX idx_sesiones_usuario     ON cobranza.sesiones_autenticacion (usuario_id);
CREATE INDEX idx_sesiones_dispositivo ON cobranza.sesiones_autenticacion (dispositivo_id);

-- ============================================================
-- REFRESH TOKENS
-- Opacos: solo se almacena el hash SHA-256 (64 hex chars).
-- Un solo token ACTIVO por sesión.
-- Los tokens CONSUMIDOS se conservan para detectar reutilización.
-- ============================================================
CREATE TABLE cobranza.refresh_tokens (
    id                  UUID         NOT NULL,
    sesion_id           UUID         NOT NULL,
    hash_token          VARCHAR(64)  NOT NULL,
    estado              VARCHAR(20)  NOT NULL DEFAULT 'ACTIVO',
    fecha_creacion      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    fecha_vencimiento   TIMESTAMPTZ  NOT NULL,
    fecha_consumo       TIMESTAMPTZ,
    fecha_revocacion    TIMESTAMPTZ,
    CONSTRAINT pk_refresh_tokens             PRIMARY KEY (id),
    CONSTRAINT fk_refresh_tokens_sesion      FOREIGN KEY (sesion_id)
        REFERENCES cobranza.sesiones_autenticacion(id),
    CONSTRAINT uq_refresh_tokens_hash        UNIQUE (hash_token),
    CONSTRAINT ck_refresh_tokens_estado      CHECK (
        estado IN ('ACTIVO', 'CONSUMIDO', 'REVOCADO')
    ),
    CONSTRAINT ck_refresh_tokens_consumo     CHECK (
        estado != 'CONSUMIDO' OR fecha_consumo IS NOT NULL
    ),
    CONSTRAINT ck_refresh_tokens_revocacion  CHECK (
        estado != 'REVOCADO' OR fecha_revocacion IS NOT NULL
    ),
    CONSTRAINT ck_refresh_tokens_exclusividad CHECK (
        fecha_consumo IS NULL OR fecha_revocacion IS NULL
    )
);

-- Un solo refresh token ACTIVO por sesión
CREATE UNIQUE INDEX uq_refresh_tokens_activo_sesion
    ON cobranza.refresh_tokens (sesion_id)
    WHERE estado = 'ACTIVO';

CREATE INDEX idx_refresh_tokens_sesion ON cobranza.refresh_tokens (sesion_id);
