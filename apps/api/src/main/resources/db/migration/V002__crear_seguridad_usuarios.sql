-- V002: Crear tablas de seguridad y usuarios
-- Flyway es el propietario exclusivo de todos los objetos de base de datos.
-- Los esquemas cobranza y auditoria existen gracias a V001.

-- ============================================================
-- ROLES
-- ============================================================
CREATE TABLE cobranza.roles (
    id                  UUID         NOT NULL DEFAULT gen_random_uuid(),
    codigo              VARCHAR(50)  NOT NULL,
    nombre              VARCHAR(100) NOT NULL,
    descripcion         TEXT,
    activo              BOOLEAN      NOT NULL DEFAULT TRUE,
    fecha_creacion      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    fecha_actualizacion TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_roles        PRIMARY KEY (id),
    CONSTRAINT uq_roles_codigo UNIQUE (codigo)
);

-- ============================================================
-- PERMISOS
-- ============================================================
CREATE TABLE cobranza.permisos (
    id          UUID         NOT NULL DEFAULT gen_random_uuid(),
    codigo      VARCHAR(100) NOT NULL,
    nombre      VARCHAR(200) NOT NULL,
    descripcion TEXT,
    activo      BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_permisos        PRIMARY KEY (id),
    CONSTRAINT uq_permisos_codigo UNIQUE (codigo)
);

-- ============================================================
-- USUARIOS
-- nombre_usuario y correo normalizados a minúsculas sin espacios externos.
-- ============================================================
CREATE TABLE cobranza.usuarios (
    id                  UUID         NOT NULL DEFAULT gen_random_uuid(),
    nombre_usuario      VARCHAR(50)  NOT NULL,
    nombres             VARCHAR(100) NOT NULL,
    apellido_paterno    VARCHAR(100) NOT NULL,
    apellido_materno    VARCHAR(100),
    correo              VARCHAR(200),
    contrasena_hash     VARCHAR(255) NOT NULL,
    activo              BOOLEAN      NOT NULL DEFAULT TRUE,
    bloqueado           BOOLEAN      NOT NULL DEFAULT FALSE,
    intentos_fallidos   INTEGER      NOT NULL DEFAULT 0,
    fecha_ultimo_acceso TIMESTAMPTZ,
    fecha_creacion      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    fecha_actualizacion TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version             BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT pk_usuarios                PRIMARY KEY (id),
    CONSTRAINT uq_usuarios_nombre_usuario UNIQUE (nombre_usuario),
    CONSTRAINT uq_usuarios_correo         UNIQUE (correo),
    CONSTRAINT ck_usuarios_intentos       CHECK (intentos_fallidos >= 0),
    CONSTRAINT ck_usuarios_nombre_normalizado
        CHECK (nombre_usuario = lower(btrim(nombre_usuario))),
    CONSTRAINT ck_usuarios_correo_normalizado
        CHECK (correo IS NULL OR correo = lower(btrim(correo)))
);

-- ============================================================
-- USUARIO_ROLES  (historial: se puede reasignar el rol)
-- La unicidad activa se garantiza con índice parcial.
-- ============================================================
CREATE TABLE cobranza.usuario_roles (
    id               UUID        NOT NULL DEFAULT gen_random_uuid(),
    usuario_id       UUID        NOT NULL,
    rol_id           UUID        NOT NULL,
    fecha_asignacion TIMESTAMPTZ NOT NULL DEFAULT now(),
    fecha_termino    TIMESTAMPTZ,
    asignado_por     UUID,
    activo           BOOLEAN     NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_usuario_roles               PRIMARY KEY (id),
    CONSTRAINT fk_usuario_roles_usuario       FOREIGN KEY (usuario_id)
        REFERENCES cobranza.usuarios(id),
    CONSTRAINT fk_usuario_roles_rol           FOREIGN KEY (rol_id)
        REFERENCES cobranza.roles(id),
    CONSTRAINT fk_usuario_roles_asignado_por  FOREIGN KEY (asignado_por)
        REFERENCES cobranza.usuarios(id),
    CONSTRAINT ck_usuario_roles_fechas        CHECK (
        fecha_termino IS NULL OR fecha_termino >= fecha_asignacion
    )
);

-- Solo una asignación activa por (usuario, rol)
CREATE UNIQUE INDEX uq_usuario_roles_activo
    ON cobranza.usuario_roles (usuario_id, rol_id)
    WHERE activo = TRUE;

-- ============================================================
-- ROL_PERMISOS
-- ============================================================
CREATE TABLE cobranza.rol_permisos (
    rol_id     UUID NOT NULL,
    permiso_id UUID NOT NULL,
    CONSTRAINT pk_rol_permisos         PRIMARY KEY (rol_id, permiso_id),
    CONSTRAINT fk_rol_permisos_rol     FOREIGN KEY (rol_id)
        REFERENCES cobranza.roles(id),
    CONSTRAINT fk_rol_permisos_permiso FOREIGN KEY (permiso_id)
        REFERENCES cobranza.permisos(id)
);

-- ============================================================
-- DISPOSITIVOS
-- Coherencia de revocación reforzada con CHECK.
-- ============================================================
CREATE TABLE cobranza.dispositivos (
    id                          UUID         NOT NULL DEFAULT gen_random_uuid(),
    usuario_id                  UUID         NOT NULL,
    identificador_instalacion   VARCHAR(36)  NOT NULL,
    nombre_dispositivo          VARCHAR(200),
    fabricante                  VARCHAR(100),
    modelo                      VARCHAR(100),
    version_android             VARCHAR(20),
    version_aplicacion          VARCHAR(20),
    activo                      BOOLEAN      NOT NULL DEFAULT TRUE,
    revocado                    BOOLEAN      NOT NULL DEFAULT FALSE,
    fecha_registro              TIMESTAMPTZ  NOT NULL DEFAULT now(),
    fecha_ultima_sincronizacion TIMESTAMPTZ,
    fecha_revocacion            TIMESTAMPTZ,
    version                     BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT pk_dispositivos                PRIMARY KEY (id),
    CONSTRAINT fk_dispositivos_usuario        FOREIGN KEY (usuario_id)
        REFERENCES cobranza.usuarios(id),
    CONSTRAINT uq_dispositivos_id_instalacion UNIQUE (identificador_instalacion),
    -- Reglas:
    --   no revocado  → activo puede ser true o false; sin fecha_revocacion
    --   revocado     → inactivo obligatorio; fecha_revocacion obligatoria
    CONSTRAINT ck_dispositivos_coherencia_revocacion CHECK (
        (revocado = FALSE AND fecha_revocacion IS NULL)
        OR
        (revocado = TRUE  AND activo = FALSE AND fecha_revocacion IS NOT NULL)
    )
);

-- ============================================================
-- SUPERVISION_USUARIOS
-- Un único supervisor activo por ejecutivo (índice parcial).
-- ============================================================
CREATE TABLE cobranza.supervision_usuarios (
    id             UUID    NOT NULL DEFAULT gen_random_uuid(),
    supervisor_id  UUID    NOT NULL,
    ejecutivo_id   UUID    NOT NULL,
    fecha_inicio   DATE    NOT NULL,
    fecha_termino  DATE,
    activo         BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_supervision_usuarios         PRIMARY KEY (id),
    CONSTRAINT fk_supervision_supervisor       FOREIGN KEY (supervisor_id)
        REFERENCES cobranza.usuarios(id),
    CONSTRAINT fk_supervision_ejecutivo        FOREIGN KEY (ejecutivo_id)
        REFERENCES cobranza.usuarios(id),
    CONSTRAINT ck_supervision_no_mismo_usuario CHECK (supervisor_id != ejecutivo_id),
    CONSTRAINT ck_supervision_coherencia_activo CHECK (
        (activo = TRUE  AND fecha_termino IS NULL)
        OR
        (activo = FALSE AND fecha_termino IS NOT NULL)
    ),
    CONSTRAINT ck_supervision_fechas CHECK (
        fecha_termino IS NULL OR fecha_termino >= fecha_inicio
    )
);

-- Solo un supervisor activo por ejecutivo
CREATE UNIQUE INDEX uq_supervision_ejecutivo_activo
    ON cobranza.supervision_usuarios (ejecutivo_id)
    WHERE activo = TRUE;

-- ============================================================
-- ÍNDICES (excluye columnas con PK o UNIQUE ya indexadas)
-- ============================================================
CREATE INDEX idx_usuario_roles_usuario  ON cobranza.usuario_roles (usuario_id);
CREATE INDEX idx_usuario_roles_rol      ON cobranza.usuario_roles (rol_id);
CREATE INDEX idx_dispositivos_usuario   ON cobranza.dispositivos (usuario_id);
CREATE INDEX idx_supervision_supervisor ON cobranza.supervision_usuarios (supervisor_id);
CREATE INDEX idx_supervision_ejecutivo  ON cobranza.supervision_usuarios (ejecutivo_id);
